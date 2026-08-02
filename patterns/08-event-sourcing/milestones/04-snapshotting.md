# Milestone 4 — Snapshotting

## Goal

Address the trade-off named in the README: replaying grows more expensive as a run's
event log grows. For the handful of events this exercise's demo produces, that's
irrelevant — but imagine a run with a few thousand retried stages recorded over its
history; re-folding all of it on every single read is wasted work if most of that
history hasn't changed since the last time someone asked.

A **snapshot** is a cached checkpoint: a `PipelineRunState` plus "how many events it
accounts for." Given a snapshot and the run's full event list, a reader only needs to
fold the events *after* the snapshot's checkpoint, not the whole history from event
zero. This is purely a performance optimization — the study-doc callout is explicit
that a snapshot must never change the *answer*, only how much work it takes to compute
it. Milestone 3's `replayFrom` already does the general-purpose part of this work; this
milestone reuses it rather than duplicating it.

## Step 1 — the snapshot itself (copy-paste)

`src/main/java/com/isaqb/practice/eventsourcing/snapshot/Snapshot.java`:

```java
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
```

## Step 2 — the snapshot-assisted replay (write this yourself)

`src/main/java/com/isaqb/practice/eventsourcing/snapshot/SnapshotAssistedProjector.java`:

```java
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
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

## Step 3 — tests (copy-paste, must pass once step 2 is done)

`src/test/java/com/isaqb/practice/eventsourcing/snapshot/SnapshotAssistedProjectorTest.java`:

```java
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
```

## Checkpoint

```bash
mvn -f patterns/08-event-sourcing/pom.xml clean verify
```

All three `SnapshotAssistedProjectorTest` cases pass. You can point to the exact line
in `SnapshotAssistedProjector` where a longer event list with an *earlier* snapshot
checkpoint would mean strictly less work than a snapshot taken later — that's the
performance benefit the study-doc's "snapshotting" callout is pointing at.

Next: [`05-main-and-demo.md`](05-main-and-demo.md).
