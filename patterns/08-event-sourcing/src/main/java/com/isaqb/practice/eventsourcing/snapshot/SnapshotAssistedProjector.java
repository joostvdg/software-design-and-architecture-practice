package com.isaqb.practice.eventsourcing.snapshot;

import com.isaqb.practice.eventsourcing.event.PipelineRunEvent;
import com.isaqb.practice.eventsourcing.projection.PipelineRunProjector;
import com.isaqb.practice.eventsourcing.state.PipelineRunState;

import java.util.List;
import java.util.Optional;

/**
 * The snapshot-aware read path: given a run's *entire* event log and, optionally, a
 * previously-taken Snapshot of it, produces the same PipelineRunState that fully
 * replaying the whole log from scratch would - but does less work when a snapshot is
 * available, because it only folds the events the snapshot doesn't already account for.
 */
public final class SnapshotAssistedProjector {

    private SnapshotAssistedProjector() {}

    /**
     * TODO: implement.
     *  - If {@code snapshot} is empty, this is exactly
     *    {@code PipelineRunProjector.replay(runId, allEvents)} - fold everything, from
     *    scratch.
     *  - If {@code snapshot} is present, take only the events *after* the checkpoint -
     *    {@code allEvents.subList(snapshot.get().eventCount(), allEvents.size())} - and
     *    fold *those* onto {@code snapshot.get().state()} via
     *    {@code PipelineRunProjector.replayFrom}. Do not re-fold the events the snapshot
     *    already accounts for.
     *
     * The correctness requirement, whatever you write: for any valid snapshot
     * (0 <= eventCount <= allEvents.size()) taken from a prefix of this same event list,
     * the result here must equal {@code PipelineRunProjector.replay(runId, allEvents)}
     * exactly. A Snapshot is a performance shortcut, never a different answer - the test
     * below checks precisely that equivalence, for a snapshot taken partway through a
     * real run's history.
     */
    public static PipelineRunState replay(
            String runId, List<PipelineRunEvent> allEvents, Optional<Snapshot> snapshot) {

        if (snapshot.isEmpty()) {
            return PipelineRunProjector.replay(runId, allEvents);
        }

        var numberOfEventsInSnapshot = snapshot.get().eventCount();
        var subSetOfEventsToReplay = allEvents.subList(numberOfEventsInSnapshot, allEvents.size());
        return PipelineRunProjector.replayFrom(snapshot.get().state(), subSetOfEventsToReplay);

    }
}
