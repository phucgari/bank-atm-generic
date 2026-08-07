package com.training.atm.validation.transfer;

import com.training.atm.validation.ValidationResult;
import com.training.atm.validation.ValidationRule;
import com.training.atm.dto.ErrorCode;

/** Rule: the destination account must exist in the system. */
public class DestinationExistsValidator implements ValidationRule<TransferContext> {
    @Override
    public ValidationResult validate(TransferContext ctx) {
        return ctx.destAccount() != null
                ? ValidationResult.valid()
                : ValidationResult.invalid(ErrorCode.ACCOUNT_NOT_FOUND,
                        "Destination account " + ctx.destAccountNumber() + " not found.");
    }
}
