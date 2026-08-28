package com.isaqb.practice.eventsourcing.event;

import java.time.Instant;

/** Published once a stage finishes unsuccessfully. May be followed by another
 * StageStarted for the same stageName if the stage is retried. */
public record StageFailed(String runId, String stageName, String reason, Instant occurredAt)
        implements PipelineRunEvent {}