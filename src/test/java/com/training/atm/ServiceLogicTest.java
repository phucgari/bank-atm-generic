package com.training.atm;

import com.training.atm.dto.ErrorCode;
import com.training.atm.dto.ServiceResult;
import com.training.atm.dto.WithdrawalResult;
import com.training.atm.event.EventBus;
import com.training.atm.event.TransferEvent;
import com.training.atm.event.WithdrawalEvent;
import com.training.atm.model.Account;
import com.training.atm.model.SavingsAccount;
import com.training.atm.model.Transaction;
import com.training.atm.model.enums.TransactionType;
import com.training.atm.repository.AccountRepository;
import com.training.atm.repository.DenominationRepository;
import com.training.atm.repository.TransactionRepository;
import com.training.atm.service.CashDispenser;
import com.training.atm.service.impl.DepositServiceImpl;
import com.training.atm.service.impl.TransferServiceImpl;
import com.training.atm.service.impl.WithdrawalServiceImpl;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ServiceLogicTest {

    @Test
    public void depositRejectsInvalidDenomination() {
        Account account = new SavingsAccount("ACC001", 100_000, "");
        InMemoryTransactionRepository transactions = new InMemoryTransactionRepository();
        DepositServiceImpl service = new DepositServiceImpl(
                new InMemoryAccountRepository(account), transactions,
                new CashDispenser(new InMemoryDenominationRepository()));

        ServiceResult<Transaction> result = service.deposit(account, 15_000);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.INVALID_AMOUNT, result.getErrorCode());
        assertEquals(100_000, account.getAccountBalance());
        assertTrue(transactions.saved.isEmpty());
    }

    @Test
    public void depositPersistsTransactionAndUpdatesBalanceAndAtmCash() {
        Account account = new SavingsAccount("ACC001", 100_000, "");
        InMemoryTransactionRepository transactions = new InMemoryTransactionRepository();
        InMemoryDenominationRepository denominations = new InMemoryDenominationRepository();
        DepositServiceImpl service = new DepositServiceImpl(
                new InMemoryAccountRepository(account), transactions, new CashDispenser(denominations));

        ServiceResult<Transaction> result = service.deposit(account, 100_000);

        assertTrue(result.isSuccess());
        assertEquals(200_000, account.getAccountBalance());
        assertEquals(1, transactions.saved.size());
        assertEquals(100_000, denominations.depositedAmount);
    }

    @Test
    public void withdrawalPersistsTransactionAndDispensesCash() {
        Account account = new SavingsAccount("ACC001", 500_000, "");
        InMemoryTransactionRepository transactions = new InMemoryTransactionRepository();
        InMemoryDenominationRepository denominations = new InMemoryDenominationRepository();
        denominations.denominations.put(100_000L, 5);
        WithdrawalServiceImpl service = new WithdrawalServiceImpl(
                new InMemoryAccountRepository(account), transactions, new CashDispenser(denominations));

        WithdrawalResult result = service.withdraw(account, 100_000);

        assertTrue(result.isSuccess());
        assertNull(result.getErrorCode());
        assertEquals(400_000, account.getAccountBalance());
        assertEquals(1, transactions.saved.size());
        assertEquals(4, denominations.denominations.get(100_000L).intValue());
    }

    @Test
    public void withdrawalRejectsWhenAtmCashIsInsufficient() {
        Account account = new SavingsAccount("ACC001", 500_000, "");
        InMemoryTransactionRepository transactions = new InMemoryTransactionRepository();
        InMemoryDenominationRepository denominations = new InMemoryDenominationRepository();
        denominations.denominations.put(100_000L, 1);
        WithdrawalServiceImpl service = new WithdrawalServiceImpl(
                new InMemoryAccountRepository(account), transactions, new CashDispenser(denominations));

        WithdrawalResult result = service.withdraw(account, 200_000);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.ATM_CASH_UNAVAILABLE, result.getErrorCode());
        assertEquals(500_000, account.getAccountBalance());
        assertTrue(transactions.saved.isEmpty());
    }

    @Test
    public void transferRejectsSameSourceAndDestinationAccount() {
        Account account = new SavingsAccount("ACC001", 500_000, "");
        TransferServiceImpl service = new TransferServiceImpl(
                new InMemoryAccountRepository(account), new InMemoryTransactionRepository(),
                null, null, null);

        ServiceResult<Transaction> result = service.transfer(account, "ACC001", 100_000);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.SAME_ACCOUNT, result.getErrorCode());
    }

    @Test
    public void serviceResultMapPreservesFailureAndTransformsSuccess() {
        ServiceResult<Integer> success = ServiceResult.success(10);
        ServiceResult<Integer> failure = ServiceResult.failure("Invalid", ErrorCode.INVALID_AMOUNT);

        assertEquals(Integer.valueOf(20), success.map(value -> value * 2).getData());
        assertEquals(ErrorCode.INVALID_AMOUNT, failure.map(value -> value * 2).getErrorCode());
    }

    @Test
    public void eventBusDispatchesOnlyMatchingEventTypes() {
        EventBus eventBus = new EventBus();
        AtomicInteger withdrawalEvents = new AtomicInteger();
        eventBus.subscribe(WithdrawalEvent.class, event -> withdrawalEvents.incrementAndGet());

        Transaction transaction = new Transaction("TX001", "ACC001", "2026-08-07 12:00:00",
                TransactionType.WITHDRAWAL, 100_000, 400_000, "");
        eventBus.publish(new TransferEvent(transaction));
        eventBus.publish(new WithdrawalEvent(transaction));

        assertEquals(1, withdrawalEvents.get());
    }

    private static final class InMemoryAccountRepository implements AccountRepository {
        private final Map<String, Account> accounts = new LinkedHashMap<>();

        private InMemoryAccountRepository(Account... accounts) {
            for (Account account : accounts) {
                this.accounts.put(account.getAccountNumber(), account);
            }
        }

        @Override public Optional<Account> findByAccountNumber(String accountNumber) {
            return Optional.ofNullable(accounts.get(accountNumber));
        }
        @Override public List<Account> findAll() { return new ArrayList<>(accounts.values()); }
        @Override public Account update(Account account) { accounts.put(account.getAccountNumber(), account); return account; }
        @Override public void updateAll(Collection<Account> accounts) { accounts.forEach(this::update); }
    }

    private static final class InMemoryTransactionRepository implements TransactionRepository {
        private final List<Transaction> saved = new ArrayList<>();

        @Override public Transaction save(Transaction transaction) { saved.add(transaction); return transaction; }
        @Override public List<Transaction> findByAccountNumber(String accountNumber) { return List.of(); }
        @Override public long sumByAccountNumberTypeAndDate(
                String accountNumber, TransactionType type, String date) { return 0; }
    }

    private static final class InMemoryDenominationRepository implements DenominationRepository {
        private final Map<Long, Integer> denominations = new LinkedHashMap<>();
        private long depositedAmount;

        @Override public long getTotalCash() {
            return denominations.entrySet().stream().mapToLong(entry -> entry.getKey() * entry.getValue()).sum();
        }
        @Override public Map<Long, Integer> getDenominations() { return new LinkedHashMap<>(denominations); }
        @Override public boolean isValidDenomination(long denomination) { return denominations.containsKey(denomination); }
        @Override public void dispenseBills(Map<Long, Integer> dispensed) {
            dispensed.forEach((denomination, count) -> denominations.merge(denomination, -count, Integer::sum));
        }
        @Override public void addDepositCash(long amount) { depositedAmount += amount; }
        @Override public boolean replenish(long denomination, int count) { return false; }
    }
}
