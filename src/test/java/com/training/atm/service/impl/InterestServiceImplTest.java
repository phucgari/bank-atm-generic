package com.training.atm.service.impl;

import com.training.atm.model.Account;
import com.training.atm.model.CurrentAccount;
import com.training.atm.model.SavingsAccount;
import com.training.atm.model.enums.TransactionType;
import com.training.atm.testutil.TestRepositories.*;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class InterestServiceImplTest {

    private InMemoryAccountRepository accountRepo;
    private InMemoryTransactionRepository txRepo;
    private InterestServiceImpl service;

    @Before
    public void setUp() {
        accountRepo = new InMemoryAccountRepository();
        txRepo = new InMemoryTransactionRepository();
        service = new InterestServiceImpl(accountRepo, txRepo);
    }

    @Test
    public void calculateInterestForAllProcessesSavingsAccount() {
        Account account = new SavingsAccount("ACC001", 1_000_000, "");
        accountRepo = new InMemoryAccountRepository(account);
        service = new InterestServiceImpl(accountRepo, txRepo);

        List<String> results = service.calculateInterestForAll();

        assertEquals(1, txRepo.saved.size());
        assertTrue(results.get(0).contains("ACC001"));
        assertTrue(account.getAccountBalance() > 1_000_000);
        assertEquals(1, txRepo.saved.size());
        assertEquals(TransactionType.INTEREST, txRepo.saved.get(0).getType());
    }

    @Test
    public void calculateInterestForAllProcessesCurrentAccount() {
        Account account = new CurrentAccount("ACC002", 10_000_000, "");
        accountRepo = new InMemoryAccountRepository(account);
        service = new InterestServiceImpl(accountRepo, txRepo);

        List<String> results = service.calculateInterestForAll();

        assertEquals(1, results.size());
        assertTrue(results.get(0).contains("ACC002"));
        assertTrue(account.getAccountBalance() > 10_000_000);
    }

    @Test
    public void calculateInterestForAllSkipsAccountsWithZeroInterest() {
        Account account = new SavingsAccount("ACC001", 0, "");
        accountRepo = new InMemoryAccountRepository(account);
        service = new InterestServiceImpl(accountRepo, txRepo);

        List<String> results = service.calculateInterestForAll();

        assertEquals(1, results.size());
        assertTrue(results.get(0).contains("no interest"));
        assertEquals(0, account.getAccountBalance());
        assertTrue(txRepo.saved.isEmpty());
    }

    @Test
    public void calculateInterestForAllSkipsNegativeBalance() {
        Account account = new CurrentAccount("ACC002", -100_000, "");
        accountRepo = new InMemoryAccountRepository(account);
        service = new InterestServiceImpl(accountRepo, txRepo);

        List<String> results = service.calculateInterestForAll();

        assertEquals(1, results.size());
        assertTrue(results.get(0).contains("no interest"));
        assertEquals(-100_000, account.getAccountBalance());
    }

    @Test
    public void calculateInterestForAllSkipsAlreadyCalculated() {
        Account account = new SavingsAccount("ACC001", 1_000_000, "");
        accountRepo = new InMemoryAccountRepository(account);
        service = new InterestServiceImpl(accountRepo, txRepo);

        service.calculateInterestForAll();
        txRepo.saved.clear();
        List<String> results = service.calculateInterestForAll();

        assertEquals(1, results.size());
        assertTrue(results.get(0).contains("already calculated"));
        assertTrue(txRepo.saved.isEmpty());
    }

    @Test
    public void calculateInterestForAllProcessesMultipleAccounts() {
        Account acc1 = new SavingsAccount("ACC001", 1_000_000, "");
        Account acc2 = new CurrentAccount("ACC002", 5_000_000, "");
        Account acc3 = new SavingsAccount("ACC003", 10_000_000, "");
        accountRepo = new InMemoryAccountRepository(acc1, acc2, acc3);
        service = new InterestServiceImpl(accountRepo, txRepo);

        List<String> results = service.calculateInterestForAll();

        assertEquals(3, results.size());
        assertEquals(3, txRepo.saved.size());
        assertTrue(acc1.getAccountBalance() > 1_000_000);
        assertTrue(acc2.getAccountBalance() > 5_000_000);
        assertTrue(acc3.getAccountBalance() > 10_000_000);
    }

    @Test
    public void calculateInterestForAllUpdatesLastInterestYearMonth() {
        Account account = new SavingsAccount("ACC001", 1_000_000, "");
        accountRepo = new InMemoryAccountRepository(account);
        service = new InterestServiceImpl(accountRepo, txRepo);

        assertTrue(account.getLastInterestYearMonth().isEmpty());

        service.calculateInterestForAll();

        assertFalse(account.getLastInterestYearMonth().isEmpty());
        assertTrue(account.getLastInterestYearMonth().matches("\\d{4}-\\d{2}"));
    }

    @Test
    public void calculateInterestForAllCreatesCorrectTransaction() {
        Account account = new SavingsAccount("ACC001", 10_000_000, "");
        accountRepo = new InMemoryAccountRepository(account);
        service = new InterestServiceImpl(accountRepo, txRepo);

        service.calculateInterestForAll();

        assertEquals(1, txRepo.saved.size());
        assertEquals("ACC001", txRepo.saved.get(0).getAccountNumber());
        assertEquals(TransactionType.INTEREST, txRepo.saved.get(0).getType());
        assertTrue(txRepo.saved.get(0).getAmount() > 0);
        assertTrue(txRepo.saved.get(0).getDescription().contains("Monthly interest"));
    }

    @Test
    public void calculateInterestForAllReturnsEmptyListWhenNoAccounts() {
        List<String> results = service.calculateInterestForAll();

        assertTrue(results.isEmpty());
    }

    @Test
    public void calculateInterestForAllCalculatesCorrectAmountForSavings() {
        Account account = new SavingsAccount("ACC001", 10_000_000, "");
        accountRepo = new InMemoryAccountRepository(account);
        service = new InterestServiceImpl(accountRepo, txRepo);

        service.calculateInterestForAll();

        long expectedInterest = (long) (10_000_000 * 0.005);
        assertEquals(10_000_000 + expectedInterest, account.getAccountBalance());
    }
}
