package com.training.atm.validation.withdrawal;

import com.training.atm.config.TransactionLimits;
import com.training.atm.util.FormatUtil;
import com.training.atm.util.ValidationUtil;
import com.training.atm.validation.TransactionValidator;

import java.util.Optional;

/** Rule: amount must be a positive multiple of the withdrawal denomination. */
public class DenominationWithdrawalValidator implements TransactionValidator<WithdrawalContext> {
    @Override
    public Optional<String> validate(WithdrawalContext ctx) {
        return ValidationUtil.isMultipleOf(ctx.amount(), TransactionLimits.WITHDRAWAL_DENOMINATION)
                ? Optional.empty()
                : Optional.of("Withdrawal amount must be a positive multiple of "
                        + FormatUtil.formatVND(TransactionLimits.WITHDRAWAL_DENOMINATION) + ".");
    }
}
