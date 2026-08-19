package com.isaqb.practice.broker.event;

/**
 * Common supertype for everything the Pipeline Runner can publish. Sealed so that
 * every switch over a PipelineEvent (see AuditLogger in milestone 3) can be exhaustive
 * without a default branch - the compiler tells you if a new event kind is added
 * somewhere that doesn't yet handle it.
 */
public sealed interface PipelineEvent permits RunStarted, StageCompleted, RunFinished {

    /** The pipeline run this event belongs to. Every event kind carries this. */
    String runId();
}
