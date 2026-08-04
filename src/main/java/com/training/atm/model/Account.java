package com.training.atm.model;

import com.training.atm.model.enums.AccountType;
import com.training.atm.model.strategy.InterestStrategy;

/**
 * Abstract base for all account types.
 *
 * <h3>Strategy (interest calculation)</h3>
 * {@code calculateInterest()} and {@code getInterestRate()} delegate to an
 * injected {@link InterestStrategy}.  Changing the interest formula for a
 * given account type no longer requires touching the Account hierarchy — only
 * a new strategy class is needed.
 *
 * <h3>OCP (withdrawal)</h3>
 * {@code verifyWithdrawAmount()} and {@code getInsufficientFundsMessage()} are
 * still polymorphic via the abstract methods {@code getWithdrawalFloor()} and
 * the message builder — no type-switches required.
 *
 * <h3>Encapsulation</h3>
 * All fields are {@code private}.  The protected constructor is the only write
 * path for subclass initialisation; direct field mutation from subclasses is
 * not possible.
 */
public abstract class Account implements Identifiable<String> {
    private String                accountNumber;
    private       long            balance;
    private final AccountType     accountType;
    private       String          lastInterestYearMonth;  // "YYYY-MM" or ""
    private final InterestStrategy interestStrategy;      // Strategy pattern

    protected Account(String accountNumber, long balance,
                      AccountType accountType, String lastInterestYearMonth,
                      InterestStrategy interestStrategy) {
        this.accountNumber         = accountNumber;
        this.balance               = balance;
        this.accountType           = accountType;
        this.lastInterestYearMonth = (lastInterestYearMonth != null) ? lastInterestYearMonth : "";
        this.interestStrategy      = interestStrategy;
    }

    // -----------------------------------------------------------------------
    // Strategy delegation — no longer abstract; algorithm lives in the strategy
    // -----------------------------------------------------------------------

    /** Delegates to the injected {@link InterestStrategy}. */
    public long calculateInterest() {
        return interestStrategy.calculate(balance);
    }

    /** Monthly rate exposed for informational display. */
    public double getInterestRate() {
        return interestStrategy.getMonthlyRate();
    }

    // -----------------------------------------------------------------------
    // Abstract contract: subclasses define withdrawal constraints and labels
    // -----------------------------------------------------------------------

    public abstract long   getWithdrawalFloor();
    public abstract String getAccountTypeLabel();
    public abstract String getInsufficientFundsMessage(long requestedAmount);

    // -----------------------------------------------------------------------
    // Non-abstract operations
    // -----------------------------------------------------------------------

    public boolean verifyWithdrawAmount(long amount) {
        return (balance - amount) >= getWithdrawalFloor();
    }

    public void updateBalance(long newBalance) {
        this.balance = newBalance;
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------
    public long        getAccountBalance()       { return balance; }
    public String      getAccountNumber()         { return accountNumber; }
    public AccountType getAccountType()           { return accountType; }
    public String      getLastInterestYearMonth() { return lastInterestYearMonth; }

    public void setLastInterestYearMonth(String yearMonth) {
        this.lastInterestYearMonth = (yearMonth != null) ? yearMonth : "";
    }

    // -----------------------------------------------------------------------
    // Identifiable<String> — account number is the identity key
    // -----------------------------------------------------------------------
    @Override public String getId()            { return accountNumber; }
    @Override public void   setId(String id)   { this.accountNumber = id; }
}
