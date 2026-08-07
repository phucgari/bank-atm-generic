package com.training.atm.dto;

import java.util.Locale;

public enum ErrorCode {
    INVALID_DEPOSIT_AMOUNT("Deposit amount must be a positive multiple of %s."),
    INVALID_WITHDRAWAL_AMOUNT("Withdrawal amount must be a positive multiple of %s."),
    INVALID_TRANSFER_AMOUNT("Transfer amount must be positive."),
    SINGLE_DEPOSIT_LIMIT_EXCEEDED("Single deposit limit is %s."),
    SINGLE_WITHDRAWAL_LIMIT_EXCEEDED("Single withdrawal limit is %s."),
    DAILY_WITHDRAWAL_LIMIT_EXCEEDED("Daily withdrawal limit is %s."),
    SINGLE_TRANSFER_LIMIT_EXCEEDED("Single transfer limit is %s."),
    DAILY_TRANSFER_LIMIT_EXCEEDED("Daily transfer limit is %s."),
    ACCOUNT_NOT_FOUND("Destination account %s not found."),
    SAME_ACCOUNT("Source and destination accounts must be different."),
    INSUFFICIENT_FUNDS("%s"),
    ATM_CASH_UNAVAILABLE("The ATM does not have sufficient cash for this transaction."),
    ATM_CASH_DISPENSE_UNAVAILABLE("The ATM cannot dispense the exact amount with available bills."),
    INTERNAL_ERROR("An internal server error occurred.");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String format(Object... values) {
        return values.length == 0
                ? message
                : String.format(Locale.ROOT, message, values);
    }
}
