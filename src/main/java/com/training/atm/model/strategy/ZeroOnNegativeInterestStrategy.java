package com.training.atm.model.strategy;

/**
 * Calculates simple interest but returns zero when the balance is negative
 * (overdrawn).  Used by {@link com.training.atm.model.CurrentAccount}.
 *
 * Swapping this strategy for a {@link CompoundInterestStrategy} is the only
 * change required to alter a current account's interest behaviour — the
 * Account class and all service classes remain untouched.
 */
public class ZeroOnNegativeInterestStrategy implements InterestStrategy {
    private final double monthlyRate;

    public ZeroOnNegativeInterestStrategy(double monthlyRate) {
        this.monthlyRate = monthlyRate;
    }

    @Override
    public long calculate(long balance) {
        return balance <= 0 ? 0L : Math.round(balance * monthlyRate);
    }

    @Override
    public double getMonthlyRate() { return monthlyRate; }
}
