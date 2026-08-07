package com.training.atm.testutil;

import com.training.atm.model.ATMCard;
import com.training.atm.repository.*;

import java.util.*;

/**
 * In-memory repository implementations for isolated unit testing of individual components.
 * Service integration tests should use TestDataManager and real file repositories instead.
 */
public class TestRepositories {

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
}
