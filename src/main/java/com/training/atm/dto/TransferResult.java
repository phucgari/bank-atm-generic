package com.training.atm.dto;

import com.training.atm.command.OperationResult;
import com.training.atm.model.Transaction;

/**
 * Immutable result object for a fund-transfer operation. Implements {@link OperationResult}.
 */
public final class TransferResult extends ServiceResult<Transaction> implements OperationResult {
    private final String destCustomerName;

    private TransferResult(boolean success, String message,
                           Transaction transaction, String destCustomerName, ErrorCode errorCode) {
        super(success, transaction, message, errorCode);
        this.destCustomerName = destCustomerName;
    }

    public static TransferResult success(Transaction tx, String destCustomerName) {
        return new TransferResult(true, null, tx, destCustomerName, null);
    }

    public static TransferResult failure(ErrorCode errorCode) {
        return new TransferResult(false, errorCode.format(), null, null, errorCode);
    }

    public Transaction getTransaction() {
        return getData();
    }

    public String getDestCustomerName() {
        return destCustomerName;
    }
}
