package com.training.atm.validation.withdrawal;

import com.training.atm.validation.ValidationResult;
import com.training.atm.validation.ValidationRule;

/** Rule: the account must have sufficient funds (respecting savings floor / overdraft limit). */
public class AccountBalanceValidator implements ValidationRule<WithdrawalContext> {
    @Override
    public ValidationResult validate(WithdrawalContext ctx) {
        return ctx.account().verifyWithdrawAmount(ctx.amount())
                ? ValidationResult.valid()
                : ValidationResult.invalid(ctx.account().getInsufficientFundsMessage(ctx.amount()));
    }
}
