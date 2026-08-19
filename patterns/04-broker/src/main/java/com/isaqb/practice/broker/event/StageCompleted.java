package com.isaqb.practice.broker.event;

/** Published once per stage, after that stage finishes (successfully or not). */
public record StageCompleted(String runId, String stageName, boolean success) implements PipelineEvent {
}
