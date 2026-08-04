package com.isaqb.practice.layers.domain.rules;

import com.isaqb.practice.layers.domain.PipelineConfig;
import com.isaqb.practice.layers.domain.ValidationError;
import com.isaqb.practice.layers.domain.ValidationRule;

import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

public class StageNamesMustBeUnique implements ValidationRule {
    @Override
    public Optional<ValidationError> check(PipelineConfig config) {
        Set<String> stageNames = new TreeSet<>();
        stageNames.addAll(config.stages());
        if (stageNames.size() != config.stages().size()) {
            return Optional.of(new ValidationError("stages", "Incorrect number of stages"));
        }
        return Optional.empty();
    }
}
