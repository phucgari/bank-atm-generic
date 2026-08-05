package com.training.atm.command;

import com.training.atm.dto.ServiceResult;

/**
 * Command interface for ATM transaction operations.
 *
 * Encapsulating each operation as a first-class object gives us:
 * <ul>
 *   <li><b>Uniform execution</b> — the scheduler calls {@code execute()} on any
 *       command without knowing its type.</li>
 *   <li><b>Self-describing operations</b> — {@code describe()} produces a
 *       human-readable summary used in audit logs and receipts without the
 *       caller having to construct the string itself.</li>
 *   <li><b>Extensibility</b> — queuing, retry, or undo can be added to this
 *       interface in the future without changing callers.</li>
 * </ul>
 *
 * @param <R> the specific {@link OperationResult} type returned by this command
 */
public interface TransactionCommand<R extends OperationResult> {
    /** Executes the operation and returns its result. */
    R execute();

    /** Short human-readable type label (e.g. {@code "WITHDRAWAL"}). Used on receipts. */
    String getType();

    /** Full operation description (e.g. {@code "WITHDRAW 1,000,000 VND from ACC001"}). Used in logs. */
    String describe();
}
