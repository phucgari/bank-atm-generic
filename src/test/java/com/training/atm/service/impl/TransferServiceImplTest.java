package com.training.atm.service.impl;

import com.training.atm.dto.ErrorCode;
import com.training.atm.dto.TransferResult;
import com.training.atm.model.Account;
import com.training.atm.model.BankCustomer;
import com.training.atm.model.CurrentAccount;
import com.training.atm.model.SavingsAccount;
import com.training.atm.model.Transaction;
import com.training.atm.model.enums.TransactionType;
import com.training.atm.testutil.TestRepositories.*;
import com.training.atm.util.DateUtil;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TransferServiceImplTest {

    private InMemoryAccountRepository accountRepo;
    private InMemoryTransactionRepository txRepo;
    private InMemoryCustomerRepository customerRepo;
    private InMemoryScheduledTransferRepository schedRepo;
    private TransferServiceImpl service;

    @Before
    public void setUp() {
        txRepo = new InMemoryTransactionRepository();
        customerRepo = new InMemoryCustomerRepository();
        schedRepo = new InMemoryScheduledTransferRepository();
    }

    @Test
    public void transferSucceedsWithValidAccounts() {
        Account source = new SavingsAccount("ACC001", 5_000_000, "");
        Account dest = new SavingsAccount("ACC002", 1_000_000, "");
        accountRepo = new InMemoryAccountRepository(source, dest);
        BankCustomer customer = new BankCustomer("CUST002", "Jane Doe", "", "", "", "ACC002");
        customerRepo = new InMemoryCustomerRepository(customer);
        service = new TransferServiceImpl(accountRepo, txRepo, schedRepo, customerRepo, null);

        TransferResult result = service.transfer(source, "ACC002", 500_000);

        assertTrue(result.isSuccess());
        assertEquals(4_500_000, source.getAccountBalance());
        assertEquals(1_500_000, dest.getAccountBalance());
        assertEquals(2, txRepo.saved.size());
    }

    @Test
    public void transferCreatesOutAndInTransactions() {
        Account source = new SavingsAccount("ACC001", 5_000_000, "");
        Account dest = new SavingsAccount("ACC002", 1_000_000, "");
        accountRepo = new InMemoryAccountRepository(source, dest);
        customerRepo = new InMemoryCustomerRepository();
        service = new TransferServiceImpl(accountRepo, txRepo, schedRepo, customerRepo, null);

        service.transfer(source, "ACC002", 500_000);

        assertEquals(2, txRepo.saved.size());
        assertEquals(TransactionType.TRANSFER_OUT, txRepo.saved.get(0).getType());
        assertEquals(TransactionType.TRANSFER_IN, txRepo.saved.get(1).getType());
        assertEquals("ACC001", txRepo.saved.get(0).getAccountNumber());
        assertEquals("ACC002", txRepo.saved.get(1).getAccountNumber());
    }

    @Test
    public void transferRejectsSameSourceAndDestination() {
        Account source = new SavingsAccount("ACC001", 5_000_000, "");
        accountRepo = new InMemoryAccountRepository(source);
        service = new TransferServiceImpl(accountRepo, txRepo, schedRepo, customerRepo, null);

        TransferResult result = service.transfer(source, "ACC001", 100_000);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.SAME_ACCOUNT, result.getErrorCode());
        assertEquals(5_000_000, source.getAccountBalance());
        assertTrue(txRepo.saved.isEmpty());
    }

    @Test
    public void transferRejectsNonExistentDestination() {
        Account source = new SavingsAccount("ACC001", 5_000_000, "");
        accountRepo = new InMemoryAccountRepository(source);
        service = new TransferServiceImpl(accountRepo, txRepo, schedRepo, customerRepo, null);

        TransferResult result = service.transfer(source, "ACC999", 100_000);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, result.getErrorCode());
        assertTrue(txRepo.saved.isEmpty());
    }

    @Test
    public void transferRejectsZeroOrNegativeAmount() {
        Account source = new SavingsAccount("ACC001", 5_000_000, "");
        Account dest = new SavingsAccount("ACC002", 1_000_000, "");
        accountRepo = new InMemoryAccountRepository(source, dest);
        service = new TransferServiceImpl(accountRepo, txRepo, schedRepo, customerRepo, null);

        TransferResult result1 = service.transfer(source, "ACC002", 0);
        TransferResult result2 = service.transfer(source, "ACC002", -100_000);

        assertFalse(result1.isSuccess());
        assertFalse(result2.isSuccess());
        assertEquals(5_000_000, source.getAccountBalance());
    }

    @Test
    public void transferRejectsAmountExceedingSingleLimit() {
        Account source = new SavingsAccount("ACC001", 100_000_000, "");
        Account dest = new SavingsAccount("ACC002", 1_000_000, "");
        accountRepo = new InMemoryAccountRepository(source, dest);
        service = new TransferServiceImpl(accountRepo, txRepo, schedRepo, customerRepo, null);

        TransferResult result = service.transfer(source, "ACC002", 11_000_000);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.SINGLE_TRANSFER_LIMIT_EXCEEDED, result.getErrorCode());
    }

    @Test
    public void transferRejectsAmountExceedingDailyLimit() {
        Account source = new SavingsAccount("ACC001", 100_000_000, "");
        Account dest = new SavingsAccount("ACC002", 1_000_000, "");
        accountRepo = new InMemoryAccountRepository(source, dest);
        service = new TransferServiceImpl(accountRepo, txRepo, schedRepo, customerRepo, null);

        Transaction priorTx = new Transaction("TX001", "ACC001", DateUtil.now(),
                TransactionType.TRANSFER_OUT, 25_000_000, 75_000_000, "");
        txRepo.saved.add(priorTx);

        TransferResult result = service.transfer(source, "ACC002", 10_000_000);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.DAILY_TRANSFER_LIMIT_EXCEEDED, result.getErrorCode());
    }

    @Test
    public void transferRespectsAccountMinimumBalance() {
        Account source = new SavingsAccount("ACC001", 100_000, "");
        Account dest = new SavingsAccount("ACC002", 1_000_000, "");
        accountRepo = new InMemoryAccountRepository(source, dest);
        service = new TransferServiceImpl(accountRepo, txRepo, schedRepo, customerRepo, null);

        TransferResult result = service.transfer(source, "ACC002", 100_000);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.INSUFFICIENT_FUNDS, result.getErrorCode());
    }

    @Test
    public void transferAllowsCurrentAccountOverdraft() {
        Account source = new CurrentAccount("ACC001", 500_000, "");
        Account dest = new SavingsAccount("ACC002", 1_000_000, "");
        accountRepo = new InMemoryAccountRepository(source, dest);
        service = new TransferServiceImpl(accountRepo, txRepo, schedRepo, customerRepo, null);

        TransferResult result = service.transfer(source, "ACC002", 1_000_000);

        assertTrue(result.isSuccess());
        assertEquals(-500_000, source.getAccountBalance());
        assertEquals(2_000_000, dest.getAccountBalance());
    }

    @Test
    public void transferAcceptsMaximumSingleAmount() {
        Account source = new SavingsAccount("ACC001", 60_000_000, "");
        Account dest = new SavingsAccount("ACC002", 1_000_000, "");
        accountRepo = new InMemoryAccountRepository(source, dest);
        service = new TransferServiceImpl(accountRepo, txRepo, schedRepo, customerRepo, null);

        TransferResult result = service.transfer(source, "ACC002", 10_000_000);

        assertTrue(result.isSuccess());
        assertEquals(50_000_000, source.getAccountBalance());
        assertEquals(11_000_000, dest.getAccountBalance());
    }

    @Test
    public void transferIncludesDestinationCustomerName() {
        Account source = new SavingsAccount("ACC001", 5_000_000, "");
        Account dest = new SavingsAccount("ACC002", 1_000_000, "");
        accountRepo = new InMemoryAccountRepository(source, dest);
        BankCustomer customer = new BankCustomer("CUST002", "John Smith", "", "", "", "ACC002");
        customerRepo = new InMemoryCustomerRepository(customer);
        service = new TransferServiceImpl(accountRepo, txRepo, schedRepo, customerRepo, null);

        TransferResult result = service.transfer(source, "ACC002", 500_000);

        assertTrue(result.isSuccess());
        assertEquals("John Smith", result.getDestCustomerName());
    }

    @Test
    public void transferHandlesMissingCustomerName() {
        Account source = new SavingsAccount("ACC001", 5_000_000, "");
        Account dest = new SavingsAccount("ACC002", 1_000_000, "");
        accountRepo = new InMemoryAccountRepository(source, dest);
        customerRepo = new InMemoryCustomerRepository();
        service = new TransferServiceImpl(accountRepo, txRepo, schedRepo, customerRepo, null);

        TransferResult result = service.transfer(source, "ACC002", 500_000);

        assertTrue(result.isSuccess());
    }
}
