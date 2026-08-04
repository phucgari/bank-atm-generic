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
import com.training.atm.validation.TransactionValidator;
import com.training.atm.validation.deposit.*;

import java.util.Optional;

/**
 * Implements deposit business rules (FR-03).
 *
 * <h3>Chain of Responsibility</h3>
 * Validation is performed by a composable {@link TransactionValidator} chain
 * assembled once at construction time.  Each validator is responsible for a
 * single rule.
 *
 * <h3>Design notes</h3>
 * SRP: only responsible for the deposit execution flow.<br>
 * DIP: depends on repository interfaces.
 */
public class DepositServiceImpl implements DepositService {
    private final AccountRepository     accountRepo;
    private final TransactionRepository txRepo;
    private final CashDispenser         cashDispenser;

    /** CoR chain assembled once — validators are stateless, so sharing is safe. */
    private final TransactionValidator<DepositContext> validationChain =
            new DenominationDepositValidator()
            .then(new SingleDepositLimitValidator());

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

        Optional<String> error = validationChain.validate(ctx);
        if (error.isPresent()) return DepositResult.failure(error.get());

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
