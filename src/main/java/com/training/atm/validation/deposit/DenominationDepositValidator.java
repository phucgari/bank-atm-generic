package com.training.atm.validation.deposit;

import com.training.atm.config.TransactionLimits;
import com.training.atm.dto.ErrorCode;
import com.training.atm.util.FormatUtil;
import com.training.atm.util.ValidationUtil;
import com.training.atm.validation.ValidationResult;
import com.training.atm.validation.ValidationRule;

/** Rule: deposit amount must be a positive multiple of the deposit denomination. */
public class DenominationDepositValidator implements ValidationRule<DepositContext> {
    @Override
    public ValidationResult validate(DepositContext ctx) {
        return ValidationUtil.isMultipleOf(ctx.amount(), TransactionLimits.DEPOSIT_DENOMINATION)
                ? ValidationResult.valid()
                : ValidationResult.invalid(ErrorCode.INVALID_AMOUNT, "Deposit amount must be a positive multiple of "
                        + FormatUtil.formatVND(TransactionLimits.DEPOSIT_DENOMINATION) + ".");
    }
}
