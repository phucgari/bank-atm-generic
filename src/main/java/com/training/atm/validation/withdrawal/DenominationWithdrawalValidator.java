package com.training.atm.validation.withdrawal;

import com.training.atm.config.TransactionLimits;
import com.training.atm.util.FormatUtil;
import com.training.atm.util.ValidationUtil;
import com.training.atm.validation.ValidationResult;
import com.training.atm.validation.ValidationRule;

/** Rule: amount must be a positive multiple of the withdrawal denomination. */
public class DenominationWithdrawalValidator implements ValidationRule<WithdrawalContext> {
    @Override
    public ValidationResult validate(WithdrawalContext ctx) {
        return ValidationUtil.isMultipleOf(ctx.amount(), TransactionLimits.WITHDRAWAL_DENOMINATION)
                ? ValidationResult.valid()
                : ValidationResult.invalid("Withdrawal amount must be a positive multiple of "
                        + FormatUtil.formatVND(TransactionLimits.WITHDRAWAL_DENOMINATION) + ".");
    }
}
