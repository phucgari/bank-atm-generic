package com.training.atm.validation.transfer;

import com.training.atm.validation.TransactionValidator;

import java.util.Optional;

/** Rule: source and destination accounts must be different. */
public class SameAccountTransferValidator implements TransactionValidator<TransferContext> {
    @Override
    public Optional<String> validate(TransferContext ctx) {
        return ctx.source().getAccountNumber().equals(ctx.destAccountNumber())
                ? Optional.of("Source and destination accounts must be different.")
                : Optional.empty();
    }
}
