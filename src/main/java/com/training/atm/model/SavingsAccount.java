package com.training.atm.model;

import com.training.atm.model.enums.AccountType;
import com.training.atm.model.strategy.CompoundInterestStrategy;
import com.training.atm.model.strategy.InterestStrategy;
import com.training.atm.util.FormatUtil;

public class SavingsAccount extends Account {
    public static final double MONTHLY_RATE = 0.005; // 0.5 % per month
    public static final long   MIN_BALANCE  = 50_000L;

    /**
     * Convenience constructor — uses the default {@link CompoundInterestStrategy}.
     * Existing code that creates a savings account without specifying a strategy
     * continues to work unchanged.
     */
    public SavingsAccount(String accountNumber, long balance, String lastInterestYearMonth) {
        this(accountNumber, balance, lastInterestYearMonth,
                new CompoundInterestStrategy(MONTHLY_RATE));
    }

    /**
     * Strategy-injecting constructor — allows a different algorithm to be
     * supplied at construction time (e.g. a promotional rate, a tiered strategy).
     */
    public SavingsAccount(String accountNumber, long balance,
                           String lastInterestYearMonth, InterestStrategy strategy) {
        super(accountNumber, balance, AccountType.SAVINGS, lastInterestYearMonth, strategy);
    }

    @Override public long   getWithdrawalFloor()  { return MIN_BALANCE; }
    @Override public String getAccountTypeLabel() { return "Savings Account"; }

    @Override
    public String getInsufficientFundsMessage(long requestedAmount) {
        return String.format(
                "Insufficient balance. After withdrawal: %s. Savings minimum balance: %s.",
                FormatUtil.formatVND(getAccountBalance() - requestedAmount),
                FormatUtil.formatVND(MIN_BALANCE));
    }
}
