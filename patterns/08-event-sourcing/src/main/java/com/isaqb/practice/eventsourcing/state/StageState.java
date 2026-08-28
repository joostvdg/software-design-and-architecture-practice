package com.isaqb.practice.eventsourcing.state;

import java.time.Instant;

/** The derived state of one stage within a run, as of however many events have been
 * folded so far. {@code attempts} counts how many times the stage has been started -
 * 1 for a stage that has never been retried, 2+ for one that has. */
public record StageState(
        String name,
        StageStatus status,
        int attempts,
        Instant startedAt,
        Instant finishedAt
) {
}
