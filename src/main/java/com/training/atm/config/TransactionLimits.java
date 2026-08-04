package com.training.atm.config;

/**
 * Central place for all transaction business-rule constants.
 * Extracting these from ATMService follows SRP: rules change for one reason,
 * service orchestration changes for a different reason.
 */
public final class TransactionLimits {
    public static final long MAX_WITHDRAWAL_SINGLE = 5_000_000L;
    public static final long MAX_WITHDRAWAL_DAILY  = 20_000_000L;
    public static final long MAX_DEPOSIT_SINGLE    = 50_000_000L;
    public static final long MAX_TRANSFER_SINGLE   = 10_000_000L;
    public static final long MAX_TRANSFER_DAILY    = 30_000_000L;
    public static final int  MAX_SCHEDULED_ACTIVE  = 5;
    public static final long WITHDRAWAL_DENOMINATION = 50_000L;
    public static final long DEPOSIT_DENOMINATION    = 10_000L;
    public static final long REPLENISH_DENOMINATION  = 100_000L;

    private TransactionLimits() { /* utility class — no instances */ }
}
