# Milestone 3 — Projection

## Goal

This is the pattern's core: the `PipelineRunProjector`, a pure function that folds an
ordered list of events into a `PipelineRunState`. Everything before this milestone was
plumbing (data types, a store to hold them); everything after this milestone is
plumbing too (a performance shortcut, and a demo). This is where "state is derived by
replaying events" stops being a sentence in the README and becomes code you write.

## Step 1 — the state model (copy-paste)

The shape of what a replay produces. This is mechanical — plain data describing a
result — so it's given in full.

`src/main/java/com/isaqb/practice/eventsourcing/state/RunStatus.java`:

```java
package com.isaqb.practice.eventsourcing.state;

public enum RunStatus {
  NOT_STARTED,
  RUNNING,
  SUCCEEDED,
  FAILED
}
```

`src/main/java/com/isaqb/practice/eventsourcing/state/StageStatus.java`:

```java
package com.isaqb.practice.eventsourcing.state;

public enum StageStatus {
  RUNNING,
  SUCCEEDED,
  FAILED
}
```

`src/main/java/com/isaqb/practice/eventsourcing/state/StageState.java`:

```java
package com.isaqb.practice.eventsourcing.state;

import java.time.Instant;

/** The derived state of one stage within a run, as of however many events have been
 * folded so far. {@code attempts} counts how many times the stage has been started -
 * 1 for a stage that has never been retried, 2+ for one that has. */
public record StageState(
    String name, StageStatus status, int attempts, Instant startedAt, Instant finishedAt) {}
```

`src/main/java/com/isaqb/practice/eventsourcing/state/PipelineRunState.java`:

```java
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
    List<StageState> stages) {

  public PipelineRunState {
    stages = List.copyOf(stages);
  }

  /** The state of a run before any event has been folded into it - "we know nothing
   * about this run yet." The starting point of every replay from event zero. */
  public static PipelineRunState unknown(String runId) {
    return new PipelineRunState(runId, RunStatus.NOT_STARTED, null, null, null, List.of());
  }
}
```

## Step 2 — the projector (write the fold yourself)

`src/main/java/com/isaqb/practice/eventsourcing/projection/PipelineRunProjector.java`:

