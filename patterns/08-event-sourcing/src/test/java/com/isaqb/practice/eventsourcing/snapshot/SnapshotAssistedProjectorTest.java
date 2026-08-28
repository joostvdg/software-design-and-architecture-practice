package com.isaqb.practice.eventsourcing.snapshot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.isaqb.practice.eventsourcing.event.PipelineRunEvent;
import com.isaqb.practice.eventsourcing.event.RunFinished;
import com.isaqb.practice.eventsourcing.event.RunStarted;
import com.isaqb.practice.eventsourcing.event.StageCompleted;
import com.isaqb.practice.eventsourcing.event.StageStarted;
import com.isaqb.practice.eventsourcing.projection.PipelineRunProjector;
import com.isaqb.practice.eventsourcing.state.PipelineRunState;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SnapshotAssistedProjectorTest {

    private static Instant t(int seconds) {
        return Instant.parse("2026-01-01T00:00:00Z").plusSeconds(seconds);
    }

    private static final List<PipelineRunEvent> RUN_EVENTS =
            List.of(
                    new RunStarted("run-1", "nightly-build", t(0)),
                    new StageStarted("run-1", "compile", t(1)),
                    new StageCompleted("run-1", "compile", t(2)),
                    new StageStarted("run-1", "test", t(3)),
                    new StageCompleted("run-1", "test", t(4)),
                    new StageStarted("run-1", "package", t(5)),
                    new StageCompleted("run-1", "package", t(6)),
                    new RunFinished("run-1", true, t(7)));

    @Test
    void withNoSnapshotBehavesLikeAFullReplay() {
        PipelineRunState viaFullReplay = PipelineRunProjector.replay("run-1", RUN_EVENTS);

        PipelineRunState viaEmptySnapshot =
                SnapshotAssistedProjector.replay("run-1", RUN_EVENTS, Optional.empty());

        assertEquals(viaFullReplay, viaEmptySnapshot);
    }

    @Test
    void withASnapshotTakenPartwayThroughMatchesAFullReplay() {
        int snapshotAt = 3;
        PipelineRunState stateAtCheckpoint =
                PipelineRunProjector.replay("run-1", RUN_EVENTS.subList(0, snapshotAt));
        Snapshot snapshot = new Snapshot(stateAtCheckpoint, snapshotAt);

        PipelineRunState viaSnapshot =
                SnapshotAssistedProjector.replay("run-1", RUN_EVENTS, Optional.of(snapshot));
        PipelineRunState viaFullReplay = PipelineRunProjector.replay("run-1", RUN_EVENTS);

        assertEquals(viaFullReplay, viaSnapshot);
    }

    @Test
    void aSnapshotAlreadyCaughtUpToTheEndStillMatchesAFullReplay() {
        PipelineRunState stateAtEnd = PipelineRunProjector.replay("run-1", RUN_EVENTS);
        Snapshot snapshot = new Snapshot(stateAtEnd, RUN_EVENTS.size());

        PipelineRunState viaSnapshot =
                SnapshotAssistedProjector.replay("run-1", RUN_EVENTS, Optional.of(snapshot));

        assertEquals(stateAtEnd, viaSnapshot);
    }
}