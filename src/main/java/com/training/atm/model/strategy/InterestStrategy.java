package com.training.atm.model.strategy;

/**
 * Strategy interface for monthly interest calculation.
 *
 * Applying the Strategy pattern here decouples the interest algorithm from
 * the Account hierarchy.  A new rate scheme (e.g. tiered, promotional,
 * fixed-deposit) is introduced by adding an {@code InterestStrategy}
 * implementation — no Account subclass needs to change.
 */
public interface InterestStrategy {
    /**
     * Calculates interest for one calendar month.
     *
     * @param balance the current account balance in VND
     * @return interest amount in VND (≥ 0)
     */
    long calculate(long balance);

    /** Monthly rate used for informational display (e.g. 0.005 for 0.5 %). */
    double getMonthlyRate();
}
