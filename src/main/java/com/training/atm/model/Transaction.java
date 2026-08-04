package com.training.atm.model;

import com.training.atm.model.enums.TransactionType;

/**
 * Immutable record of a single account transaction.
 *
 * Fix #5: all fields are {@code private final} — a DTO that can only be
 * created through its constructor has no mutable surface area.
 */
public class Transaction {
    private final String          transactionId;
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
}
