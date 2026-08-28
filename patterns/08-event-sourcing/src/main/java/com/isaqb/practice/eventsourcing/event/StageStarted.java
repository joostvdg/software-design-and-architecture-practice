package com.isaqb.practice.eventsourcing.event;

import java.time.Instant;

/**
 * Published every time a stage starts - including retries. A stage that fails and is
 * retried produces a second StageStarted with the same stageName; that's how a retry
 * is represented in this exercise, not as a separate event kind.
 */
public record StageStarted(String runId, String stageName, Instant occurredAt)
        implements PipelineRunEvent {}