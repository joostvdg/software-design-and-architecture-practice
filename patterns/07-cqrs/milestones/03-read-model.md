# Milestone 3 — Read model

## Goal

Build the query side: `PipelineRunSummary`, the dashboard-shaped read model, and
`PipelineRunQueryService`, which serves it from its own projection store. The
interesting part of this milestone is `updateProjection`: the method that turns a
`PipelineRun` (write-side shape) into a `PipelineRunSummary` (read-side shape). That
mapping — not a database, not a network call — *is* the "read model" in this exercise.
Production systems might back the projection store with a real database or search
index; the mapping logic you're about to write is the same either way.

Notice `PipelineRunQueryService` never mutates a `PipelineRun` and never calls
`PipelineRunCommandService` — it only ever *reads* the public surface of a `PipelineRun`
it's handed. Who calls `updateProjection`, and when, is deliberately not this class's
concern; that's wired up in milestone 4.

## Step 1 — the read-side DTO (copy-paste)

`src/main/java/com/isaqb/practice/cqrs/query/PipelineRunSummary.java`:

```java
package com.isaqb.practice.cqrs.query;

import java.time.Duration;

/**
 * The read side of CQRS: a shape built for the Dashboard, not for mutation. Nothing
 * about this record can change a pipeline run - it's a snapshot, rebuilt from the
 * write side by PipelineRunQueryService.updateProjection after every command.
 */
public record PipelineRunSummary(
    String runId,
    String status,
    int stagesCompleted,
    int stagesTotal,
    String currentStage,
    Duration duration) {}
```

`currentStage` is `null` when there's no pending stage (either the run just started
with zero stages completed and there IS a pending one, or every stage is done) — see
the TODO below for the exact rule.

## Step 2 — the query service (storage and lookups given, projection logic left to you)

`src/main/java/com/isaqb/practice/cqrs/query/PipelineRunQueryService.java`:

```java
package com.isaqb.practice.cqrs.query;

import com.isaqb.practice.cqrs.command.PipelineRun;
import com.isaqb.practice.cqrs.command.PipelineStage;
import com.isaqb.practice.cqrs.command.StageStatus;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The read side of CQRS: serves PipelineRunSummary from its own projection store,
 * never from a PipelineRun directly. Rebuilding the projection store is a deliberate,
 * explicit step (updateProjection) - see milestone 4 for who calls it and when.
 */
public class PipelineRunQueryService {

  private final Map<String, PipelineRunSummary> projections = new ConcurrentHashMap<>();
  private final Clock clock;

  public PipelineRunQueryService(Clock clock) {
    this.clock = clock;
  }

  public Optional<PipelineRunSummary> getSummary(String runId) {
    return Optional.ofNullable(projections.get(runId));
  }

  public List<PipelineRunSummary> listSummaries() {
    return List.copyOf(projections.values());
  }

  /**
   * Rebuilds the projection for one run from its current write-side state, and stores
   * it, keyed by run.id() - overwriting whatever was there before.
   *
   * TODO: build a PipelineRunSummary from `run`:
   *  - runId: run.id()
   *  - status: run.status().name()
   *  - stagesCompleted: how many of run.stages() have status() == StageStatus.COMPLETE
   *  - stagesTotal: run.stages().size()
   *  - currentStage: the name() of the first stage (in list order) whose status() is
   *    StageStatus.PENDING, or null if none are pending
   *  - duration: Duration.between(run.startedAt(), run.finishedAt().orElse(clock.instant()))
   *    - i.e. "how long it's run so far" while still running, or the final duration
   *    once finished
   * Then put it into `projections` under run.id(), replacing any previous entry.
   */
  public void updateProjection(PipelineRun run) {
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

## Step 3 — tests (copy-paste, must pass once step 2 is done)

These tests use `PipelineRunCommandService` from milestone 2 to put a `PipelineRun`
into various states, then call `updateProjection` *manually* — there's no automatic
wiring between the two services yet. That's intentional: it makes the seam milestone 4
adds visible, by making you feel its absence first.

`src/test/java/com/isaqb/practice/cqrs/query/PipelineRunQueryServiceTest.java`:

```java
package com.isaqb.practice.cqrs.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.isaqb.practice.cqrs.command.PipelineRunCommandService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class PipelineRunQueryServiceTest {

  private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
  private static final Clock FIXED_CLOCK = Clock.fixed(START, ZoneOffset.UTC);

  private final PipelineRunCommandService commandService =
      new PipelineRunCommandService(FIXED_CLOCK);
  private final PipelineRunQueryService queryService = new PipelineRunQueryService(FIXED_CLOCK);

  @Test
  void projectsAFreshlyStartedRun() {
    var run = commandService.startRun("run-1", List.of("compile", "test"));

    queryService.updateProjection(run);

    var summary = queryService.getSummary("run-1").orElseThrow();
    assertEquals("run-1", summary.runId());
    assertEquals("RUNNING", summary.status());
    assertEquals(0, summary.stagesCompleted());
    assertEquals(2, summary.stagesTotal());
    assertEquals("compile", summary.currentStage());
    assertEquals(Duration.ZERO, summary.duration());
  }

  @Test
  void projectsPartialProgressAndAdvancesCurrentStage() {
    commandService.startRun("run-1", List.of("compile", "test"));
    var run = commandService.completeStage("run-1", "compile");

    queryService.updateProjection(run);

    var summary = queryService.getSummary("run-1").orElseThrow();
    assertEquals(1, summary.stagesCompleted());
    assertEquals("test", summary.currentStage());
  }

  @Test
  void projectsNoCurrentStageWhenAllStagesComplete() {
    commandService.startRun("run-1", List.of("compile"));
    var run = commandService.completeStage("run-1", "compile");

    queryService.updateProjection(run);

    var summary = queryService.getSummary("run-1").orElseThrow();
    assertEquals(1, summary.stagesCompleted());
    assertNull(summary.currentStage());
  }

  @Test
  void projectionIsOverwrittenNotAccumulatedOnRepeatedUpdates() {
    commandService.startRun("run-1", List.of("compile", "test"));
    queryService.updateProjection(commandService.get("run-1"));
    var run = commandService.completeStage("run-1", "compile");

    queryService.updateProjection(run);

    assertEquals(1, queryService.listSummaries().size());
    assertEquals(1, queryService.getSummary("run-1").orElseThrow().stagesCompleted());
  }

  @Test
  void missingRunHasNoSummaryUntilProjected() {
    assertTrue(queryService.getSummary("never-projected").isEmpty());
  }
}
```

## Checkpoint

```bash
mvn -f patterns/07-cqrs/pom.xml clean verify
```

All `PipelineRunQueryServiceTest` cases pass. Notice you had to call
`queryService.updateProjection(...)` yourself after every command in these tests — in
milestone 4 that call becomes automatic. Keep that feeling in mind; it's exactly the
gap an asynchronous projection consumer would also have to fill, just later and on a
different thread.

Next: [`04-wiring-and-eventual-consistency.md`](04-wiring-and-eventual-consistency.md).
