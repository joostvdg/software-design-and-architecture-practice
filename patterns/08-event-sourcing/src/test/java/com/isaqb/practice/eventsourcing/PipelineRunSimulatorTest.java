package com.isaqb.practice.eventsourcing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.isaqb.practice.eventsourcing.event.PipelineRunEvent;
import com.isaqb.practice.eventsourcing.projection.PipelineRunProjector;
import com.isaqb.practice.eventsourcing.snapshot.Snapshot;
import com.isaqb.practice.eventsourcing.snapshot.SnapshotAssistedProjector;
import com.isaqb.practice.eventsourcing.state.PipelineRunState;
import com.isaqb.practice.eventsourcing.state.RunStatus;
import com.isaqb.practice.eventsourcing.state.StageStatus;
import com.isaqb.practice.eventsourcing.store.EventStore;
import com.isaqb.practice.eventsourcing.store.InMemoryEventStore;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PipelineRunSimulatorTest {

    @Test
    void theSimulatedRunEndsUpSucceededWithARetriedCompileStage() {
        EventStore store = new InMemoryEventStore();
        PipelineRunSimulator.run(store, "run-1");

        PipelineRunState state = PipelineRunProjector.replay("run-1", store.eventsFor("run-1"));

        assertEquals(RunStatus.SUCCEEDED, state.status());
        assertEquals(3, state.stages().size());
        var compile = state.stages().get(0);
        assertEquals("compile", compile.name());
        assertEquals(2, compile.attempts());
        assertEquals(StageStatus.SUCCEEDED, compile.status());
    }

    @Test
    void aSnapshotTakenPartwayThroughTheSimulatedRunStillMatchesAFullReplay() {
        EventStore store = new InMemoryEventStore();
        PipelineRunSimulator.run(store, "run-1");
        List<PipelineRunEvent> allEvents = store.eventsFor("run-1");

        int snapshotAt = 4;
        PipelineRunState snapshotState =
                PipelineRunProjector.replay("run-1", allEvents.subList(0, snapshotAt));
        Snapshot snapshot = new Snapshot(snapshotState, snapshotAt);

        PipelineRunState viaSnapshot =
                SnapshotAssistedProjector.replay("run-1", allEvents, Optional.of(snapshot));
        PipelineRunState viaFullReplay = PipelineRunProjector.replay("run-1", allEvents);

        assertEquals(viaFullReplay, viaSnapshot);
    }

    @Test
    void auditorCanSeeAnIntermediateStateThatDashboardNoLongerCan() {
        EventStore store = new InMemoryEventStore();
        PipelineRunSimulator.run(store, "run-1");
        List<PipelineRunEvent> allEvents = store.eventsFor("run-1");

        // After the first StageFailed (event 3), the run was still RUNNING - a fact only
        // the Auditor's replay-to-event-N view can recover; the Dashboard's full replay
        // only ever reports the final SUCCEEDED outcome.
        PipelineRunState asOfEvent3 = PipelineRunProjector.replay("run-1", allEvents.subList(0, 3));
        PipelineRunState finalState = PipelineRunProjector.replay("run-1", allEvents);

        assertEquals(RunStatus.RUNNING, asOfEvent3.status());
        assertEquals(RunStatus.SUCCEEDED, finalState.status());
        assertTrue(asOfEvent3.stages().get(0).status() == StageStatus.FAILED);
    }
}