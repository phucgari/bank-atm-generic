package com.training.atm.validation.withdrawal;

import com.training.atm.config.TransactionLimits;
import com.training.atm.dto.ErrorCode;
import com.training.atm.util.FormatUtil;
import com.training.atm.validation.ValidationResult;
import com.training.atm.validation.ValidationRule;

/** Rule: single withdrawal cannot exceed the per-transaction limit. */
public class SingleWithdrawalLimitValidator implements ValidationRule<WithdrawalContext> {
    @Override
    public ValidationResult validate(WithdrawalContext ctx) {
        return ctx.amount() <= TransactionLimits.MAX_WITHDRAWAL_SINGLE
                ? ValidationResult.valid()
                : ValidationResult.invalid(ErrorCode.SINGLE_WITHDRAWAL_LIMIT_EXCEEDED,
                        FormatUtil.formatVND(TransactionLimits.MAX_WITHDRAWAL_SINGLE));
    }
}
