package com.isaqb.practice.plugin;

import java.util.List;

/** The outcome of running a whole pipeline: one result per step, in order. */
public record PipelineRunResult(List<StepResult> stepResults) {

    public PipelineRunResult{
        stepResults = List.copyOf(stepResults);
    }

    public boolean allSucceeded() {
        return stepResults.stream().allMatch(StepResult::success);
    }
}
