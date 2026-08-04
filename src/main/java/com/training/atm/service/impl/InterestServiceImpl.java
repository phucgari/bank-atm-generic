package com.training.atm.service.impl;

import com.training.atm.model.Account;
import com.training.atm.model.Transaction;
import com.training.atm.model.enums.TransactionType;
import com.training.atm.repository.AccountRepository;
import com.training.atm.repository.TransactionRepository;
import com.training.atm.service.InterestService;
import com.training.atm.util.DateUtil;
import com.training.atm.util.FormatUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements monthly interest calculation (FR-06).
 *
 * SRP: only responsible for interest logic.
 * OCP: calls account.calculateInterest() polymorphically — different formulas
 *      (compound for Savings, simple for Current) are resolved at runtime
 *      without any type-switch here.
 *
 * Fix #4: renamed account.updateAccount() → account.updateBalance().
 */
public class InterestServiceImpl implements InterestService {
    private final AccountRepository     accountRepo;
    private final TransactionRepository txRepo;

    public InterestServiceImpl(AccountRepository accountRepo, TransactionRepository txRepo) {
        this.accountRepo = accountRepo;
        this.txRepo      = txRepo;
    }

    @Override
    public List<String> calculateInterestForAll() {
        String ym = DateUtil.currentYearMonth();
        List<String> results = new ArrayList<>();

        for (Account acc : accountRepo.findAll()) {
            if (ym.equals(acc.getLastInterestYearMonth())) {
                results.add(acc.getAccountNumber() + ": already calculated for " + ym);
                continue;
            }
            long interest = acc.calculateInterest(); // polymorphic dispatch
            if (interest <= 0) {
                results.add(acc.getAccountNumber() + ": no interest (balance too low or zero)");
                continue;
            }
            long newBalance = acc.getAccountBalance() + interest;
            acc.updateBalance(newBalance);          // Fix #4: renamed from updateAccount
            acc.setLastInterestYearMonth(ym);
            accountRepo.update(acc);

            Transaction tx = new Transaction(DateUtil.generateTxId(), acc.getAccountNumber(),
                    DateUtil.now(), TransactionType.INTEREST, interest, newBalance,
                    "Monthly interest: " + ym);
            txRepo.save(tx);

            results.add(acc.getAccountNumber() + ": +"
                    + FormatUtil.formatVND(interest)
                    + " (new balance: " + FormatUtil.formatVND(newBalance) + ")");
        }
        return results;
    }
}
