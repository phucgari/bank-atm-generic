package com.training.atm.validation.transfer;

import com.training.atm.model.Account;

/**
 * Carries all data required by the transfer validation chain.
 *
 * {@code destAccount} is pre-fetched by the service before building the
 * context; it is {@code null} when the destination account does not exist.
 * The {@link DestinationExistsValidator} checks for this before later
 * validators (which assume a non-null destination) run.
 */
public record TransferContext(
        Account source,
        String  destAccountNumber,
        Account destAccount,        // null → account not found
        long    amount,
        long    dailyTotal
) {}
