package com.isaqb.practice.eventsourcing.state;


import java.time.Instant;
import java.util.List;

/**
 * The derived state of a pipeline run, as of however many events have been folded so
 * far. Never stored directly anywhere - always the return value of
 * PipelineRunProjector.replay(...) or .replayFrom(...). Two calls to replay() with the
 * same event list always produce an equal PipelineRunState (records get structural
 * equality for free), which is what makes the equivalence check in milestone 4 possible.
 */
public record PipelineRunState(
        String runId,
        RunStatus status,
        String pipelineName,
        Instant startedAt,
        Instant finishedAt,
        List<StageState> stages
) {

    public PipelineRunState {
        stages = List.copyOf(stages);
    }

    /** The state of a run before any event has been folded into it - "we know nothing
     * about this run yet." The starting point of every replay from event zero. */
    public static PipelineRunState unknown(String runId) {
        return new PipelineRunState(runId, RunStatus.NOT_STARTED, null, null, null, List.of());
    }
}
