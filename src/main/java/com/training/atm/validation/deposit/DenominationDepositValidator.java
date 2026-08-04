package com.training.atm.validation.deposit;

import com.training.atm.config.TransactionLimits;
import com.training.atm.util.FormatUtil;
import com.training.atm.util.ValidationUtil;
import com.training.atm.validation.TransactionValidator;

import java.util.Optional;

/** Rule: deposit amount must be a positive multiple of the deposit denomination. */
public class DenominationDepositValidator implements TransactionValidator<DepositContext> {
    @Override
    public Optional<String> validate(DepositContext ctx) {
        return ValidationUtil.isMultipleOf(ctx.amount(), TransactionLimits.DEPOSIT_DENOMINATION)
                ? Optional.empty()
                : Optional.of("Deposit amount must be a positive multiple of "
                        + FormatUtil.formatVND(TransactionLimits.DEPOSIT_DENOMINATION) + ".");
    }
}
