package com.training.atm.validation.transfer;

import com.training.atm.config.TransactionLimits;
import com.training.atm.util.FormatUtil;
import com.training.atm.validation.ValidationResult;
import com.training.atm.validation.ValidationRule;

/** Rule: cumulative daily outbound transfers must not exceed the daily limit. */
public class DailyTransferLimitValidator implements ValidationRule<TransferContext> {
    @Override
    public ValidationResult validate(TransferContext ctx) {
        return ctx.dailyTotal() + ctx.amount() <= TransactionLimits.MAX_TRANSFER_DAILY
                ? ValidationResult.valid()
                : ValidationResult.invalid("Daily transfer limit of "
                        + FormatUtil.formatVND(TransactionLimits.MAX_TRANSFER_DAILY)
                        + " would be exceeded.");
    }
}
