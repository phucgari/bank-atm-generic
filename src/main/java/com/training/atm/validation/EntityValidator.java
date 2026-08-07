package com.training.atm.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EntityValidator<T> {
    private final List<ValidationRule<T>> rules = new ArrayList<>();

    public EntityValidator<T> addRule(ValidationRule<T> rule) {
        rules.add(rule);
        return this;
    }

    public List<ValidationResult> validate(T entity) {
        return rules.stream()
                .map(rule -> rule.validate(entity))
                .filter(result -> !result.isValid())
                .collect(Collectors.toList());
    }
}
