package com.isaqb.practice.eventsourcing;

import com.isaqb.practice.eventsourcing.event.PipelineRunEvent;
import com.isaqb.practice.eventsourcing.projection.PipelineRunProjector;
import com.isaqb.practice.eventsourcing.snapshot.Snapshot;
import com.isaqb.practice.eventsourcing.snapshot.SnapshotAssistedProjector;
import com.isaqb.practice.eventsourcing.state.PipelineRunState;
import com.isaqb.practice.eventsourcing.store.EventStore;
import com.isaqb.practice.eventsourcing.store.InMemoryEventStore;
import java.util.List;
import java.util.Optional;

/**
 * Composition root and demo entry point: the only class in this module allowed to know
 * about every layer at once. Simulates a pipeline run (with one retried stage), then
 * shows all three case-study actors working purely off the append-only log: the
 * Auditor replaying the full history, the Dashboard computing current state on demand,
 * and a snapshot-assisted replay proving it agrees with a full replay while skipping
 * the events a snapshot already accounts for.
 */
public final class Main {

    private Main() {}

    public static void main(String[] args) {
        EventStore store = new InMemoryEventStore();
        String runId = "run-101";

        PipelineRunSimulator.run(store, runId);

        System.out.println("--- Auditor: state after each event ---");
        Auditor.printHistory(store, runId);

        System.out.println("--- Dashboard: current state (full replay) ---");
        Dashboard.showCurrentState(store, runId);

        System.out.println("--- Snapshot-assisted replay ---");
        List<PipelineRunEvent> allEvents = store.eventsFor(runId);
        int snapshotAt = 4;
        PipelineRunState snapshotState =
                PipelineRunProjector.replay(runId, allEvents.subList(0, snapshotAt));
        Snapshot snapshot = new Snapshot(snapshotState, snapshotAt);

        PipelineRunState viaSnapshot =
                SnapshotAssistedProjector.replay(runId, allEvents, Optional.of(snapshot));
        PipelineRunState viaFullReplay = PipelineRunProjector.replay(runId, allEvents);

        System.out.println(
                "  snapshot taken at event "
                        + snapshotAt
                        + " of "
                        + allEvents.size()
                        + "; snapshot-assisted result equals full replay: "
                        + viaSnapshot.equals(viaFullReplay));
    }
}