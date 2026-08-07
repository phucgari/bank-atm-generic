package com.training.atm.validation.transfer;

import com.training.atm.config.TransactionLimits;
import com.training.atm.dto.ErrorCode;
import com.training.atm.util.FormatUtil;
import com.training.atm.validation.ValidationResult;
import com.training.atm.validation.ValidationRule;

/** Rule: single transfer cannot exceed the per-transaction limit. */
public class SingleTransferLimitValidator implements ValidationRule<TransferContext> {
    @Override
    public ValidationResult validate(TransferContext ctx) {
        return ctx.amount() <= TransactionLimits.MAX_TRANSFER_SINGLE
                ? ValidationResult.valid()
                : ValidationResult.invalid(ErrorCode.SINGLE_TRANSFER_LIMIT_EXCEEDED,
                        FormatUtil.formatVND(TransactionLimits.MAX_TRANSFER_SINGLE));
    }
}
