package com.isaqb.practice.layers.domain;

import java.util.List;

public record ValidationResult(List<ValidationError> errors) {
    public ValidationResult {
        errors = List.copyOf(errors);
    }

    public static ValidationResult valid() {
        return new ValidationResult(List.<ValidationError>of());
    }

    public boolean isValid() {
        return errors.isEmpty();
    }
}
