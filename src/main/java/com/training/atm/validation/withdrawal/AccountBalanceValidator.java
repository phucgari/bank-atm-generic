package com.training.atm.validation.withdrawal;

import com.training.atm.validation.TransactionValidator;

import java.util.Optional;

/** Rule: the account must have sufficient funds (respecting savings floor / overdraft limit). */
public class AccountBalanceValidator implements TransactionValidator<WithdrawalContext> {
    @Override
    public Optional<String> validate(WithdrawalContext ctx) {
        return ctx.account().verifyWithdrawAmount(ctx.amount())
                ? Optional.empty()
                : Optional.of(ctx.account().getInsufficientFundsMessage(ctx.amount()));
    }
}
