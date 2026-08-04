package com.training.atm.validation.withdrawal;

import com.training.atm.config.TransactionLimits;
import com.training.atm.util.FormatUtil;
import com.training.atm.validation.TransactionValidator;

import java.util.Optional;

/** Rule: single withdrawal cannot exceed the per-transaction limit. */
public class SingleWithdrawalLimitValidator implements TransactionValidator<WithdrawalContext> {
    @Override
    public Optional<String> validate(WithdrawalContext ctx) {
        return ctx.amount() <= TransactionLimits.MAX_WITHDRAWAL_SINGLE
                ? Optional.empty()
                : Optional.of("Single withdrawal limit is "
                        + FormatUtil.formatVND(TransactionLimits.MAX_WITHDRAWAL_SINGLE) + ".");
    }
}
