# Milestone 4 — Wiring, and the eventual-consistency thought experiment

## Goal

Right now `PipelineRunCommandService` and `PipelineRunQueryService` don't know about
each other — milestone 3's tests called `updateProjection` by hand after every command.
This milestone closes that gap through an explicit seam (`PipelineRunChangeListener`),
writes the `Main` composition root that wires everything together, and then asks you to
reason about what would change if that seam became asynchronous. No new business logic
gets written here — the interesting decisions were milestone 2 (command rules) and
milestone 3 (projection mapping). This milestone is about *how the two sides talk to
each other*, which is where CQRS's eventual-consistency trade-off actually lives.

## Step 1 — the seam (copy-paste)

`src/main/java/com/isaqb/practice/cqrs/command/PipelineRunChangeListener.java`:

```java
package com.isaqb.practice.cqrs.command;

/**
 * The seam between the write and read sides. PipelineRunCommandService calls this
 * after every successful command - it has no idea whether the implementation updates
 * a projection synchronously in the same thread (this exercise) or hands the run off
 * to an async queue for a separate consumer to project later (the real-world,
 * eventually consistent version - see the thought experiment at the end of this
 * milestone, and README section 3).
 */
public interface PipelineRunChangeListener {

  void onRunChanged(PipelineRun run);
}
```

## Step 2 — wire the listener into `PipelineRunCommandService` (copy-paste)

Edit the `PipelineRunCommandService` you wrote in milestone 2. Replace its field and
constructor with:

```java
  private final Map<String, PipelineRun> runs = new ConcurrentHashMap<>();
  private final Clock clock;
  private final PipelineRunChangeListener listener;

  public PipelineRunCommandService(Clock clock) {
    this(clock, run -> {});
  }

  public PipelineRunCommandService(Clock clock, PipelineRunChangeListener listener) {
    this.clock = clock;
    this.listener = listener;
  }
```

The single-argument constructor now delegates to the two-argument one with a no-op
listener (`run -> {}`), so every test you wrote in milestone 2 — which all use
`new PipelineRunCommandService(FIXED_CLOCK)` — keeps compiling and passing unchanged.

