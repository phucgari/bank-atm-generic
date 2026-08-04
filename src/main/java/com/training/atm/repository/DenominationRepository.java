package com.training.atm.repository;

import java.util.Map;

/**
 * Manages cash denomination inventory inside the ATM.
 * Segregated from ATM metadata (ISP) so that CashDispenser only depends
 * on denomination operations, not on unrelated location/branch data.
 */
public interface DenominationRepository {
    long getTotalCash();
    Map<Long, Integer> getDenominations();
    boolean isValidDenomination(long denomination);
    void dispenseBills(Map<Long, Integer> dispensed);
    void addDepositCash(long amount);
    boolean replenish(long denomination, int count);
}
