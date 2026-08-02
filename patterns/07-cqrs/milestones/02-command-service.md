# Milestone 2 — Command service

## Goal

Build `PipelineRunCommandService`: the only class allowed to create or mutate a
`PipelineRun`. Every public method corresponds to one command the Pipeline Runner can
issue — `startRun`, `completeStage`, `finishRun` — and each one enforces a rule about
what's a *legal* state transition. This is the write side's real job in CQRS: not just
storing state, but being the single place that decides whether a change is allowed.

## Step 1 — the class shell (storage and lookup given, commands left to you)

`src/main/java/com/isaqb/practice/cqrs/command/PipelineRunCommandService.java`:

```java
package com.isaqb.practice.cqrs.command;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The write side of CQRS: the only class allowed to create or mutate a PipelineRun.
 * Each public method is one command a Pipeline Runner can issue.
 */
public class PipelineRunCommandService {

  private final Map<String, PipelineRun> runs = new ConcurrentHashMap<>();
  private final Clock clock;

  public PipelineRunCommandService(Clock clock) {
    this.clock = clock;
  }

  /** Looks up a run by id, or throws if it doesn't exist. */
  public PipelineRun get(String runId) {
    PipelineRun run = runs.get(runId);
    if (run == null) {
      throw new PipelineRunNotFoundException(runId);
    }
    return run;
  }

  /**
   * Starts a new pipeline run with the given stages, all initially PENDING.
   *
   * TODO:
   *  - Reject a blank runId or an empty stageNames list with InvalidCommandException.
   *  - Reject a runId that's already in use with InvalidCommandException (a run id
   *    must be unique - starting the same run twice is a bug in the caller, not
   *    something to silently overwrite).
   *  - Otherwise, create a new PipelineRun (use clock.instant() for startedAt), store
   *    it, and return it.
   */
  public PipelineRun startRun(String runId, List<String> stageNames) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  /**
   * Marks one stage of an existing run as complete.
   *
   * TODO:
   *  - Look the run up via get(runId) (throws PipelineRunNotFoundException if missing
   *    - nothing more to do there).
   *  - Reject if the run's status is already FINISHED (InvalidCommandException - you
   *    can't complete a stage on a run that's already done).
   *  - Reject if no stage with that name exists (InvalidCommandException). Hint:
   *    run.findStage(stageName) is package-private and visible here.
   *  - Reject if that stage is already COMPLETE (InvalidCommandException - completing
   *    the same stage twice is a bug in the caller, not a no-op).
   *  - Otherwise, mark it complete and return the run. Hint: PipelineStage.markComplete()
   *    is package-private but PipelineRun doesn't expose a public "complete this stage"
   *    method - you're expected to call stage.markComplete() directly here, since
   *    PipelineRunCommandService is in the same package as PipelineStage.
   */
  public PipelineRun completeStage(String runId, String stageName) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  /**
   * Finishes an existing run.
   *
   * TODO:
   *  - Look the run up via get(runId).
   *  - Reject if the run's status is already FINISHED (InvalidCommandException).
   *  - Reject if any stage is not yet COMPLETE (InvalidCommandException - a run can
   *    only finish once every stage is done). Hint: run.allStagesComplete() is
   *    package-private and visible here.
   *  - Otherwise, call run.finish(clock.instant()) and return the run.
   */
  public PipelineRun finishRun(String runId) {
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

## Step 2 — tests (copy-paste, must pass once step 1 is done)

`src/test/java/com/isaqb/practice/cqrs/command/PipelineRunCommandServiceTest.java`:

```java
package com.isaqb.practice.cqrs.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class PipelineRunCommandServiceTest {

  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

  private final PipelineRunCommandService service = new PipelineRunCommandService(FIXED_CLOCK);

  @Test
  void startRunCreatesARunningRunWithPendingStages() {
    PipelineRun run = service.startRun("run-1", List.of("compile", "test"));

    assertEquals(RunStatus.RUNNING, run.status());
    assertEquals(2, run.stages().size());
    assertTrue(run.stages().stream().allMatch(s -> s.status() == StageStatus.PENDING));
  }

  @Test
  void startRunRejectsDuplicateRunId() {
    service.startRun("run-1", List.of("compile"));

    assertThrows(
        InvalidCommandException.class, () -> service.startRun("run-1", List.of("compile")));
  }

  @Test
  void startRunRejectsEmptyStageList() {
    assertThrows(InvalidCommandException.class, () -> service.startRun("run-1", List.of()));
  }

  @Test
  void completeStageMarksOnlyTheNamedStageComplete() {
    service.startRun("run-1", List.of("compile", "test"));

    PipelineRun run = service.completeStage("run-1", "compile");

    var compile = run.stages().stream().filter(s -> s.name().equals("compile")).findFirst().orElseThrow();
    var test = run.stages().stream().filter(s -> s.name().equals("test")).findFirst().orElseThrow();
    assertEquals(StageStatus.COMPLETE, compile.status());
    assertEquals(StageStatus.PENDING, test.status());
  }

  @Test
  void completeStageRejectsUnknownRun() {
    assertThrows(
        PipelineRunNotFoundException.class, () -> service.completeStage("no-such-run", "compile"));
  }

  @Test
  void completeStageRejectsUnknownStage() {
    service.startRun("run-1", List.of("compile"));

    assertThrows(
        InvalidCommandException.class, () -> service.completeStage("run-1", "no-such-stage"));
  }

  @Test
  void completeStageRejectsAlreadyCompleteStage() {
    service.startRun("run-1", List.of("compile"));
    service.completeStage("run-1", "compile");

    assertThrows(InvalidCommandException.class, () -> service.completeStage("run-1", "compile"));
  }

  @Test
  void finishRunRejectsIncompleteStages() {
    service.startRun("run-1", List.of("compile", "test"));
    service.completeStage("run-1", "compile");

    assertThrows(InvalidCommandException.class, () -> service.finishRun("run-1"));
  }

  @Test
  void finishRunSucceedsOnceAllStagesComplete() {
    service.startRun("run-1", List.of("compile", "test"));
    service.completeStage("run-1", "compile");
    service.completeStage("run-1", "test");

    PipelineRun run = service.finishRun("run-1");

    assertEquals(RunStatus.FINISHED, run.status());
    assertEquals(FIXED_CLOCK.instant(), run.finishedAt().orElseThrow());
  }

  @Test
  void finishRunRejectsAlreadyFinishedRun() {
    service.startRun("run-1", List.of("compile"));
    service.completeStage("run-1", "compile");
    service.finishRun("run-1");

    assertThrows(InvalidCommandException.class, () -> service.finishRun("run-1"));
  }
}
```

## Checkpoint

```bash
mvn -f patterns/07-cqrs/pom.xml clean verify
```

All `PipelineRunCommandServiceTest` cases pass. Take a moment to check: every rejection
path above throws *before* mutating anything — a rejected command should never leave a
run half-changed. That matters more here than it would look at first: the query side
(milestone 3) will only ever see runs through this service, so an inconsistent
intermediate state here would leak straight into the read model.

Next: [`03-read-model.md`](03-read-model.md).
