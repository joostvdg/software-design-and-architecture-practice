package com.isaqb.practice.broker.event;

/** Published once, after every stage has completed. */
public record RunFinished(String runId, boolean success) implements PipelineEvent {
}
