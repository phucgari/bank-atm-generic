package com.training.atm.dto;

import com.training.atm.command.OperationResult;
import com.training.atm.model.Transaction;

import java.util.Map;

/**
 * Immutable result object for a withdrawal operation.
 * Implements {@link OperationResult} so the scheduler can handle any
 * {@code TransactionCommand<?>} uniformly.
 */
public final class WithdrawalResult implements OperationResult {
    private final boolean            success;
    private final String             message;
    private final Transaction        transaction;
    private final Map<Long, Integer> dispensed;
    private final long               remainingAtmCash;

    private WithdrawalResult(boolean success, String message, Transaction transaction,
                              Map<Long, Integer> dispensed, long remainingAtmCash) {
        this.success          = success;
        this.message          = message;
        this.transaction      = transaction;
        this.dispensed        = dispensed;
        this.remainingAtmCash = remainingAtmCash;
    }

    public static WithdrawalResult success(Transaction tx, Map<Long, Integer> dispensed, long remainingCash) {
        return new WithdrawalResult(true, null, tx, dispensed, remainingCash);
    }

    public static WithdrawalResult failure(String message) {
        return new WithdrawalResult(false, message, null, null, 0);
    }

    @Override public boolean            isSuccess()          { return success; }
    @Override public String             getMessage()         { return message; }
    public    Transaction               getTransaction()     { return transaction; }
    public    Map<Long, Integer>        getDispensed()       { return dispensed; }
    public    long                      getRemainingAtmCash(){ return remainingAtmCash; }
}
