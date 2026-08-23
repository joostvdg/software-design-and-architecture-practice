package com.isaqb.practice.cqrs.query;

import java.time.Duration;

/**
 * The read side of CQRS: a shape built for the Dashboard, not for mutation. Nothing
 * about this record can change a pipeline run - it's a snapshot, rebuilt from the
 * write side by PipelineRunQueryService.updateProjection after every command.
 */
public record PipelineRunSummary(
        String runId,
        String status,
        int stagesCompleted,
        int stagesTotal,
        String currentStage,
        Duration duration
) {
}
