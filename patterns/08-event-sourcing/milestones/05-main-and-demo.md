# Milestone 5 — Main and demo

## Goal

Write the three case-study actors on top of the pattern — `PipelineRunSimulator`
(stands in for the Pipeline Runner), `Auditor`, and `Dashboard` — plus `Main`, the
composition root that wires them together and runs a demo end to end. Everything in
this milestone is copy-paste: there's no new pattern logic here, only wiring on top of
milestones 1-4, and wiring is exactly what a composition root and a demo are for.

## Step 1 — the Pipeline Runner (copy-paste)

`src/main/java/com/isaqb/practice/eventsourcing/PipelineRunSimulator.java`:

```java
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
```

## Step 2 — the Auditor (copy-paste)

`src/main/java/com/isaqb/practice/eventsourcing/Auditor.java`:

```java
package com.isaqb.practice.eventsourcing;

import com.isaqb.practice.eventsourcing.event.PipelineRunEvent;
import com.isaqb.practice.eventsourcing.projection.PipelineRunProjector;
import com.isaqb.practice.eventsourcing.state.PipelineRunState;
import com.isaqb.practice.eventsourcing.store.EventStore;
import java.util.List;

/**
 * Stands in for the case study's Auditor: for compliance review, it doesn't just want
 * "what is the state now" - it wants to see how the state evolved, fact by fact. This
 * is only possible because every fact is still in the log; a design that only kept a
 * mutable "current status" row could never answer "what did we know after event 3,"
 * only "what do we know now."
 */
public final class Auditor {

  private Auditor() {}

  public static void printHistory(EventStore store, String runId) {
    List<PipelineRunEvent> events = store.eventsFor(runId);
    for (int n = 1; n <= events.size(); n++) {
      PipelineRunState asOfN = PipelineRunProjector.replay(runId, events.subList(0, n));
      System.out.printf(
          "  after event %d (%s): status=%s%n",
          n, events.get(n - 1).getClass().getSimpleName(), asOfN.status());
    }
  }
}
```

## Step 3 — the Dashboard (copy-paste)

`src/main/java/com/isaqb/practice/eventsourcing/Dashboard.java`:

```java
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
```

## Step 4 — Main (copy-paste)

`src/main/java/com/isaqb/practice/eventsourcing/Main.java`:

```java
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
```

## Step 5 — end-to-end test (copy-paste)

`src/test/java/com/isaqb/practice/eventsourcing/PipelineRunSimulatorTest.java`:

```java
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
```

## Step 6 — try it for real

```bash
mvn -f patterns/08-event-sourcing/pom.xml clean package
java -jar patterns/08-event-sourcing/target/event-sourcing-1.0.0-SNAPSHOT.jar
```

You should see the Auditor's per-event status progression (including one `RUNNING`
after the first failed compile attempt, before it recovers), the Dashboard's final
`SUCCEEDED` state with all three stages, and a line confirming the snapshot-assisted
replay agreed with the full replay.

## Checkpoint

- [ ] `mvn -f patterns/08-event-sourcing/pom.xml clean verify` passes, every test green.
- [ ] Running the jar prints all three sections as described in step 6.
- [ ] You can answer, out loud, the question milestone 0 asked: if the process crashed
      and restarted right now, what would `Dashboard.showCurrentState` print next time,
      and why would it be unaffected by the crash?

Next: [`06-build-and-release.md`](06-build-and-release.md).
