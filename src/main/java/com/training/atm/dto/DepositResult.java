package com.training.atm.dto;

import com.training.atm.command.OperationResult;
import com.training.atm.model.Transaction;

/**
 * Immutable result object for a deposit operation. Implements {@link OperationResult}.
 */
public final class DepositResult extends ServiceResult<Transaction> implements OperationResult {

    private DepositResult(boolean success, String message, Transaction transaction) {
        super(success, transaction, message, ErrorCode.SUCCESS);
    }

    public static DepositResult success(Transaction tx) {
        return new DepositResult(true, null, tx);
    }

    public static DepositResult failure(String message) {
        return new DepositResult(false, message, null);
    }

    public Transaction getTransaction() {
        return getData();
    }
}