```java
package com.isaqb.practice.eventsourcing.projection;

import com.isaqb.practice.eventsourcing.event.PipelineRunEvent;
import com.isaqb.practice.eventsourcing.state.PipelineRunState;
import java.util.List;

/**
 * The heart of this exercise: turns an ordered list of facts into the single derived
 * value that answers "what is the state of this run" - and, just as important, "what
 * was its state after only the first N events" (see Auditor, milestone 6).
 * PipelineRunState is never stored directly anywhere in this module; it is always
 * computed on demand by replaying events through this class.
 */
public final class PipelineRunProjector {

  private PipelineRunProjector() {}

  /**
   * Replays every event in {@code events}, in order, starting from
   * {@link PipelineRunState#unknown(String)}. Exactly
   * {@code replayFrom(PipelineRunState.unknown(runId), events)} - given here so you
   * don't have to spell that out at every call site.
   */
  public static PipelineRunState replay(String runId, List<PipelineRunEvent> events) {
    return replayFrom(PipelineRunState.unknown(runId), events);
  }

  /**
   * Folds {@code events}, in order, onto {@code initialState} and returns the result.
   * This is a pure function: it never mutates {@code initialState}, never mutates
   * {@code events}, performs no I/O, and calling it twice with the same arguments
   * always returns an equal result. That purity is exactly what makes it safe to unit
   * test with nothing but a hand-built {@code List<PipelineRunEvent>} - no EventStore
   * needed - and it's also what milestone 4's snapshot-assisted replay depends on:
   * folding onto a previously-saved state must behave identically to folding onto a
   * fresh one.
   *
   * TODO: implement the fold. Starting from {@code initialState}, apply each event in
   * {@code events}, in order:
   *
   *  - RunStarted: set pipelineName and startedAt from the event; status -> RUNNING.
   *  - StageStarted: if no StageState with this stageName exists yet, add a new one -
   *    attempts=1, status=RUNNING, startedAt=this event's time, finishedAt=null. If one
   *    already exists (this is a retry), replace it - attempts = old attempts + 1,
   *    status=RUNNING, startedAt=this event's time (the new attempt's start),
   *    finishedAt=null. Keep stages in the order they were *first* started, even across
   *    retries.
   *  - StageCompleted: replace the matching StageState with status=SUCCEEDED,
   *    finishedAt=this event's time (name/attempts/startedAt unchanged).
   *  - StageFailed: replace the matching StageState with status=FAILED,
   *    finishedAt=this event's time (name/attempts/startedAt unchanged).
   *  - RunFinished: status -> SUCCEEDED if event.success() else FAILED; finishedAt =
   *    this event's time.
   *
   *  A StageCompleted or StageFailed for a stage name with no prior StageStarted in
   *  {@code initialState} or {@code events} so far is malformed input for this
   *  exercise: throw IllegalStateException naming the runId and stage. The projector
   *  trusts the log is well-formed - a malformed log is a bug in whatever appended it,
   *  not something a reader should silently paper over.
   *
   * Hint: a LinkedHashMap<String, StageState> keyed by stage name preserves insertion
   * order for you, and re-`put`-ting an existing key updates its value without moving
   * its position - convenient for "keep stages in first-started order, but let later
   * events update them in place." Build the map from initialState.stages(), apply every
   * event to it, then take map.values() as your final stages list.
   */
  public static PipelineRunState replayFrom(
      PipelineRunState initialState, List<PipelineRunEvent> events) {
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

## Step 3 — tests (copy-paste, must pass once step 2 is done)

`src/test/java/com/isaqb/practice/eventsourcing/projection/PipelineRunProjectorTest.java`:

```java
package com.isaqb.practice.eventsourcing.projection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.isaqb.practice.eventsourcing.event.PipelineRunEvent;
import com.isaqb.practice.eventsourcing.event.RunFinished;
import com.isaqb.practice.eventsourcing.event.RunStarted;
import com.isaqb.practice.eventsourcing.event.StageCompleted;
import com.isaqb.practice.eventsourcing.event.StageFailed;
import com.isaqb.practice.eventsourcing.event.StageStarted;
import com.isaqb.practice.eventsourcing.state.PipelineRunState;
import com.isaqb.practice.eventsourcing.state.RunStatus;
import com.isaqb.practice.eventsourcing.state.StageStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PipelineRunProjectorTest {

  private static Instant t(int seconds) {
    return Instant.parse("2026-01-01T00:00:00Z").plusSeconds(seconds);
  }

  @Test
  void replayingAnEmptyListReturnsTheUnknownState() {
    PipelineRunState state = PipelineRunProjector.replay("run-1", List.of());

    assertEquals(PipelineRunState.unknown("run-1"), state);
  }

  @Test
  void runStartedSetsRunningStatusAndPipelineName() {
    List<PipelineRunEvent> events = List.of(new RunStarted("run-1", "nightly-build", t(0)));

    PipelineRunState state = PipelineRunProjector.replay("run-1", events);

    assertEquals(RunStatus.RUNNING, state.status());
    assertEquals("nightly-build", state.pipelineName());
  }

  @Test
  void aFullSuccessfulRunProducesSucceededStatusAndAllStagesSucceeded() {
    List<PipelineRunEvent> events =
        List.of(
            new RunStarted("run-1", "nightly-build", t(0)),
            new StageStarted("run-1", "compile", t(1)),
            new StageCompleted("run-1", "compile", t(2)),
            new StageStarted("run-1", "test", t(3)),
            new StageCompleted("run-1", "test", t(4)),
            new RunFinished("run-1", true, t(5)));

    PipelineRunState state = PipelineRunProjector.replay("run-1", events);

    assertEquals(RunStatus.SUCCEEDED, state.status());
    assertEquals(2, state.stages().size());
    assertEquals("compile", state.stages().get(0).name());
    assertEquals(StageStatus.SUCCEEDED, state.stages().get(0).status());
    assertEquals("test", state.stages().get(1).name());
    assertEquals(StageStatus.SUCCEEDED, state.stages().get(1).status());
  }

  @Test
  void aRetriedStageHasAttemptsGreaterThanOneAndKeepsItsOriginalPosition() {
    List<PipelineRunEvent> events =
        List.of(
            new RunStarted("run-1", "nightly-build", t(0)),
            new StageStarted("run-1", "compile", t(1)),
            new StageFailed("run-1", "compile", "flaky download", t(2)),
            new StageStarted("run-1", "compile", t(3)),
            new StageCompleted("run-1", "compile", t(4)),
            new StageStarted("run-1", "test", t(5)),
            new StageCompleted("run-1", "test", t(6)),
            new RunFinished("run-1", true, t(7)));

    PipelineRunState state = PipelineRunProjector.replay("run-1", events);

    var compile = state.stages().get(0);
    assertEquals("compile", compile.name());
    assertEquals(2, compile.attempts());
    assertEquals(StageStatus.SUCCEEDED, compile.status());
    assertEquals(RunStatus.SUCCEEDED, state.status());
  }

  @Test
  void aFailedStageThatIsNeverRetriedProducesAFailedRun() {
    List<PipelineRunEvent> events =
        List.of(
            new RunStarted("run-1", "nightly-build", t(0)),
            new StageStarted("run-1", "compile", t(1)),
            new StageFailed("run-1", "compile", "out of disk space", t(2)),
            new RunFinished("run-1", false, t(3)));

    PipelineRunState state = PipelineRunProjector.replay("run-1", events);

    assertEquals(RunStatus.FAILED, state.status());
    assertEquals(StageStatus.FAILED, state.stages().get(0).status());
  }

  @Test
  void replayFromContinuesFoldingOntoANonEmptyInitialState() {
    PipelineRunState afterTwoEvents =
        PipelineRunProjector.replay(
            "run-1",
            List.of(
                new RunStarted("run-1", "nightly-build", t(0)),
                new StageStarted("run-1", "compile", t(1))));

    PipelineRunState finalState =
        PipelineRunProjector.replayFrom(
            afterTwoEvents,
            List.of(
                new StageCompleted("run-1", "compile", t(2)),
                new RunFinished("run-1", true, t(3))));

    PipelineRunState fullReplay =
        PipelineRunProjector.replay(
            "run-1",
            List.of(
                new RunStarted("run-1", "nightly-build", t(0)),
                new StageStarted("run-1", "compile", t(1)),
                new StageCompleted("run-1", "compile", t(2)),
                new RunFinished("run-1", true, t(3))));

    assertEquals(fullReplay, finalState);
  }

  @Test
  void aStageCompletedWithNoPriorStageStartedIsRejected() {
    List<PipelineRunEvent> events =
        List.of(
            new RunStarted("run-1", "nightly-build", t(0)),
            new StageCompleted("run-1", "compile", t(1)));

    assertThrows(IllegalStateException.class, () -> PipelineRunProjector.replay("run-1", events));
  }
}
```

The `replayFromContinuesFoldingOntoANonEmptyInitialState` test is the one to pay
attention to: it's not testing "replay from scratch" again, it's proving `replayFrom`
is a genuine general-purpose fold, not a `replay` that happens to ignore its first
argument. Milestone 4 depends on that being true.

## Checkpoint

```bash
mvn -f patterns/08-event-sourcing/pom.xml clean verify
```

All seven `PipelineRunProjectorTest` cases pass. Take a moment to confirm: nothing
under `projection/` imports anything from `store/` — the fold works on a plain
`List<PipelineRunEvent>`, with no idea an `EventStore` even exists.

Next: [`04-snapshotting.md`](04-snapshotting.md).
