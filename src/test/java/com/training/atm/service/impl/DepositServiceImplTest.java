package com.training.atm.service.impl;

import com.training.atm.dto.DepositResult;
import com.training.atm.dto.ErrorCode;
import com.training.atm.model.Account;
import com.training.atm.model.CurrentAccount;
import com.training.atm.model.SavingsAccount;
import com.training.atm.service.CashDispenser;
import com.training.atm.testutil.TestRepositories.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class DepositServiceImplTest {

    private InMemoryAccountRepository accountRepo;
    private InMemoryTransactionRepository txRepo;
    private InMemoryDenominationRepository denomRepo;
    private DepositServiceImpl service;

    @Before
    public void setUp() {
        txRepo = new InMemoryTransactionRepository();
        denomRepo = new InMemoryDenominationRepository();
        denomRepo.denominations.put(500_000L, 10);
        denomRepo.denominations.put(100_000L, 20);
        denomRepo.denominations.put(50_000L, 30);
    }

    @Test
    public void depositSucceedsWithValidAmount() {
        Account account = new SavingsAccount("ACC001", 100_000, "");
        accountRepo = new InMemoryAccountRepository(account);
        service = new DepositServiceImpl(accountRepo, txRepo, new CashDispenser(denomRepo));

        DepositResult result = service.deposit(account, 100_000);

        assertTrue(result.isSuccess());
        assertNull(result.getErrorCode());
        assertEquals(200_000, account.getAccountBalance());
        assertEquals(1, txRepo.saved.size());
        assertEquals(100_000, txRepo.saved.get(0).getAmount());
    }

    @Test
    public void depositUpdatesAtmCash() {
        Account account = new SavingsAccount("ACC001", 500_000, "");
        accountRepo = new InMemoryAccountRepository(account);
        service = new DepositServiceImpl(accountRepo, txRepo, new CashDispenser(denomRepo));

        service.deposit(account, 200_000);

        assertEquals(200_000, denomRepo.depositedAmount);
    }

    @Test
    public void depositRejectsInvalidDenomination() {
        Account account = new SavingsAccount("ACC001", 100_000, "");
        accountRepo = new InMemoryAccountRepository(account);
        service = new DepositServiceImpl(accountRepo, txRepo, new CashDispenser(denomRepo));

        DepositResult result = service.deposit(account, 15_000);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.INVALID_AMOUNT, result.getErrorCode());
        assertEquals(100_000, account.getAccountBalance());
        assertTrue(txRepo.saved.isEmpty());
        assertEquals(0, denomRepo.depositedAmount);
    }

    @Test
    public void depositRejectsAmountExceedingSingleLimit() {
        Account account = new SavingsAccount("ACC001", 100_000, "");
        accountRepo = new InMemoryAccountRepository(account);
        service = new DepositServiceImpl(accountRepo, txRepo, new CashDispenser(denomRepo));

        DepositResult result = service.deposit(account, 51_000_000);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.LIMIT_EXCEEDED, result.getErrorCode());
        assertEquals(100_000, account.getAccountBalance());
        assertTrue(txRepo.saved.isEmpty());
    }

    @Test
    public void depositAcceptsMaximumAllowedAmount() {
        Account account = new SavingsAccount("ACC001", 1_000_000, "");
        accountRepo = new InMemoryAccountRepository(account);
        service = new DepositServiceImpl(accountRepo, txRepo, new CashDispenser(denomRepo));

        DepositResult result = service.deposit(account, 50_000_000);

        assertTrue(result.isSuccess());
        assertEquals(51_000_000, account.getAccountBalance());
    }

    @Test
    public void depositWorksForCurrentAccount() {
        Account account = new CurrentAccount("ACC002", -500_000, "");
        accountRepo = new InMemoryAccountRepository(account);
        service = new DepositServiceImpl(accountRepo, txRepo, new CashDispenser(denomRepo));

        DepositResult result = service.deposit(account, 1_000_000);

        assertTrue(result.isSuccess());
        assertEquals(500_000, account.getAccountBalance());
    }

    @Test
    public void depositRejectsZeroAmount() {
        Account account = new SavingsAccount("ACC001", 100_000, "");
        accountRepo = new InMemoryAccountRepository(account);
        service = new DepositServiceImpl(accountRepo, txRepo, new CashDispenser(denomRepo));

        DepositResult result = service.deposit(account, 0);

        assertFalse(result.isSuccess());
        assertEquals(100_000, account.getAccountBalance());
    }

    @Test
    public void depositRejectsNegativeAmount() {
        Account account = new SavingsAccount("ACC001", 100_000, "");
        accountRepo = new InMemoryAccountRepository(account);
        service = new DepositServiceImpl(accountRepo, txRepo, new CashDispenser(denomRepo));

        DepositResult result = service.deposit(account, -50_000);

        assertFalse(result.isSuccess());
        assertEquals(100_000, account.getAccountBalance());
    }
}
