package com.training.atm.model;

import com.training.atm.model.enums.AccountType;
import com.training.atm.model.strategy.InterestStrategy;
import com.training.atm.model.strategy.ZeroOnNegativeInterestStrategy;
import com.training.atm.util.FormatUtil;

public class CurrentAccount extends Account {
    public static final double MONTHLY_RATE    = 0.001;         // 0.1 % per month
    public static final long   OVERDRAFT_LIMIT = -1_000_000L;   // maximum allowed negative balance
    private final long overdraftLimit;

    /**
     * Convenience constructor — uses the default
     * {@link ZeroOnNegativeInterestStrategy}.
     */
    public CurrentAccount(String accountNumber, long balance, String lastInterestYearMonth) {
        this(accountNumber, balance, lastInterestYearMonth,
                new ZeroOnNegativeInterestStrategy(MONTHLY_RATE));
    }

    /** Strategy-injecting constructor. */
    public CurrentAccount(String accountNumber, long balance,
                           String lastInterestYearMonth, InterestStrategy strategy) {
        this(accountNumber, balance, lastInterestYearMonth, strategy, OVERDRAFT_LIMIT);
    }

    public CurrentAccount(String accountNumber, long balance,
                           String lastInterestYearMonth, InterestStrategy strategy,
                           long overdraftLimit) {
        super(accountNumber, balance, AccountType.CURRENT, lastInterestYearMonth, strategy);
        this.overdraftLimit = overdraftLimit;
    }

    @Override public long   getWithdrawalFloor()  { return overdraftLimit; }
    @Override public String getAccountTypeLabel() { return "Current Account"; }

    @Override
    public String getInsufficientFundsMessage(long requestedAmount) {
        return String.format(
                "Insufficient balance. After withdrawal: %s. Overdraft limit: %s.",
                FormatUtil.formatVND(getAccountBalance() - requestedAmount),
                FormatUtil.formatVND(overdraftLimit));
    }
}
