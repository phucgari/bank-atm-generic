package com.training.atm.service.impl;

import com.training.atm.dto.WithdrawalResult;
import com.training.atm.model.Account;
import com.training.atm.model.Transaction;
import com.training.atm.model.enums.TransactionType;
import com.training.atm.repository.AccountRepository;
import com.training.atm.repository.TransactionRepository;
import com.training.atm.service.CashDispenser;
import com.training.atm.service.WithdrawalService;
import com.training.atm.util.DateUtil;
import com.training.atm.validation.EntityValidator;
import com.training.atm.validation.ValidationResult;
import com.training.atm.validation.withdrawal.*;

import java.util.List;
import java.util.Map;

/**
 * Implements withdrawal business rules (FR-02).
 *
 * <h3>Validation</h3>
 * Validation is performed by an {@link EntityValidator} assembled once at
 * construction time. Each rule is responsible for a single constraint.
 *
 * <h3>Design notes</h3>
 * SRP: only responsible for the withdrawal execution flow.<br>
 * DIP: depends on repository interfaces, not file implementations.<br>
 * OCP: polymorphic calls to {@link Account#verifyWithdrawAmount} and
 *      {@link Account#getInsufficientFundsMessage} — no type-switches required.
 */
public class WithdrawalServiceImpl implements WithdrawalService {
    private final AccountRepository              accountRepo;
    private final TransactionRepository          txRepo;
    private final CashDispenser                  cashDispenser;

    private final EntityValidator<WithdrawalContext> validator = new EntityValidator<WithdrawalContext>()
            .addRule(new DenominationWithdrawalValidator())
            .addRule(new SingleWithdrawalLimitValidator())
            .addRule(new DailyWithdrawalLimitValidator())
            .addRule(new AccountBalanceValidator())
            .addRule(new AtmCashValidator());

    public WithdrawalServiceImpl(AccountRepository accountRepo,
                                  TransactionRepository txRepo,
                                  CashDispenser cashDispenser) {
        this.accountRepo   = accountRepo;
        this.txRepo        = txRepo;
        this.cashDispenser = cashDispenser;
    }

    @Override
    public WithdrawalResult withdraw(Account account, long amount) {
        long dailyTotal = txRepo.sumByAccountNumberTypeAndDate(
                account.getAccountNumber(), TransactionType.WITHDRAWAL, DateUtil.today());
        WithdrawalContext ctx = new WithdrawalContext(
                account, amount, dailyTotal, cashDispenser.getAvailableCash());

        List<ValidationResult> errors = validator.validate(ctx);
        if (!errors.isEmpty()) return WithdrawalResult.failure(errors.getFirst().getErrorMessage());

        Map<Long, Integer> dispensed = cashDispenser.dispenseCash(amount);
        if (dispensed == null)
            return WithdrawalResult.failure(
                    "ATM cannot dispense the exact amount with available bills. Try a different amount.");

        long newBalance = account.getAccountBalance() - amount;
        account.updateBalance(newBalance);
        accountRepo.update(account);

        Transaction tx = new Transaction(DateUtil.generateTxId(), account.getAccountNumber(),
                DateUtil.now(), TransactionType.WITHDRAWAL, amount, newBalance, "");
        txRepo.save(tx);

        return WithdrawalResult.success(tx, dispensed, cashDispenser.getAvailableCash());
    }
}
