package com.isaqb.practice.layers.domain.rules;

import com.isaqb.practice.layers.domain.PipelineConfig;
import com.isaqb.practice.layers.domain.ValidationError;
import com.isaqb.practice.layers.domain.ValidationRule;

import java.util.Optional;

public class MustHaveAtLeastOneStage implements ValidationRule {
    @Override
    public Optional<ValidationError> check(PipelineConfig config) {
        if (config.stages() == null || config.stages().isEmpty()) {
            return Optional.of(new ValidationError("stages", "No stages defined"));
        }
        return Optional.empty();
    }
}
