package com.training.atm.validation.transfer;

import com.training.atm.validation.TransactionValidator;

import java.util.Optional;

/** Rule: the destination account must exist in the system. */
public class DestinationExistsValidator implements TransactionValidator<TransferContext> {
    @Override
    public Optional<String> validate(TransferContext ctx) {
        return ctx.destAccount() != null
                ? Optional.empty()
                : Optional.of("Destination account " + ctx.destAccountNumber() + " not found.");
    }
}
