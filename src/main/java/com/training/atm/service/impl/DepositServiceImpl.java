package com.training.atm.service.impl;

import com.training.atm.dto.DepositResult;
import com.training.atm.model.Account;
import com.training.atm.model.Transaction;
import com.training.atm.model.enums.TransactionType;
import com.training.atm.repository.AccountRepository;
import com.training.atm.repository.TransactionRepository;
import com.training.atm.service.CashDispenser;
import com.training.atm.service.DepositService;
import com.training.atm.util.DateUtil;
import com.training.atm.validation.EntityValidator;
import com.training.atm.validation.ValidationResult;
import com.training.atm.validation.deposit.*;

import java.util.List;

/**
 * Implements deposit business rules (FR-03).
 *
 * <h3>Validation</h3>
 * Validation is performed by an {@link EntityValidator} assembled once at
 * construction time. Each rule is responsible for a single constraint.
 *
 * <h3>Design notes</h3>
 * SRP: only responsible for the deposit execution flow.<br>
 * DIP: depends on repository interfaces.
 */
public class DepositServiceImpl implements DepositService {
    private final AccountRepository     accountRepo;
    private final TransactionRepository txRepo;
    private final CashDispenser         cashDispenser;

    private final EntityValidator<DepositContext> validator = new EntityValidator<DepositContext>()
            .addRule(new DenominationDepositValidator())
            .addRule(new SingleDepositLimitValidator());

    public DepositServiceImpl(AccountRepository accountRepo,
                               TransactionRepository txRepo,
                               CashDispenser cashDispenser) {
        this.accountRepo   = accountRepo;
        this.txRepo        = txRepo;
        this.cashDispenser = cashDispenser;
    }

    @Override
    public DepositResult deposit(Account account, long amount) {
        DepositContext ctx = new DepositContext(account, amount);

        List<ValidationResult> errors = validator.validate(ctx);
        if (!errors.isEmpty()) return DepositResult.failure(
                errors.getFirst().getErrorMessage(), errors.getFirst().getErrorCode());

        long newBalance = account.getAccountBalance() + amount;
        account.updateBalance(newBalance);
        accountRepo.update(account);
        cashDispenser.acceptAmount(amount);

        Transaction tx = new Transaction(DateUtil.generateTxId(), account.getAccountNumber(),
                DateUtil.now(), TransactionType.DEPOSIT, amount, newBalance, "");
        txRepo.save(tx);

        return DepositResult.success(tx);
    }
}
