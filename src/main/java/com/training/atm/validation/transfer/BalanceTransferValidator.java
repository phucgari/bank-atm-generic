package com.training.atm.validation.transfer;

import com.training.atm.validation.TransactionValidator;

import java.util.Optional;

/** Rule: source account must have sufficient funds (respects savings floor / overdraft limit). */
public class BalanceTransferValidator implements TransactionValidator<TransferContext> {
    @Override
    public Optional<String> validate(TransferContext ctx) {
        return ctx.source().verifyWithdrawAmount(ctx.amount())
                ? Optional.empty()
                : Optional.of(ctx.source().getInsufficientFundsMessage(ctx.amount()));
    }
}
