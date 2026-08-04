package com.training.atm.model;

import com.training.atm.model.enums.TransactionType;

/**
 * Record of a single account transaction.
 *
 * <p>Effectively immutable: all business fields are {@code private final} and
 * can only be set through the constructor.  The transaction ID is mutable only
 * through the {@link Identifiable} contract ({@link #setId(String)}), required
 * so the entity can be handled uniformly by the generic repository layer.
 */
public class Transaction implements Identifiable<String> {
    private String                transactionId;
    private final String          accountNumber;
    private final String          dateTime;      // "yyyy-MM-dd HH:mm:ss"
    private final TransactionType type;
    private final long            amount;
    private final long            balanceAfter;
    private final String          description;

    public Transaction(String transactionId, String accountNumber, String dateTime,
                       TransactionType type, long amount, long balanceAfter, String description) {
        this.transactionId = transactionId;
        this.accountNumber = accountNumber;
        this.dateTime      = dateTime;
        this.type          = type;
        this.amount        = amount;
        this.balanceAfter  = balanceAfter;
        this.description   = description;
    }

    public String          getTransactionId() { return transactionId; }
    public String          getAccountNumber() { return accountNumber; }
    public String          getDateTime()      { return dateTime; }
    public TransactionType getType()          { return type; }
    public long            getAmount()        { return amount; }
    public long            getBalanceAfter()  { return balanceAfter; }
    public String          getDescription()   { return description; }

    // -----------------------------------------------------------------------
    // Identifiable<String> — transaction ID is the identity key
    // -----------------------------------------------------------------------
    @Override public String getId()            { return transactionId; }
    @Override public void   setId(String id)   { this.transactionId = id; }
}
