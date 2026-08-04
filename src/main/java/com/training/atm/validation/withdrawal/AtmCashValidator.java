package com.training.atm.validation.withdrawal;

import com.training.atm.validation.TransactionValidator;

import java.util.Optional;

/** Rule: the ATM must have enough physical cash to cover the withdrawal. */
public class AtmCashValidator implements TransactionValidator<WithdrawalContext> {
    @Override
    public Optional<String> validate(WithdrawalContext ctx) {
        return ctx.atmAvailableCash() >= ctx.amount()
                ? Optional.empty()
                : Optional.of("ATM does not have sufficient cash for this transaction.");
    }
}
