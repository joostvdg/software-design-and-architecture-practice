package com.isaqb.practice.broker.event;

/** Published once, when the Pipeline Runner begins executing a run. */
public record RunStarted(String runId, String pipelineName) implements PipelineEvent{
}
