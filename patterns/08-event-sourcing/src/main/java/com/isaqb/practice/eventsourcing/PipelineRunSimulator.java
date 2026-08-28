package com.isaqb.practice.eventsourcing;

import com.isaqb.practice.eventsourcing.event.RunFinished;
import com.isaqb.practice.eventsourcing.event.RunStarted;
import com.isaqb.practice.eventsourcing.event.StageCompleted;
import com.isaqb.practice.eventsourcing.event.StageFailed;
import com.isaqb.practice.eventsourcing.event.StageStarted;
import com.isaqb.practice.eventsourcing.store.EventStore;
import java.time.Instant;

/**
 * Stands in for the case study's Pipeline Runner: appends one event per lifecycle step
 * to the given EventStore, including one deliberate stage retry (compile fails once,
 * then succeeds) so the Auditor and Dashboard have something interesting to show. This
 * class only ever calls EventStore.append - it never computes or holds a
 * PipelineRunState itself. That's the whole point: the writer's job is to record facts,
 * not to know or care what "current state" means to any particular reader.
 */
public final class PipelineRunSimulator {

    private PipelineRunSimulator() {}

    public static void run(EventStore store, String runId) {
        Instant t = Instant.parse("2026-08-01T09:00:00Z");

        store.append(new RunStarted(runId, "nightly-build", t));

        t = t.plusSeconds(1);
        store.append(new StageStarted(runId, "compile", t));
        t = t.plusSeconds(30);
        store.append(new StageFailed(runId, "compile", "flaky dependency download", t));

        t = t.plusSeconds(5);
        store.append(new StageStarted(runId, "compile", t)); // retry
        t = t.plusSeconds(20);
        store.append(new StageCompleted(runId, "compile", t));

        t = t.plusSeconds(1);
        store.append(new StageStarted(runId, "test", t));
        t = t.plusSeconds(45);
        store.append(new StageCompleted(runId, "test", t));

        t = t.plusSeconds(1);
        store.append(new StageStarted(runId, "package", t));
        t = t.plusSeconds(15);
        store.append(new StageCompleted(runId, "package", t));

        t = t.plusSeconds(1);
        store.append(new RunFinished(runId, true, t));
    }
}