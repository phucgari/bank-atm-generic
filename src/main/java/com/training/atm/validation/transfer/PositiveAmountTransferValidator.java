package com.training.atm.validation.transfer;

import com.training.atm.validation.ValidationResult;
import com.training.atm.validation.ValidationRule;
import com.training.atm.dto.ErrorCode;

/** Rule: transfer amount must be positive. */
public class PositiveAmountTransferValidator implements ValidationRule<TransferContext> {
    @Override
    public ValidationResult validate(TransferContext ctx) {
        return ctx.amount() > 0
                ? ValidationResult.valid()
                : ValidationResult.invalid(ErrorCode.INVALID_AMOUNT, "Transfer amount must be positive.");
    }
}
