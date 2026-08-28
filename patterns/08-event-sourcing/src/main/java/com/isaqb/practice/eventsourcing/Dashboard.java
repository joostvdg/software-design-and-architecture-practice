package com.isaqb.practice.eventsourcing;

import com.isaqb.practice.eventsourcing.projection.PipelineRunProjector;
import com.isaqb.practice.eventsourcing.state.PipelineRunState;
import com.isaqb.practice.eventsourcing.state.StageState;
import com.isaqb.practice.eventsourcing.store.EventStore;

/**
 * Stands in for the case study's Dashboard: it wants "the current status of run X," but
 * there is no stored "current status" field anywhere in this module to read - it
 * computes that, on demand, by replaying the run's whole event log. If the process
 * restarted and lost every in-memory object it had ever built, the next call to
 * showCurrentState would produce exactly the same answer, purely from the log.
 */
public final class Dashboard {

    private Dashboard() {}

    public static void showCurrentState(EventStore store, String runId) {
        PipelineRunState state = PipelineRunProjector.replay(runId, store.eventsFor(runId));
        System.out.println(
                "  run " + runId + ": " + state.status() + " (" + state.pipelineName() + ")");
        for (StageState stage : state.stages()) {
            System.out.println(
                    "    - "
                            + stage.name()
                            + ": "
                            + stage.status()
                            + " (attempts="
                            + stage.attempts()
                            + ")");
        }
    }
}