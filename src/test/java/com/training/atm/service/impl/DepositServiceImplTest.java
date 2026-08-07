package com.training.atm.service.impl;

import com.training.atm.dto.DepositResult;
import com.training.atm.dto.ErrorCode;
import com.training.atm.model.Account;
import com.training.atm.repository.*;
import com.training.atm.service.CashDispenser;
import com.training.atm.testutil.TestDataManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class DepositServiceImplTest {

    private AccountRepository accountRepo;
    private TransactionRepository txRepo;
    private DenominationRepository denomRepo;
    private DepositServiceImpl service;

    @BeforeClass
    public static void initTestData() {
        TestDataManager.resetTestData();
    }

    @Before
    public void setUp() {
        TestDataManager.resetTestData();
        RepositoryContext context = TestDataManager.createRepositories();
        accountRepo = context.accounts();
        txRepo = context.transactions();
        denomRepo = context.denominations();
        service = new DepositServiceImpl(accountRepo, txRepo, new CashDispenser(denomRepo));
    }

    @Test
    public void depositSucceedsWithValidAmount() {
        Account account = accountRepo.findByAccountNumber("ACC001").orElseThrow();
        long initialBalance = account.getAccountBalance();

        DepositResult result = service.deposit(account, 100_000);

        assertTrue(result.isSuccess());
        assertNull(result.getErrorCode());
        assertEquals(initialBalance + 100_000, account.getAccountBalance());

        // Verify transaction was saved
        var transactions = txRepo.findByAccountNumber("ACC001");
        assertTrue(transactions.stream().anyMatch(tx -> tx.getAmount() == 100_000));
    }

    @Test
    public void depositUpdatesAtmCash() {
        Account account = accountRepo.findByAccountNumber("ACC001").orElseThrow();
        long initialCash = denomRepo.getTotalCash();

        service.deposit(account, 200_000);

        // Note: deposit adds to ATM cash
        assertEquals(initialCash + 200_000, denomRepo.getTotalCash());
    }

    @Test
    public void depositRejectsInvalidDenomination() {
        Account account = accountRepo.findByAccountNumber("ACC001").orElseThrow();
        long initialBalance = account.getAccountBalance();

        DepositResult result = service.deposit(account, 15_000);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.INVALID_DEPOSIT_AMOUNT, result.getErrorCode());
        assertEquals(initialBalance, account.getAccountBalance());
    }

    @Test
    public void depositRejectsAmountExceedingSingleLimit() {
        Account account = accountRepo.findByAccountNumber("ACC001").orElseThrow();
        long initialBalance = account.getAccountBalance();

        DepositResult result = service.deposit(account, 51_000_000);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.SINGLE_DEPOSIT_LIMIT_EXCEEDED, result.getErrorCode());
        assertEquals(initialBalance, account.getAccountBalance());
    }

    @Test
    public void depositAcceptsMaximumAllowedAmount() {
        Account account = accountRepo.findByAccountNumber("ACC001").orElseThrow();
        long initialBalance = account.getAccountBalance();

        DepositResult result = service.deposit(account, 50_000_000);

        assertTrue(result.isSuccess());
        assertEquals(initialBalance + 50_000_000, account.getAccountBalance());
    }

    @Test
    public void depositWorksForCurrentAccount() {
        Account account = accountRepo.findByAccountNumber("ACC003").orElseThrow(); // CURRENT account
        long initialBalance = account.getAccountBalance();

        DepositResult result = service.deposit(account, 1_000_000);

        assertTrue(result.isSuccess());
        assertEquals(initialBalance + 1_000_000, account.getAccountBalance());
    }

    @Test
    public void depositRejectsZeroAmount() {
        Account account = accountRepo.findByAccountNumber("ACC001").orElseThrow();
        long initialBalance = account.getAccountBalance();

        DepositResult result = service.deposit(account, 0);

        assertFalse(result.isSuccess());
        assertEquals(initialBalance, account.getAccountBalance());
    }

    @Test
    public void depositRejectsNegativeAmount() {
        Account account = accountRepo.findByAccountNumber("ACC001").orElseThrow();
        long initialBalance = account.getAccountBalance();

        DepositResult result = service.deposit(account, -50_000);

        assertFalse(result.isSuccess());
        assertEquals(initialBalance, account.getAccountBalance());
    }
}
