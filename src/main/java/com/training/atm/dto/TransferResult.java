package com.training.atm.dto;

import com.training.atm.command.OperationResult;
import com.training.atm.model.Transaction;

/** Immutable result object for a fund-transfer operation. Implements {@link OperationResult}. */
public final class TransferResult implements OperationResult {
    private final boolean     success;
    private final String      message;
    private final Transaction transaction;
    private final String      destCustomerName;

    private TransferResult(boolean success, String message,
                            Transaction transaction, String destCustomerName) {
        this.success          = success;
        this.message          = message;
        this.transaction      = transaction;
        this.destCustomerName = destCustomerName;
    }

    public static TransferResult success(Transaction tx, String destCustomerName) {
        return new TransferResult(true,  null,    tx, destCustomerName);
    }

    public static TransferResult failure(String message) {
        return new TransferResult(false, message, null, null);
    }

    @Override public boolean     isSuccess()          { return success; }
    @Override public String      getMessage()         { return message; }
    public    Transaction        getTransaction()     { return transaction; }
    public    String             getDestCustomerName(){ return destCustomerName; }
}
