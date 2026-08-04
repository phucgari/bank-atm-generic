package com.training.atm.model.strategy;

/**
 * Calculates one period of compound interest: {@code balance × rate}.
 * Used by {@link com.training.atm.model.SavingsAccount}.
 */
public class CompoundInterestStrategy implements InterestStrategy {
    private final double monthlyRate;

    public CompoundInterestStrategy(double monthlyRate) {
        this.monthlyRate = monthlyRate;
    }

    @Override
    public long calculate(long balance) {
        return Math.round(balance * monthlyRate);
    }

    @Override
    public double getMonthlyRate() { return monthlyRate; }
}
