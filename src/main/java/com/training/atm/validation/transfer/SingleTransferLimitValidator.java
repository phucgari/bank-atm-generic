package com.training.atm.validation.transfer;

import com.training.atm.config.TransactionLimits;
import com.training.atm.util.FormatUtil;
import com.training.atm.validation.TransactionValidator;

import java.util.Optional;

/** Rule: single transfer cannot exceed the per-transaction limit. */
public class SingleTransferLimitValidator implements TransactionValidator<TransferContext> {
    @Override
    public Optional<String> validate(TransferContext ctx) {
        return ctx.amount() <= TransactionLimits.MAX_TRANSFER_SINGLE
                ? Optional.empty()
                : Optional.of("Single transfer limit is "
                        + FormatUtil.formatVND(TransactionLimits.MAX_TRANSFER_SINGLE) + ".");
    }
}
