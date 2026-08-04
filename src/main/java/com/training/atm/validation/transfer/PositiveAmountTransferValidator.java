package com.training.atm.validation.transfer;

import com.training.atm.validation.TransactionValidator;

import java.util.Optional;

/** Rule: transfer amount must be positive. */
public class PositiveAmountTransferValidator implements TransactionValidator<TransferContext> {
    @Override
    public Optional<String> validate(TransferContext ctx) {
        return ctx.amount() > 0
                ? Optional.empty()
                : Optional.of("Transfer amount must be positive.");
    }
}
