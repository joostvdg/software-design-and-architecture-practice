package com.isaqb.practice.eventsourcing.event;

import java.time.Instant;

/**
 * Common supertype for every fact that can be recorded about a pipeline run. Sealed so
 * that any exhaustive switch over a PipelineRunEvent (see PipelineRunProjector,
 * milestone 3) is checked by the compiler - if a sixth event kind is ever added, every
 * such switch fails to compile until it's handled, instead of silently ignoring it at
 * runtime.
 */
public sealed interface PipelineRunEvent
    permits RunStarted, StageStarted, StageCompleted, StageFailed, RunFinished{

    /** The pipeline run this event belongs to. Every event kind carries this. */
    String runId();

    /** When this fact was recorded. Part of what makes the log an audit trail, not just
     * a sequence - "when" matters as much as "what" and "in what order". */
    Instant occurredAt();
}
