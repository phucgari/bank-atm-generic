package com.training.atm.validation;

import com.training.atm.dto.ErrorCode;

public final class ValidationResult {
    private final boolean valid;
    private final String errorMessage;
    private final ErrorCode errorCode;

    private ValidationResult(boolean valid, String errorMessage, ErrorCode errorCode) {
        this.valid = valid;
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
    }

    public static ValidationResult valid() {
        return new ValidationResult(true, null, null);
    }

    public static ValidationResult invalid(ErrorCode errorCode, Object... values) {
        return new ValidationResult(false, errorCode.format(values), errorCode);
    }

    public boolean isValid() {
        return valid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
