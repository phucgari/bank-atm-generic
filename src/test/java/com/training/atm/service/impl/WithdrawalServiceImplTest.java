package com.training.atm.service.impl;

import com.training.atm.dto.ErrorCode;
import com.training.atm.dto.WithdrawalResult;
import com.training.atm.model.Account;
import com.training.atm.model.CurrentAccount;
import com.training.atm.model.SavingsAccount;
import com.training.atm.model.Transaction;
import com.training.atm.model.enums.TransactionType;
import com.training.atm.service.CashDispenser;
import com.training.atm.testutil.TestRepositories.*;
import com.training.atm.util.DateUtil;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class WithdrawalServiceImplTest {

    private InMemoryAccountRepository accountRepo;
    private InMemoryTransactionRepository txRepo;
    private InMemoryDenominationRepository denomRepo;
    private WithdrawalServiceImpl service;

    @Before
    public void setUp() {
        txRepo = new InMemoryTransactionRepository();
        denomRepo = new InMemoryDenominationRepository();
        denomRepo.denominations.put(500_000L, 10);
        denomRepo.denominations.put(100_000L, 20);
        denomRepo.denominations.put(50_000L, 30);
    }

    @Test
    public void withdrawalSucceedsWithValidAmount() {
        Account account = new SavingsAccount("ACC001", 5_000_000, "");
        accountRepo = new InMemoryAccountRepository(account);
        service = new WithdrawalServiceImpl(accountRepo, txRepo, new CashDispenser(denomRepo));

        WithdrawalResult result = service.withdraw(account, 500_000);

        assertTrue(result.isSuccess());
        assertNull(result.getErrorCode());
        assertEquals(4_500_000, account.getAccountBalance());
        assertEquals(1, txRepo.saved.size());
        assertEquals(500_000, txRepo.saved.get(0).getAmount());
        assertNotNull(result.getDispensed());
    }

    @Test
    public void withdrawalReducesAtmCash() {
        Account account = new SavingsAccount("ACC001", 5_000_000, "");
        accountRepo = new InMemoryAccountRepository(account);
        service = new WithdrawalServiceImpl(accountRepo, txRepo, new CashDispenser(denomRepo));
        long initialCash = denomRepo.getTotalCash();

        service.withdraw(account, 500_000);

        assertEquals(initialCash - 500_000, denomRepo.getTotalCash());
    }

    @Test
    public void withdrawalRejectsInvalidDenomination() {
        Account account = new SavingsAccount("ACC001", 5_000_000, "");
        accountRepo = new InMemoryAccountRepository(account);
        service = new WithdrawalServiceImpl(accountRepo, txRepo, new CashDispenser(denomRepo));

        WithdrawalResult result = service.withdraw(account, 75_000);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.INVALID_AMOUNT, result.getErrorCode());
        assertEquals(5_000_000, account.getAccountBalance());
        assertTrue(txRepo.saved.isEmpty());
    }

    @Test
    public void withdrawalRejectsAmountExceedingSingleLimit() {
        Account account = new SavingsAccount("ACC001", 25_000_000, "");
        accountRepo = new InMemoryAccountRepository(account);
        service = new WithdrawalServiceImpl(accountRepo, txRepo, new CashDispenser(denomRepo));

        WithdrawalResult result = service.withdraw(account, 21_000_000);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.LIMIT_EXCEEDED, result.getErrorCode());
        assertEquals(25_000_000, account.getAccountBalance());
    }

    @Test
    public void withdrawalRejectsAmountExceedingDailyLimit() {
        Account account = new SavingsAccount("ACC001", 50_000_000, "");
        accountRepo = new InMemoryAccountRepository(account);
        service = new WithdrawalServiceImpl(accountRepo, txRepo, new CashDispenser(denomRepo));

        Transaction priorTx = new Transaction("TX001", "ACC001", DateUtil.now(),
                TransactionType.WITHDRAWAL, 16_000_000, 34_000_000, "");
        txRepo.saved.add(priorTx);

        WithdrawalResult result = service.withdraw(account, 5_000_000);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.LIMIT_EXCEEDED, result.getErrorCode());
    }

    @Test
    public void withdrawalRespectsAccountMinimumBalance() {
        Account account = new SavingsAccount("ACC001", 100_000, "");
        accountRepo = new InMemoryAccountRepository(account);
        service = new WithdrawalServiceImpl(accountRepo, txRepo, new CashDispenser(denomRepo));

        WithdrawalResult result = service.withdraw(account, 100_000);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.INSUFFICIENT_FUNDS, result.getErrorCode());
        assertEquals(100_000, account.getAccountBalance());
    }

    @Test
    public void withdrawalAllowsCurrentAccountOverdraft() {
        Account account = new CurrentAccount("ACC002", 500_000, "");
        accountRepo = new InMemoryAccountRepository(account);
        service = new WithdrawalServiceImpl(accountRepo, txRepo, new CashDispenser(denomRepo));

        WithdrawalResult result = service.withdraw(account, 1_000_000);

        assertTrue(result.isSuccess());
        assertEquals(-500_000, account.getAccountBalance());
    }

    @Test
    public void withdrawalRejectsWhenExceedsOverdraftLimit() {
        Account account = new CurrentAccount("ACC002", 100_000, "");
        accountRepo = new InMemoryAccountRepository(account);
        service = new WithdrawalServiceImpl(accountRepo, txRepo, new CashDispenser(denomRepo));

        WithdrawalResult result = service.withdraw(account, 1_200_000);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.INSUFFICIENT_FUNDS, result.getErrorCode());
    }

    @Test
    public void withdrawalRejectsWhenAtmCashInsufficient() {
        denomRepo.denominations.clear();
        denomRepo.denominations.put(100_000L, 1);
        Account account = new SavingsAccount("ACC001", 5_000_000, "");
        accountRepo = new InMemoryAccountRepository(account);
        service = new WithdrawalServiceImpl(accountRepo, txRepo, new CashDispenser(denomRepo));

        WithdrawalResult result = service.withdraw(account, 500_000);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.ATM_CASH_UNAVAILABLE, result.getErrorCode());
        assertEquals(5_000_000, account.getAccountBalance());
    }

    @Test
    public void withdrawalRejectsWhenCannotDispenseExactAmount() {
        denomRepo.denominations.clear();
        denomRepo.denominations.put(100_000L, 10);
        Account account = new SavingsAccount("ACC001", 5_000_000, "");
        accountRepo = new InMemoryAccountRepository(account);
        service = new WithdrawalServiceImpl(accountRepo, txRepo, new CashDispenser(denomRepo));

        WithdrawalResult result = service.withdraw(account, 150_000);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.ATM_CASH_UNAVAILABLE, result.getErrorCode());
    }

    @Test
    public void withdrawalAcceptsMaximumSingleAmount() {
        denomRepo.denominations.put(500_000L, 100);
        Account account = new SavingsAccount("ACC001", 6_000_000, "");
        accountRepo = new InMemoryAccountRepository(account);
        service = new WithdrawalServiceImpl(accountRepo, txRepo, new CashDispenser(denomRepo));

        WithdrawalResult result = service.withdraw(account, 5_000_000);

        assertTrue(result.isSuccess());
        assertEquals(1_000_000, account.getAccountBalance());
    }
}
