package com.isaqb.practice.microservices.orchestrator;

/**
 * A pipeline run as the Orchestrator knows it. Once COMPLETED, artifactName/
 * artifactVersion/artifactDigest describe what it produced - plain strings, not the
 * Registry's Artifact type, because the Orchestrator does not depend on the Registry's
 * internal types. It only knows the same wire-format fields the Registry does.
 */
public record PipelineRun(
        String id,
        RunStatus status,
        String artifactName,
        String artifactVersion,
        String artifactDigest) {

    /** A run that has just started, with no artifact produced yet. */
    public static PipelineRun running(String id) {
        return new PipelineRun(id, RunStatus.RUNNING, null, null, null);
    }
}
