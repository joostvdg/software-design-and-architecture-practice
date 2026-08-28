package com.isaqb.practice.eventsourcing.event;

import java.time.Instant;

/** Published once a stage finishes successfully. */
public record StageCompleted(String runId, String stageName, Instant occurredAt)
        implements PipelineRunEvent {}