package com.training.atm.validation;

import java.util.Optional;

/**
 * Chain-of-Responsibility interface for transaction validation.
 *
 * Each handler validates one rule and returns either:
 * <ul>
 *   <li>{@link Optional#empty()} — rule passed, pass to the next handler.</li>
 *   <li>{@code Optional.of(errorMessage)} — rule failed, stop the chain.</li>
 * </ul>
 *
 * The {@code then()} default method composes two validators into a single
 * handler, allowing chains to be assembled declaratively at construction time:
 *
 * <pre>{@code
 * TransactionValidator<WithdrawalContext> chain =
 *     new DenominationWithdrawalValidator()
 *     .then(new SingleWithdrawalLimitValidator())
 *     .then(new DailyWithdrawalLimitValidator())
 *     .then(new AccountBalanceValidator())
 *     .then(new AtmCashValidator());
 * }</pre>
 *
 * Adding a new rule requires only a new class and appending it to the chain —
 * no existing validator or service code changes.
 *
 * @param <T> the context object that carries all data a handler might need
 */
@FunctionalInterface
public interface TransactionValidator<T> {
    Optional<String> validate(T context);

    default TransactionValidator<T> then(TransactionValidator<T> next) {
        return ctx -> {
            Optional<String> result = this.validate(ctx);
            return result.isPresent() ? result : next.validate(ctx);
        };
    }
}
