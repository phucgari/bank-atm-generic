package com.training.atm.validation.withdrawal;

import com.training.atm.config.TransactionLimits;
import com.training.atm.dto.ErrorCode;
import com.training.atm.util.FormatUtil;
import com.training.atm.validation.ValidationResult;
import com.training.atm.validation.ValidationRule;

/** Rule: cumulative daily withdrawals (including this one) must not exceed the daily limit. */
public class DailyWithdrawalLimitValidator implements ValidationRule<WithdrawalContext> {
    @Override
    public ValidationResult validate(WithdrawalContext ctx) {
        return ctx.dailyTotal() + ctx.amount() <= TransactionLimits.MAX_WITHDRAWAL_DAILY
                ? ValidationResult.valid()
                : ValidationResult.invalid(ErrorCode.DAILY_WITHDRAWAL_LIMIT_EXCEEDED,
                        FormatUtil.formatVND(TransactionLimits.MAX_WITHDRAWAL_DAILY));
    }
}
