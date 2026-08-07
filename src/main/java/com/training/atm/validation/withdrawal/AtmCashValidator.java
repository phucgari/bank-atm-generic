package com.training.atm.validation.withdrawal;

import com.training.atm.validation.ValidationResult;
import com.training.atm.validation.ValidationRule;

/** Rule: the ATM must have enough physical cash to cover the withdrawal. */
public class AtmCashValidator implements ValidationRule<WithdrawalContext> {
    @Override
    public ValidationResult validate(WithdrawalContext ctx) {
        return ctx.atmAvailableCash() >= ctx.amount()
                ? ValidationResult.valid()
                : ValidationResult.invalid("ATM does not have sufficient cash for this transaction.");
    }
}
