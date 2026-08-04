package com.training.atm.validation.transfer;

import com.training.atm.config.TransactionLimits;
import com.training.atm.util.FormatUtil;
import com.training.atm.validation.TransactionValidator;

import java.util.Optional;

/** Rule: cumulative daily outbound transfers must not exceed the daily limit. */
public class DailyTransferLimitValidator implements TransactionValidator<TransferContext> {
    @Override
    public Optional<String> validate(TransferContext ctx) {
        return ctx.dailyTotal() + ctx.amount() <= TransactionLimits.MAX_TRANSFER_DAILY
                ? Optional.empty()
                : Optional.of("Daily transfer limit of "
                        + FormatUtil.formatVND(TransactionLimits.MAX_TRANSFER_DAILY)
                        + " would be exceeded.");
    }
}
