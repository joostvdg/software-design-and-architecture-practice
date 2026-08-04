package com.isaqb.practice.layers.domain.rules;

import com.isaqb.practice.layers.domain.PipelineConfig;
import com.isaqb.practice.layers.domain.ValidationError;
import com.isaqb.practice.layers.domain.ValidationRule;

import java.util.Optional;

public class NameMustNotBeBlank implements ValidationRule {
    @Override
    public Optional<ValidationError> check(PipelineConfig config) {
        if (config.name() == null || config.name().trim().isEmpty()) {
            return Optional.of(new ValidationError("name", "Name must not be empty"));
        }
        return Optional.empty();
    }
}
