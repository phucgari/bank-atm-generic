package com.training.atm.validation.transfer;

import com.training.atm.validation.ValidationResult;
import com.training.atm.validation.ValidationRule;

/** Rule: source account must have sufficient funds (respects savings floor / overdraft limit). */
public class BalanceTransferValidator implements ValidationRule<TransferContext> {
    @Override
    public ValidationResult validate(TransferContext ctx) {
        return ctx.source().verifyWithdrawAmount(ctx.amount())
                ? ValidationResult.valid()
                : ValidationResult.invalid(ctx.source().getInsufficientFundsMessage(ctx.amount()));
    }
}
