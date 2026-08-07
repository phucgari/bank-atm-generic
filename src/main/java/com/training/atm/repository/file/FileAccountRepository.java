package com.training.atm.repository.file;

import com.training.atm.model.Account;
import com.training.atm.model.CurrentAccount;
import com.training.atm.model.SavingsAccount;
import com.training.atm.model.enums.AccountType;
import com.training.atm.model.strategy.CompoundInterestStrategy;
import com.training.atm.model.strategy.ZeroOnNegativeInterestStrategy;
import com.training.atm.repository.AccountRepository;

import java.io.*;
import java.util.*;

/**
 * Text-file implementation of {@link AccountRepository}.
 * Format: accountNumber|type|balance|lastInterestYearMonth
 *
 * Strategy pattern: accounts are created via the strategy-injecting
 * constructor, making the algorithm explicit at the construction site.
 * Swapping to a different interest formula (e.g. tiered, promotional) only
 * requires passing a different {@link com.training.atm.model.strategy.InterestStrategy}
 * here — no Account subclass changes are needed.
 *
 * CoR-safe OCP: an exhaustive {@code switch} on {@link AccountType} ensures
 * the compiler flags any unhandled account type at build time.
 */
public class FileAccountRepository implements AccountRepository {
    private static final String FILE = DataDirectory.getPath("accounts.txt");
    private final Map<String, Account> accounts = new LinkedHashMap<>();

    public FileAccountRepository() { load(); }

    private void load() {
        File f = new File(FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split("\\|", -1);
                if (p.length < 4) continue;
                Account acc = parseAccount(p);
                if (acc != null) accounts.put(acc.getAccountNumber(), acc);
            }
        } catch (IOException e) {
            System.err.println("Error loading accounts: " + e.getMessage());
        }
    }

    private Account parseAccount(String[] p) {
        try {
            long balance = Long.parseLong(p[2]);
            AccountType type = AccountType.valueOf(p[1]);
            // Strategy pattern: each account type gets its own algorithm injected.
            return switch (type) {
                case SAVINGS -> new SavingsAccount(p[0], balance, p[3],
                        new CompoundInterestStrategy(SavingsAccount.MONTHLY_RATE));
                case CURRENT -> new CurrentAccount(p[0], balance, p[3],
                        new ZeroOnNegativeInterestStrategy(CurrentAccount.MONTHLY_RATE));
            };
        } catch (IllegalArgumentException e) {
            System.err.println("Unknown account type '" + p[1] + "' for account " + p[0] + " — skipped.");
            return null;
        }
    }

    private void saveAll() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE))) {
            for (Account a : accounts.values()) {
                pw.println(a.getAccountNumber() + "|" + a.getAccountType() + "|"
                        + a.getAccountBalance() + "|" + a.getLastInterestYearMonth());
            }
        } catch (IOException e) {
            System.err.println("Error saving accounts: " + e.getMessage());
        }
    }

    @Override public Optional<Account> findByAccountNumber(String accountNumber) {
        return Optional.ofNullable(accounts.get(accountNumber));
    }

    @Override public List<Account> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(accounts.values()));
    }

    @Override public Account update(Account account) {
        if (accounts.containsKey(account.getAccountNumber())) {
            accounts.put(account.getAccountNumber(), account);
            saveAll();
            return account;
        }
        return null;
    }

    @Override public void updateAll(Collection<Account> updated) {
        for (Account a : updated) accounts.put(a.getAccountNumber(), a);
        saveAll();
    }
}
