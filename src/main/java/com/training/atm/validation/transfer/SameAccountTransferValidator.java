package com.training.atm.validation.transfer;

import com.training.atm.validation.ValidationResult;
import com.training.atm.validation.ValidationRule;
import com.training.atm.dto.ErrorCode;

/** Rule: source and destination accounts must be different. */
public class SameAccountTransferValidator implements ValidationRule<TransferContext> {
    @Override
    public ValidationResult validate(TransferContext ctx) {
        return ctx.source().getAccountNumber().equals(ctx.destAccountNumber())
                ? ValidationResult.invalid(ErrorCode.SAME_ACCOUNT)
                : ValidationResult.valid();
    }
}
