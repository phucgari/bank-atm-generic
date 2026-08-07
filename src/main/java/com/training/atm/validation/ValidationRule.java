package com.training.atm.validation;

@FunctionalInterface
public interface ValidationRule<T> {
    ValidationResult validate(T entity);
}
