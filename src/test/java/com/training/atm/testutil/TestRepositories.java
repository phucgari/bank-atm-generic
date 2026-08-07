package com.training.atm.testutil;

import com.training.atm.model.*;
import com.training.atm.model.enums.TransactionType;
import com.training.atm.repository.*;

import java.util.*;

/**
 * In-memory repository implementations for isolated testing.
 */
public class TestRepositories {

    public static class InMemoryAccountRepository implements AccountRepository {
        private final Map<String, Account> accounts = new LinkedHashMap<>();

        public InMemoryAccountRepository(Account... accounts) {
            for (Account account : accounts) {
                this.accounts.put(account.getAccountNumber(), account);
            }
        }

        @Override
        public Optional<Account> findByAccountNumber(String accountNumber) {
            return Optional.ofNullable(accounts.get(accountNumber));
        }

        @Override
        public List<Account> findAll() {
            return new ArrayList<>(accounts.values());
        }

        @Override
        public Account update(Account account) {
            accounts.put(account.getAccountNumber(), account);
            return account;
        }

        @Override
        public void updateAll(Collection<Account> accounts) {
            accounts.forEach(this::update);
        }
    }

    public static class InMemoryTransactionRepository implements TransactionRepository {
        public final List<Transaction> saved = new ArrayList<>();

        @Override
        public Transaction save(Transaction transaction) {
            saved.add(transaction);
            return transaction;
        }

        @Override
        public List<Transaction> findByAccountNumber(String accountNumber) {
            return saved.stream()
                    .filter(tx -> tx.getAccountNumber().equals(accountNumber))
                    .toList();
        }

        @Override
        public long sumByAccountNumberTypeAndDate(String accountNumber, TransactionType type, String date) {
            return saved.stream()
                    .filter(tx -> tx.getAccountNumber().equals(accountNumber)
                            && tx.getType() == type
                            && tx.getDateTime().startsWith(date))
                    .mapToLong(Transaction::getAmount)
                    .sum();
        }
    }

    public static class InMemoryDenominationRepository implements DenominationRepository {
        public final Map<Long, Integer> denominations = new LinkedHashMap<>();
        public long depositedAmount;

        @Override
        public long getTotalCash() {
            return denominations.entrySet().stream()
                    .mapToLong(entry -> entry.getKey() * entry.getValue())
                    .sum();
        }

        @Override
        public Map<Long, Integer> getDenominations() {
            return new LinkedHashMap<>(denominations);
        }

        @Override
        public boolean isValidDenomination(long denomination) {
            return denominations.containsKey(denomination);
        }

        @Override
        public void dispenseBills(Map<Long, Integer> dispensed) {
            dispensed.forEach((denomination, count) ->
                    denominations.merge(denomination, -count, Integer::sum));
        }

        @Override
        public void addDepositCash(long amount) {
            depositedAmount += amount;
        }

        @Override
        public boolean replenish(long denomination, int count) {
            denominations.merge(denomination, count, Integer::sum);
            return true;
        }
    }

    public static class InMemoryCardRepository implements CardRepository {
        private final Map<String, ATMCard> cards = new LinkedHashMap<>();

        public InMemoryCardRepository(ATMCard... cards) {
            for (ATMCard card : cards) {
                this.cards.put(card.getCardId(), card);
            }
        }

        @Override
        public Optional<ATMCard> findById(String cardId) {
            return Optional.ofNullable(cards.get(cardId));
        }

        @Override
        public ATMCard update(ATMCard card) {
            cards.put(card.getCardId(), card);
            return card;
        }

        public List<ATMCard> findAll() {
            return new ArrayList<>(cards.values());
        }
    }

    public static class InMemoryATMInfoRepository implements ATMInfoRepository {
        private String location;
        private String branchName;

        public InMemoryATMInfoRepository(String location, String branchName) {
            this.location = location;
            this.branchName = branchName;
        }

        @Override
        public String getLocation() {
            return location;
        }

        @Override
        public String getBranchName() {
            return branchName;
        }

        @Override
        public long getMaxCapacity() {
            return 0;
        }
    }

    public static class InMemoryCustomerRepository implements CustomerRepository {
        private final Map<String, BankCustomer> customersByAccount = new LinkedHashMap<>();

        public InMemoryCustomerRepository(BankCustomer... customers) {
            for (BankCustomer customer : customers) {
                customersByAccount.put(customer.getAccountNumber(), customer);
            }
        }

        @Override
        public Optional<BankCustomer> findByAccountNumber(String accountNumber) {
            return Optional.ofNullable(customersByAccount.get(accountNumber));
        }

        @Override
        public Optional<BankCustomer> findByCardId(String cardId) {
            return customersByAccount.values().stream()
                    .filter(customer -> Objects.equals(cardId, customer.getCardId()))
                    .findFirst();
        }

        @Override
        public List<BankCustomer> findAll() { return new ArrayList<>(customersByAccount.values()); }
    }

    public static class InMemoryScheduledTransferRepository implements ScheduledTransferRepository {
        private final Map<String, ScheduledTransfer> transfers = new LinkedHashMap<>();

        @Override
        public List<ScheduledTransfer> findAllActive() {
            return transfers.values().stream()
                    .filter(ScheduledTransfer::canExecute)
                    .toList();
        }

        @Override
        public ScheduledTransfer save(ScheduledTransfer transfer) {
            transfers.put(transfer.getId(), transfer);
            return transfer;
        }

        @Override
        public ScheduledTransfer update(ScheduledTransfer transfer) {
            transfers.put(transfer.getId(), transfer);
            return transfer;
        }

        @Override
        public Optional<ScheduledTransfer> findById(String id) {
            return Optional.ofNullable(transfers.get(id));
        }

        @Override
        public List<ScheduledTransfer> findBySourceAccount(String accountNumber) {
            return transfers.values().stream()
                    .filter(transfer -> transfer.getSourceAccount().equals(accountNumber))
                    .toList();
        }

        @Override
        public long countActiveBySourceAccount(String accountNumber) {
            return findBySourceAccount(accountNumber).stream()
                    .filter(ScheduledTransfer::canExecute)
                    .count();
        }
    }
}
