package com.training.atm.dto;

import com.training.atm.command.OperationResult;
import com.training.atm.model.Transaction;

/** Immutable result object for a deposit operation. Implements {@link OperationResult}. */
public final class DepositResult implements OperationResult {
    private final boolean     success;
    private final String      message;
    private final Transaction transaction;

    private DepositResult(boolean success, String message, Transaction transaction) {
        this.success     = success;
        this.message     = message;
        this.transaction = transaction;
    }

    public static DepositResult success(Transaction tx)      { return new DepositResult(true,  null,    tx); }
    public static DepositResult failure(String message)      { return new DepositResult(false, message, null); }

    @Override public boolean     isSuccess()          { return success; }
    @Override public String      getMessage()         { return message; }
    public    Transaction        getTransaction()     { return transaction; }
}
