package com.training.atm.validation.transfer;

import com.training.atm.validation.ValidationResult;
import com.training.atm.validation.ValidationRule;
import com.training.atm.dto.ErrorCode;

/** Rule: source account must have sufficient funds (respects savings floor / overdraft limit). */
public class BalanceTransferValidator implements ValidationRule<TransferContext> {
    @Override
    public ValidationResult validate(TransferContext ctx) {
        return ctx.source().verifyWithdrawAmount(ctx.amount())
                ? ValidationResult.valid()
                : ValidationResult.invalid(ErrorCode.INSUFFICIENT_FUNDS,
                        ctx.source().getInsufficientFundsMessage(ctx.amount()));
    }
}
