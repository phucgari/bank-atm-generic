package com.training.atm.validation.withdrawal;

import com.training.atm.config.TransactionLimits;
import com.training.atm.util.FormatUtil;
import com.training.atm.validation.TransactionValidator;

import java.util.Optional;

/** Rule: cumulative daily withdrawals (including this one) must not exceed the daily limit. */
public class DailyWithdrawalLimitValidator implements TransactionValidator<WithdrawalContext> {
    @Override
    public Optional<String> validate(WithdrawalContext ctx) {
        return ctx.dailyTotal() + ctx.amount() <= TransactionLimits.MAX_WITHDRAWAL_DAILY
                ? Optional.empty()
                : Optional.of("Daily withdrawal limit of "
                        + FormatUtil.formatVND(TransactionLimits.MAX_WITHDRAWAL_DAILY)
                        + " would be exceeded.");
    }
}
