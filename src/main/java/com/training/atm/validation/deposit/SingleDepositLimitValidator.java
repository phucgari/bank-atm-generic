package com.training.atm.validation.deposit;

import com.training.atm.config.TransactionLimits;
import com.training.atm.util.FormatUtil;
import com.training.atm.validation.TransactionValidator;

import java.util.Optional;

/** Rule: single deposit cannot exceed the per-transaction limit. */
public class SingleDepositLimitValidator implements TransactionValidator<DepositContext> {
    @Override
    public Optional<String> validate(DepositContext ctx) {
        return ctx.amount() <= TransactionLimits.MAX_DEPOSIT_SINGLE
                ? Optional.empty()
                : Optional.of("Single deposit limit is "
                        + FormatUtil.formatVND(TransactionLimits.MAX_DEPOSIT_SINGLE) + ".");
    }
}
