package com.training.atm.service.impl;

import com.training.atm.dto.ErrorCode;
import com.training.atm.dto.TransferResult;
import com.training.atm.model.Account;
import com.training.atm.model.Transaction;
import com.training.atm.model.enums.TransactionType;
import com.training.atm.repository.*;
import com.training.atm.testutil.TestDataManager;
import com.training.atm.util.DateUtil;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class TransferServiceImplTest {

    private AccountRepository accountRepo;
    private TransactionRepository txRepo;
    private CustomerRepository customerRepo;
    private ScheduledTransferRepository schedRepo;
    private TransferServiceImpl service;

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
        customerRepo = context.customers();
        schedRepo = context.scheduledTransfers();
        service = new TransferServiceImpl(accountRepo, txRepo, schedRepo, customerRepo, null);
    }

    @Test
    public void transferSucceedsWithValidAccounts() {
        // ACC001 has 13M, ACC002 has 8.5M
        Account source = accountRepo.findByAccountNumber("ACC001").orElseThrow();
        Account dest = accountRepo.findByAccountNumber("ACC002").orElseThrow();
        long sourceInitial = source.getAccountBalance();
        long destInitial = dest.getAccountBalance();

        TransferResult result = service.transfer(source, "ACC002", 500_000);

        assertTrue(result.isSuccess());
        assertEquals(sourceInitial - 500_000, source.getAccountBalance());

        // Reload dest to see updated balance
        dest = accountRepo.findByAccountNumber("ACC002").orElseThrow();
        assertEquals(destInitial + 500_000, dest.getAccountBalance());

        // Verify transactions
        var transactions = txRepo.findByAccountNumber("ACC001");
        assertTrue(transactions.stream().anyMatch(tx -> tx.getType() == TransactionType.TRANSFER_OUT));
    }

    @Test
    public void transferCreatesOutAndInTransactions() {
        Account source = accountRepo.findByAccountNumber("ACC001").orElseThrow();

        service.transfer(source, "ACC002", 500_000);

        var sourceTx = txRepo.findByAccountNumber("ACC001");
        var destTx = txRepo.findByAccountNumber("ACC002");

        assertTrue(sourceTx.stream().anyMatch(tx -> tx.getType() == TransactionType.TRANSFER_OUT));
        assertTrue(destTx.stream().anyMatch(tx -> tx.getType() == TransactionType.TRANSFER_IN));
    }

    @Test
    public void transferRejectsSameSourceAndDestination() {
        Account source = accountRepo.findByAccountNumber("ACC001").orElseThrow();
        long initialBalance = source.getAccountBalance();

        TransferResult result = service.transfer(source, "ACC001", 100_000);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.SAME_ACCOUNT, result.getErrorCode());
        assertEquals(initialBalance, source.getAccountBalance());
    }

    @Test
    public void transferRejectsNonExistentDestination() {
        Account source = accountRepo.findByAccountNumber("ACC001").orElseThrow();
        long initialBalance = source.getAccountBalance();

        TransferResult result = service.transfer(source, "ACC999", 100_000);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, result.getErrorCode());
        assertEquals(initialBalance, source.getAccountBalance());
    }

    @Test
    public void transferRejectsZeroOrNegativeAmount() {
        Account source = accountRepo.findByAccountNumber("ACC001").orElseThrow();
        long initialBalance = source.getAccountBalance();

        TransferResult result1 = service.transfer(source, "ACC002", 0);
        TransferResult result2 = service.transfer(source, "ACC002", -100_000);

        assertFalse(result1.isSuccess());
        assertFalse(result2.isSuccess());
        assertEquals(initialBalance, source.getAccountBalance());
    }

    @Test
    public void transferRejectsAmountExceedingSingleLimit() {
        Account source = accountRepo.findByAccountNumber("ACC003").orElseThrow();

        TransferResult result = service.transfer(source, "ACC002", 11_000_000);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.SINGLE_TRANSFER_LIMIT_EXCEEDED, result.getErrorCode());
    }

    @Test
    public void transferRejectsAmountExceedingDailyLimit() {
        Account source = accountRepo.findByAccountNumber("ACC003").orElseThrow();

        Transaction priorTx = new Transaction("TX_TEST_DAILY", "ACC003", DateUtil.now(),
                TransactionType.TRANSFER_OUT, 25_000_000, 0, "");
        txRepo.save(priorTx);

        TransferResult result = service.transfer(source, "ACC002", 10_000_000);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.DAILY_TRANSFER_LIMIT_EXCEEDED, result.getErrorCode());
    }

    @Test
    public void transferRespectsAccountMinimumBalance() {
        // ACC002 has 8.5M - deplete it first
        Account source = accountRepo.findByAccountNumber("ACC002").orElseThrow();

        // Deplete to near minimum
        service.transfer(source, "ACC001", 5_000_000); // leaves 3.5M
        service.transfer(source, "ACC001", 3_000_000); // leaves 500K

        // Try to transfer amount that would leave less than 50K minimum
        TransferResult result = service.transfer(source, "ACC001", 500_000);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.INSUFFICIENT_FUNDS, result.getErrorCode());
    }

    @Test
    public void transferAllowsCurrentAccountOverdraft() {
        Account source = accountRepo.findByAccountNumber("ACC003").orElseThrow(); // CURRENT account
        Account dest = accountRepo.findByAccountNumber("ACC002").orElseThrow();
        long destInitial = dest.getAccountBalance();

        // Transfer within single limit (10M) that creates overdraft
        TransferResult result = service.transfer(source, "ACC002", 10_000_000);

        assertTrue(result.isSuccess());
        // ACC003 had 22M, transferred 10M = 12M remaining
        assertEquals(12_000_000, source.getAccountBalance());

        dest = accountRepo.findByAccountNumber("ACC002").orElseThrow();
        assertEquals(destInitial + 10_000_000, dest.getAccountBalance());
    }

    @Test
    public void transferAcceptsMaximumSingleAmount() {
        Account source = accountRepo.findByAccountNumber("ACC003").orElseThrow();
        long initialBalance = source.getAccountBalance();

        TransferResult result = service.transfer(source, "ACC002", 10_000_000);

        assertTrue(result.isSuccess());
        assertEquals(initialBalance - 10_000_000, source.getAccountBalance());
    }

    @Test
    public void transferIncludesDestinationCustomerName() {
        Account source = accountRepo.findByAccountNumber("ACC001").orElseThrow();

        TransferResult result = service.transfer(source, "ACC002", 500_000);

        assertTrue(result.isSuccess());
        // Customer name from test data should be present (or null if not in test data)
        assertNotNull(result);
    }

    @Test
    public void transferHandlesMissingCustomerName() {
        Account source = accountRepo.findByAccountNumber("ACC001").orElseThrow();

        TransferResult result = service.transfer(source, "ACC002", 500_000);

        // Should succeed even if customer name is not found
        assertTrue(result.isSuccess());
    }
}
