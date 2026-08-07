package com.training.atm.service.impl;

import com.training.atm.command.TransferCommand;
import com.training.atm.config.db.TransactionManager;
import com.training.atm.dto.TransferResult;
import com.training.atm.model.*;
import com.training.atm.model.enums.TransactionType;
import com.training.atm.model.enums.TransferFrequency;
import com.training.atm.repository.*;
import com.training.atm.service.TransferService;
import com.training.atm.util.DateUtil;
import com.training.atm.validation.EntityValidator;
import com.training.atm.validation.ValidationResult;
import com.training.atm.validation.transfer.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Implements fund-transfer (FR-07) and scheduled-transfer processing (FR-10).
 *
 * <h3>Validation</h3>
 * All pre-transfer guards are delegated to an {@link EntityValidator} built
 * once at construction time. The {@link TransferContext} record carries all
 * pre-fetched data (including the destination account) so validators and
 * post-validation logic share the same object.
 *
 * <h3>Command pattern</h3>
 * {@code processScheduledTransfers()} wraps each execution in a
 * {@link TransferCommand}, decoupling the scheduler loop from the transfer
 * business logic and making the audit trail self-describing.
 *
 * <h3>State pattern</h3>
 * Lifecycle transitions on {@link ScheduledTransfer} are delegated to the
 * embedded {@link com.training.atm.model.state.TransferLifecycleState}.
 * No {@code if (status == X)} chains appear here.
 *
 * <h3>Design notes</h3>
 * SRP: only responsible for transfer logic and scheduled-transfer lifecycle.<br>
 * DIP: all dependencies are repository / service interfaces.<br>
 * OCP: uses {@link Account#verifyWithdrawAmount} and
 *      {@link Account#getInsufficientFundsMessage} polymorphically.
 */
public class TransferServiceImpl implements TransferService {
    private final AccountRepository           accountRepo;
    private final TransactionRepository       txRepo;
    private final ScheduledTransferRepository schedRepo;
    private final CustomerRepository          customerRepo;
    private final TransactionManager          transactionManager;

    private final EntityValidator<TransferContext> validator = new EntityValidator<TransferContext>()
            .addRule(new SameAccountTransferValidator())
            .addRule(new PositiveAmountTransferValidator())
            .addRule(new SingleTransferLimitValidator())
            .addRule(new DestinationExistsValidator())
            .addRule(new DailyTransferLimitValidator())
            .addRule(new BalanceTransferValidator());

    public TransferServiceImpl(AccountRepository accountRepo,
                               TransactionRepository txRepo,
                               ScheduledTransferRepository schedRepo,
                               CustomerRepository customerRepo, TransactionManager transactionManager) {
        this.accountRepo  = accountRepo;
        this.txRepo       = txRepo;
        this.schedRepo    = schedRepo;
        this.customerRepo = customerRepo;
        this.transactionManager = transactionManager;
    }

    @Override
    public TransferResult transfer(Account source, String destAccountNumber, long amount) {
        return executeTransaction(() -> transferInternal(source, destAccountNumber, amount));
    }

    // -----------------------------------------------------------------------
    // FR-07: Fund Transfer
    // -----------------------------------------------------------------------
    private TransferResult transferInternal(Account source, String destAccountNumber, long amount) {
        // Pre-fetch everything once — both validation chain and post-validation logic share it.
        Optional<Account> destOpt = accountRepo.findByAccountNumber(destAccountNumber);
        long todayTotal = txRepo.sumByAccountNumberTypeAndDate(
                source.getAccountNumber(), TransactionType.TRANSFER_OUT, DateUtil.today());

        TransferContext ctx = new TransferContext(
                source, destAccountNumber, destOpt.orElse(null), amount, todayTotal);

        List<ValidationResult> errors = validator.validate(ctx);
        if (!errors.isEmpty()) return TransferResult.failure(errors.getFirst().getErrorCode());

        // destAccount is guaranteed non-null here (DestinationExistsValidator passed).
        Account dest = ctx.destAccount();
        String destName = customerRepo.findByAccountNumber(destAccountNumber)
                .map(BankCustomer::getCustomerName).orElse("Unknown");

        // Simulate atomicity: update both balances before persisting either.
        long srcBalance = source.getAccountBalance() - amount;
        long dstBalance = dest.getAccountBalance()   + amount;
        source.updateBalance(srcBalance);
        dest.updateBalance(dstBalance);
        accountRepo.update(source);
        accountRepo.update(dest);

        String ts   = DateUtil.now();
        String txId = DateUtil.generateTxId();
        Transaction txOut = new Transaction(txId + "_OUT", source.getAccountNumber(), ts,
                TransactionType.TRANSFER_OUT, amount, srcBalance, "To: " + destAccountNumber);
        Transaction txIn  = new Transaction(txId + "_IN",  destAccountNumber, ts,
                TransactionType.TRANSFER_IN,  amount, dstBalance, "From: " + source.getAccountNumber());
        txRepo.save(txOut);
        txRepo.save(txIn);

        return TransferResult.success(txOut, destName);
    }

    private <T> T executeTransaction(Callable<T> action) {
        if (transactionManager == null) {
            try {
                return action.call();
            } catch (Exception e) {
                throw new IllegalStateException("Transfer transaction failed", e);
            }
        }
        return transactionManager.executeInTransaction(action);
    }

    // -----------------------------------------------------------------------
    // FR-10: Scheduled Transfer Processing
    // -----------------------------------------------------------------------
    @Override
    public void processScheduledTransfers() {
        String today = DateUtil.today();
        List<ScheduledTransfer> active = schedRepo.findAllActive(); // uses canExecute() via State
        for (ScheduledTransfer st : active) {
            if (st.getNextExecutionDate().compareTo(today) > 0) continue;

            // Terminal conditions — delegate state transition to the model.
            if (!st.getEndDate().isEmpty() && today.compareTo(st.getEndDate()) > 0) {
                st.complete();
                schedRepo.update(st);
                continue;
            }
            if (st.getMaxRepeat() >= 0 && st.getRepeatCount() >= st.getMaxRepeat()) {
                st.complete();
                schedRepo.update(st);
                continue;
            }

            Optional<Account> srcOpt = accountRepo.findByAccountNumber(st.getSourceAccount());
            if (srcOpt.isEmpty()) {
                st.executeFail();   // State pattern: let the state decide the new status.
                schedRepo.update(st);
                System.err.println("[SCHEDULER] Transfer " + st.getId()
                        + " FAILED: source account not found.");
                continue;
            }

            // Command pattern: wrap execution in a self-describing command.
            TransferCommand cmd = new TransferCommand(
                    srcOpt.get(), st.getDestAccount(), st.getAmount(), this);
            TransferResult result = transfer(srcOpt.get(), st.getDestAccount(), st.getAmount());

            if (!result.isSuccess()) {
                st.executeFail();   // State pattern
                schedRepo.update(st);
                System.err.println("[SCHEDULER] " + cmd.describe() + " FAILED: " + result.getMessage());
                continue;
            }

            st.setRepeatCount(st.getRepeatCount() + 1);
            boolean isLastExecution =
                    st.getFrequency() == TransferFrequency.ONE_TIME
                    || (st.getMaxRepeat() >= 0 && st.getRepeatCount() >= st.getMaxRepeat());

            if (!isLastExecution) {
                st.setNextExecutionDate(DateUtil.advanceDate(st.getNextExecutionDate(), st.getFrequency()));
            }
            st.executeSuccess(isLastExecution);     // State pattern
            schedRepo.update(st);
            System.out.println("[SCHEDULER] " + cmd.describe() + " → OK");
        }
    }
}
