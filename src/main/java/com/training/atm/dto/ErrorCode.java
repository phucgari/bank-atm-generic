package com.training.atm.dto;

public enum ErrorCode {
    INVALID_AMOUNT("The transaction amount is invalid."),
    LIMIT_EXCEEDED("The transaction limit would be exceeded."),
    ACCOUNT_NOT_FOUND("The requested account was not found."),
    SAME_ACCOUNT("Source and destination accounts must be different."),
    INSUFFICIENT_FUNDS("The account has insufficient funds."),
    ATM_CASH_UNAVAILABLE("The ATM does not have sufficient cash."),
    INTERNAL_ERROR("An internal server error occurred.");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
