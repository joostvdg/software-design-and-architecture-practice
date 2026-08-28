package com.isaqb.practice.eventsourcing.event;

import java.time.Instant;

/** Published once, after every stage has reached a final outcome. */
public record RunFinished(String runId, boolean success, Instant occurredAt)
        implements PipelineRunEvent {}