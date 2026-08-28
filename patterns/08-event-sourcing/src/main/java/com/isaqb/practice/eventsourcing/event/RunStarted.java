package com.isaqb.practice.eventsourcing.event;

import java.time.Instant;

/** Published once, when a pipeline run begins. */
public record RunStarted(String runId, String pipelineName, Instant occurredAt)
        implements PipelineRunEvent {}