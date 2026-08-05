package com.training.atm.dto;

import com.training.atm.command.OperationResult;
import com.training.atm.model.Transaction;

import java.util.Map;

/**
 * Immutable result object for a withdrawal operation.
 * Implements {@link OperationResult} so the scheduler can handle any
 * {@code TransactionCommand<?>} uniformly.
 */
public final class WithdrawalResult extends ServiceResult<Transaction> implements OperationResult {
    private final Map<Long, Integer> dispensed;
    private final long remainingAtmCash;

    private WithdrawalResult(boolean success, String message, Transaction transaction,
                             Map<Long, Integer> dispensed, long remainingAtmCash) {
        super(success, transaction, message, ErrorCode.SUCCESS);
        this.dispensed = dispensed;
        this.remainingAtmCash = remainingAtmCash;
    }

    public static WithdrawalResult success(Transaction tx, Map<Long, Integer> dispensed, long remainingCash) {
        return new WithdrawalResult(true, null, tx, dispensed, remainingCash);
    }

    public static WithdrawalResult failure(String message) {
        return new WithdrawalResult(false, message, null, null, 0);
    }

    public Transaction getTransaction() {
        return getData();
    }

    public Map<Long, Integer> getDispensed() {
        return dispensed;
    }

    public long getRemainingAtmCash() {
        return remainingAtmCash;
    }
}
