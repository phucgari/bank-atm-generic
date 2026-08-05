package com.training.atm.dto;

import com.training.atm.command.OperationResult;
import com.training.atm.model.Transaction;

/**
 * Immutable result object for a fund-transfer operation. Implements {@link OperationResult}.
 */
public final class TransferResult extends ServiceResult<Transaction> implements OperationResult {
    private final String destCustomerName;

    private TransferResult(boolean success, String message,
                           Transaction transaction, String destCustomerName) {
        super(success, transaction, message, ErrorCode.SUCCESS);
        this.destCustomerName = destCustomerName;
    }

    public static TransferResult success(Transaction tx, String destCustomerName) {
        return new TransferResult(true, null, tx, destCustomerName);
    }

    public static TransferResult failure(String message) {
        return new TransferResult(false, message, null, null);
    }

    public Transaction getTransaction() {
        return getData();
    }

    public String getDestCustomerName() {
        return destCustomerName;
    }
}
