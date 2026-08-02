# Milestone 1 — The Model

## Goal

Build `PipelineRunModel`: the only place run state lives, and the only place the
rules for changing it are enforced. Nothing in this milestone renders anything or
reads a command string — that's the View's and Controller's job, in later milestones.

Delete `src/test/java/com/isaqb/practice/mvcfamily/SmokeTest.java` now — the tests you
add in this milestone replace it as your "is the build green" signal.

## Step 1 — status enum and run record (copy-paste)

`src/main/java/com/isaqb/practice/mvcfamily/RunStatus.java`:

```java
package com.isaqb.practice.mvcfamily;

public enum RunStatus {
  RUNNING,
  FINISHED,
  FAILED
}
```

`src/main/java/com/isaqb/practice/mvcfamily/PipelineRun.java`:

```java
package com.isaqb.practice.mvcfamily;

/** One pipeline run's state at a point in time. Immutable — the Model replaces it, not mutates it. */
public record PipelineRun(String id, RunStatus status, int stagesCompleted, int stagesTotal) {

  public static PipelineRun starting(String id, int stagesTotal) {
    return new PipelineRun(id, RunStatus.RUNNING, 0, stagesTotal);
  }
}
```

## Step 2 — exceptions (copy-paste)

`src/main/java/com/isaqb/practice/mvcfamily/UnknownRunException.java`:

```java
package com.isaqb.practice.mvcfamily;

public class UnknownRunException extends RuntimeException {

  public UnknownRunException(String runId) {
    super("no such run: " + runId);
  }
}
```

`src/main/java/com/isaqb/practice/mvcfamily/InvalidTransitionException.java`:

```java
package com.isaqb.practice.mvcfamily;

public class InvalidTransitionException extends RuntimeException {

  public InvalidTransitionException(String message) {
    super(message);
  }
}
```

## Step 3 — `PipelineRunModel` (write the core logic yourself)

Create `src/main/java/com/isaqb/practice/mvcfamily/PipelineRunModel.java`:

```java
package com.isaqb.practice.mvcfamily;

import java.util.List;

/** Owns every run's state and the rules for changing it. Knows nothing about text or stdin. */
public class PipelineRunModel {

  // TODO: back this with a Map<String, PipelineRun> (or LinkedHashMap to keep insertion order
  // stable for the View's table).

  public void addRun(String id, int stagesTotal) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  /**
   * Completes the next stage of {@code runId}. If this was the last stage, the run's
   * status becomes FINISHED.
   *
   * @throws UnknownRunException if no run has this id.
   * @throws InvalidTransitionException if the run is not RUNNING (already FINISHED or FAILED).
   */
  public void advanceStage(String runId) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  /**
   * Marks a run FAILED.
   *
   * @throws UnknownRunException if no run has this id.
   * @throws InvalidTransitionException if the run is not RUNNING.
   */
  public void failRun(String runId) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  /** Read-only snapshot of every run, in the order they were added. */
  public List<PipelineRun> allRuns() {
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

The "can't advance a finished/failed run" rule is the exercise's stand-in for every
real Model's job: deciding which state transitions are even legal, independent of who
asked for them or how.

## Step 4 — tests (copy-paste, must pass once step 3 is done)

`src/test/java/com/isaqb/practice/mvcfamily/PipelineRunModelTest.java`:

```java
package com.isaqb.practice.mvcfamily;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PipelineRunModelTest {

  private final PipelineRunModel model = new PipelineRunModel();

  @Test
  void advancingCompletesStagesAndFinishesOnTheLastOne() {
    model.addRun("run-1", 2);

    model.advanceStage("run-1");
    assertEquals(RunStatus.RUNNING, model.allRuns().get(0).status());
    assertEquals(1, model.allRuns().get(0).stagesCompleted());

    model.advanceStage("run-1");
    assertEquals(RunStatus.FINISHED, model.allRuns().get(0).status());
  }

  @Test
  void advancingAFinishedRunThrows() {
    model.addRun("run-1", 1);
    model.advanceStage("run-1");

    assertThrows(InvalidTransitionException.class, () -> model.advanceStage("run-1"));
  }

  @Test
  void advancingUnknownRunThrows() {
    assertThrows(UnknownRunException.class, () -> model.advanceStage("nope"));
  }

  @Test
  void failingARunningRunSetsFailedStatus() {
    model.addRun("run-1", 3);

    model.failRun("run-1");

    assertEquals(RunStatus.FAILED, model.allRuns().get(0).status());
  }
}
```

## Checkpoint

```bash
mvn -f patterns/10-mvc-family/pom.xml clean verify
```

All `PipelineRunModelTest` cases pass, and `PipelineRunModel.java` has zero `import`
statements pointing outside this package's data types — no text formatting, no
`System.out`, no `Scanner`.

Next: [`02-view.md`](02-view.md).
