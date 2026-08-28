package com.isaqb.practice.eventsourcing.snapshot;

import com.isaqb.practice.eventsourcing.state.PipelineRunState;

/**
 * A cached checkpoint: the PipelineRunState after folding exactly the first
 * {@code eventCount} events of a run's log, plus that count itself. Replaying from
 * event zero every time works fine for a handful of events, but a Snapshot lets a
 * reader skip straight to "the state as of event N" and only fold whatever happened
 * *after* that - see SnapshotAssistedProjector.
 */
public record Snapshot(PipelineRunState state, int eventCount) {

    public Snapshot {
        if (eventCount < 0) {
            throw new IllegalArgumentException("eventCount must be >= 0, was " + eventCount);
        }
    }
}
