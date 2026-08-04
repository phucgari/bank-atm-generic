package com.training.atm.validation.withdrawal;

import com.training.atm.model.Account;

/**
 * Carries all data required by the withdrawal validation chain.
 * Pre-fetching values (daily total, ATM cash) into the context avoids
 * multiple round-trips to repositories inside individual validators.
 */
public record WithdrawalContext(
        Account account,
        long    amount,
        long    dailyTotal,
        long    atmAvailableCash
) {}
