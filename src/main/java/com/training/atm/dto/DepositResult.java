package com.training.atm.dto;

import com.training.atm.command.OperationResult;
import com.training.atm.model.Transaction;

/**
 * Immutable result object for a deposit operation. Implements {@link OperationResult}.
 */
public final class DepositResult extends ServiceResult<Transaction> implements OperationResult {

    private DepositResult(boolean success, String message, Transaction transaction, ErrorCode errorCode) {
        super(success, transaction, message, errorCode);
    }

    public static DepositResult success(Transaction tx) {
        return new DepositResult(true, null, tx, null);
    }

    public static DepositResult failure(String message) {
        return failure(message, ErrorCode.INTERNAL_ERROR);
    }

    public static DepositResult failure(String message, ErrorCode errorCode) {
        return new DepositResult(false, message, null, errorCode);
    }

    public Transaction getTransaction() {
        return getData();
    }
}
