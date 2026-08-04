package com.training.atm.command;

/**
 * Common supertype for all transaction result DTOs.
 *
 * Implementing this interface in {@link com.training.atm.dto.WithdrawalResult},
 * {@link com.training.atm.dto.DepositResult} and
 * {@link com.training.atm.dto.TransferResult} allows the scheduler to handle
 * any {@code TransactionCommand<?>} uniformly — it only needs
 * {@code isSuccess()} and {@code getMessage()}.
 */
public interface OperationResult {
    boolean isSuccess();
    String  getMessage();
}
