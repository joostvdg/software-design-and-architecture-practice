# Milestone 1 — Write model

## Goal

Build the write side's data model: the `PipelineRun` aggregate, its `PipelineStage`
children, and the two status enums. This is the *only* place `PipelineRun` state can be
mutated from — every mutating method is package-private to `command`, so nothing
outside this package (in particular, nothing in the `query` package you'll build in
milestone 3) can change a run's state directly. That restriction is CQRS's write-side
half made literal: one narrow gate for all state changes.

This milestone is all copy-paste — no `TODO`s. The interesting decisions (what commands
are allowed, what makes a command invalid) come in milestone 2, once there's a model to
apply them to.

Delete `src/test/java/com/isaqb/practice/cqrs/SmokeTest.java` now — the test you add in
this milestone replaces it as your "is the build green" signal.

## Step 1 — status enums (copy-paste)

`src/main/java/com/isaqb/practice/cqrs/command/RunStatus.java`:

```java
package com.isaqb.practice.cqrs.command;

/** The overall lifecycle state of a pipeline run. */
public enum RunStatus {
  RUNNING,
  FINISHED
}
```

`src/main/java/com/isaqb/practice/cqrs/command/StageStatus.java`:

```java
package com.isaqb.practice.cqrs.command;

/** The lifecycle state of one stage within a pipeline run. */
public enum StageStatus {
  PENDING,
  COMPLETE
}
```

## Step 2 — `PipelineStage` (copy-paste)

`src/main/java/com/isaqb/practice/cqrs/command/PipelineStage.java`:

```java
package com.isaqb.practice.cqrs.command;

/**
 * One stage within a pipeline run. Mutable only from within the `command` package -
 * {@link #markComplete()} is package-private on purpose, so only
 * {@code PipelineRunCommandService} (via {@code PipelineRun}) can ever complete a
 * stage.
 */
public final class PipelineStage {

  private final String name;
  private StageStatus status;

  public PipelineStage(String name) {
    this.name = name;
    this.status = StageStatus.PENDING;
  }

  public String name() {
    return name;
  }

  public StageStatus status() {
    return status;
  }

  void markComplete() {
    this.status = StageStatus.COMPLETE;
  }
}
```

## Step 3 — `PipelineRun` (copy-paste)

`src/main/java/com/isaqb/practice/cqrs/command/PipelineRun.java`:

```java
package com.isaqb.practice.cqrs.command;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The write-side aggregate: the one source of truth for a pipeline run's state.
 * Every mutating method is package-private - only {@code PipelineRunCommandService},
 * in this same package, is allowed to call them. The `query` package only ever sees
 * this class's public (read-only) surface.
 */
public final class PipelineRun {

  private final String id;
  private final List<PipelineStage> stages;
  private final Instant startedAt;
  private RunStatus status;
  private Instant finishedAt;

  public PipelineRun(String id, List<String> stageNames, Instant startedAt) {
    this.id = id;
    this.stages = new ArrayList<>();
    for (String stageName : stageNames) {
      this.stages.add(new PipelineStage(stageName));
    }
    this.startedAt = startedAt;
    this.status = RunStatus.RUNNING;
  }

  public String id() {
    return id;
  }

  public List<PipelineStage> stages() {
    return List.copyOf(stages);
  }

  public RunStatus status() {
    return status;
  }

  public Instant startedAt() {
    return startedAt;
  }

  public Optional<Instant> finishedAt() {
    return Optional.ofNullable(finishedAt);
  }

  Optional<PipelineStage> findStage(String stageName) {
    return stages.stream().filter(stage -> stage.name().equals(stageName)).findFirst();
  }

  boolean allStagesComplete() {
    return stages.stream().allMatch(stage -> stage.status() == StageStatus.COMPLETE);
  }

  void finish(Instant finishedAt) {
    this.status = RunStatus.FINISHED;
    this.finishedAt = finishedAt;
  }
}
```

## Step 4 — exceptions (copy-paste)

`src/main/java/com/isaqb/practice/cqrs/command/PipelineRunNotFoundException.java`:

```java
package com.isaqb.practice.cqrs.command;

/** Thrown when a command targets a run id that doesn't exist. */
public class PipelineRunNotFoundException extends RuntimeException {

  public PipelineRunNotFoundException(String runId) {
    super("no pipeline run with id: " + runId);
  }
}
```

`src/main/java/com/isaqb/practice/cqrs/command/InvalidCommandException.java`:

```java
package com.isaqb.practice.cqrs.command;

/** Thrown when a command would violate a pipeline run's lifecycle rules. */
public class InvalidCommandException extends RuntimeException {

  public InvalidCommandException(String message) {
    super(message);
  }
}
```

## Step 5 — test (copy-paste)

`src/test/java/com/isaqb/practice/cqrs/command/PipelineRunTest.java`:

```java
package com.isaqb.practice.cqrs.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PipelineRunTest {

  @Test
  void startsWithAllStagesPendingAndStatusRunning() {
    var run = new PipelineRun("run-1", List.of("compile", "test"), Instant.EPOCH);

    assertEquals(RunStatus.RUNNING, run.status());
    assertEquals(2, run.stages().size());
    assertTrue(run.stages().stream().allMatch(s -> s.status() == StageStatus.PENDING));
    assertTrue(run.finishedAt().isEmpty());
  }

  @Test
  void stagesAreReturnedAsAnImmutableSnapshot() {
    var run = new PipelineRun("run-1", List.of("compile"), Instant.EPOCH);

    List<PipelineStage> stages = run.stages();

    assertThrows(UnsupportedOperationException.class, () -> stages.add(new PipelineStage("extra")));
  }
}
```

## Checkpoint

```bash
mvn -f patterns/07-cqrs/pom.xml clean verify
```

`PipelineRunTest` passes, and nothing under `command/` imports anything from a `query`
package (it doesn't exist yet — it will after milestone 3, and it must stay that way
afterwards too: `command` never imports `query`).

Next: [`02-command-service.md`](02-command-service.md).
