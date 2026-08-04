package com.isaqb.practice.layers.domain;

import java.util.Optional;

/** One independent piece of validation logic. Returns an error iff the rule fails.  */
public interface ValidationRule {
    Optional<ValidationError> check(PipelineConfig config);
}