Then, in each of `startRun`, `completeStage`, and `finishRun`, add one line —
`listener.onRunChanged(run);` — immediately before the `return run;` at the end of the
method (use whatever your local variable is named if it isn't `run`). Do this only on
the success path: a command that throws `InvalidCommandException` or
`PipelineRunNotFoundException` must never notify the listener, because nothing actually
changed.

## Step 3 — the synchronous implementation (copy-paste)

`src/main/java/com/isaqb/practice/cqrs/query/SynchronousProjectionUpdater.java`:

```java
package com.isaqb.practice.cqrs.query;

import com.isaqb.practice.cqrs.command.PipelineRun;
import com.isaqb.practice.cqrs.command.PipelineRunChangeListener;

/**
 * Updates the read-side projection synchronously, in the same thread, immediately
 * after the command that changed it. This is what keeps this exercise's read side
 * always consistent with the write side - and exactly the piece you'd swap out for
 * genuine eventual consistency. See the thought experiment below.
 */
public class SynchronousProjectionUpdater implements PipelineRunChangeListener {

  private final PipelineRunQueryService queryService;

  public SynchronousProjectionUpdater(PipelineRunQueryService queryService) {
    this.queryService = queryService;
  }

  @Override
  public void onRunChanged(PipelineRun run) {
    queryService.updateProjection(run);
  }
}
```

## Step 4 — the composition root (wiring given, one small formatting task left to you)

`src/main/java/com/isaqb/practice/cqrs/Main.java`:

```java
package com.isaqb.practice.cqrs;

import com.isaqb.practice.cqrs.command.PipelineRunChangeListener;
import com.isaqb.practice.cqrs.command.PipelineRunCommandService;
import com.isaqb.practice.cqrs.query.PipelineRunQueryService;
import com.isaqb.practice.cqrs.query.PipelineRunSummary;
import com.isaqb.practice.cqrs.query.SynchronousProjectionUpdater;
import java.time.Clock;
import java.util.List;

/**
 * Composition root: wires the query side's SynchronousProjectionUpdater into the
 * command side as its PipelineRunChangeListener, then drives one pipeline run through
 * both the command side (Pipeline Runner's view) and the query side (Dashboard's view)
 * to demonstrate the split end to end.
 */
public final class Main {

  private Main() {}

  public static void main(String[] args) {
    Clock clock = Clock.systemUTC();
    PipelineRunQueryService queryService = new PipelineRunQueryService(clock);
    PipelineRunChangeListener listener = new SynchronousProjectionUpdater(queryService);
    PipelineRunCommandService commandService = new PipelineRunCommandService(clock, listener);

    // Pipeline Runner's view: issue commands.
    commandService.startRun("nightly-build-42", List.of("compile", "test", "package"));
    commandService.completeStage("nightly-build-42", "compile");
    commandService.completeStage("nightly-build-42", "test");

    // Dashboard's view: read the projection, already up to date - no explicit
    // updateProjection call needed here, unlike milestone 3's tests.
    System.out.println(formatSummary(queryService.getSummary("nightly-build-42").orElseThrow()));

    commandService.completeStage("nightly-build-42", "package");
    commandService.finishRun("nightly-build-42");

    System.out.println(formatSummary(queryService.getSummary("nightly-build-42").orElseThrow()));
  }

  // See step 5 below.
  static String formatSummary(PipelineRunSummary summary) {
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

## Step 5 — output formatting (write this yourself)

Implement `formatSummary` above so it returns a single line along the lines of:

```
nightly-build-42: RUNNING, 2/3 stages done, current=package, duration=PT12.3S
```

or, once finished:

```
nightly-build-42: FINISHED, 3/3 stages done, current=none, duration=PT18S
```

Exact wording is up to you — the test below only checks that the pieces are present.
Use `"none"` (or similar) when `summary.currentStage()` is `null`.

## Step 6 — test (copy-paste, must pass once step 5 is done)

`src/test/java/com/isaqb/practice/cqrs/MainTest.java`:

```java
package com.isaqb.practice.cqrs;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.isaqb.practice.cqrs.query.PipelineRunSummary;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class MainTest {

  @Test
  void formatsARunningSummary() {
    var summary = new PipelineRunSummary("run-1", "RUNNING", 2, 3, "package", Duration.ofSeconds(12));

    String formatted = Main.formatSummary(summary);

    assertTrue(formatted.contains("run-1"));
    assertTrue(formatted.contains("RUNNING"));
    assertTrue(formatted.contains("2"));
    assertTrue(formatted.contains("3"));
    assertTrue(formatted.contains("package"));
  }

  @Test
  void formatsAFinishedSummaryWithNoCurrentStage() {
    var summary = new PipelineRunSummary("run-1", "FINISHED", 3, 3, null, Duration.ofSeconds(18));

    String formatted = Main.formatSummary(summary);

    assertTrue(formatted.contains("FINISHED"));
    assertTrue(formatted.toLowerCase().contains("none"));
  }
}
```

## Step 7 — try it for real

```bash
mvn -f patterns/07-cqrs/pom.xml clean package
java -jar patterns/07-cqrs/target/cqrs-1.0.0-SNAPSHOT.jar
```

You should see two summary lines printed: one mid-run (2/3 stages done), one finished
(3/3). Both come from `queryService.getSummary(...)` — `Main` never reads a
`PipelineRun` directly to build them.

## Thought experiment: what if this were asynchronous?

`SynchronousProjectionUpdater` calls `queryService.updateProjection(run)` directly, in
the same thread, before `onRunChanged` returns — which is why `Main` can call a command
and immediately read an up-to-date summary with no waiting. That's *not* what you'd get
in a system where read and write sides are scaled independently.

Picture a `QueuedProjectionUpdater` instead: its `onRunChanged` just puts `run` on a
queue and returns immediately, and a separate consumer (a different thread, or a
different process entirely) pulls runs off that queue and calls
`queryService.updateProjection` on its own schedule. Swapping it in would be a one-line
change in `Main` — construct a `QueuedProjectionUpdater` instead of a
`SynchronousProjectionUpdater` — because `PipelineRunCommandService` only depends on the
`PipelineRunChangeListener` interface, never on a concrete implementation. Nothing in
`command` or `query` would need to change.

But the *behavior* would change: immediately after `commandService.completeStage(...)`
returns, `queryService.getSummary(...)` could briefly return the *previous* summary,
until the consumer catches up. A Dashboard reading during that window sees stale data.
That's the eventual-consistency trade-off from README section 3, made concrete: you'd
accept that staleness window in exchange for the command side never blocking on however
long projection rebuilding takes, and the freedom to scale or redeploy the projection
consumer independently of the command service.

This exercise does not ask you to implement `QueuedProjectionUpdater` — a real one needs
a thread pool or an actual message queue, and the point was to *see* the seam, not to
stand up infrastructure. If you want the extra practice, it's a reasonable stretch goal
using `java.util.concurrent.BlockingQueue` and a background thread, but it's not part of
this milestone's checkpoint.

## Checkpoint

- [ ] `mvn -f patterns/07-cqrs/pom.xml clean verify` passes, all packages' tests green.
- [ ] The jar run in step 7 behaves as described.
- [ ] You can point to the one line in `Main` that would change to make projection
      updates asynchronous, and state in one sentence what a Dashboard user might
      briefly see as a result.

Next: [`05-build-and-release.md`](05-build-and-release.md).
