package com.training.atm.validation.deposit;

import com.training.atm.config.TransactionLimits;
import com.training.atm.dto.ErrorCode;
import com.training.atm.util.FormatUtil;
import com.training.atm.validation.ValidationResult;
import com.training.atm.validation.ValidationRule;

/** Rule: single deposit cannot exceed the per-transaction limit. */
public class SingleDepositLimitValidator implements ValidationRule<DepositContext> {
    @Override
    public ValidationResult validate(DepositContext ctx) {
        return ctx.amount() <= TransactionLimits.MAX_DEPOSIT_SINGLE
                ? ValidationResult.valid()
                : ValidationResult.invalid(ErrorCode.LIMIT_EXCEEDED, "Single deposit limit is "
                        + FormatUtil.formatVND(TransactionLimits.MAX_DEPOSIT_SINGLE) + ".");
    }
}
