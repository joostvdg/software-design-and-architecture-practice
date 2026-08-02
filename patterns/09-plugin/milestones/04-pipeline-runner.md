# Milestone 4 — The pipeline runner (the core)

## Goal

Build `PipelineRunner`: the core that executes an ordered list of step definitions,
resolving each one's plugin through the registry, and never importing a class from
the `plugins` package. This milestone is where you feel the extension point actually
being *used*, not just defined.

## Step 1 — step definition and run result (copy-paste)

`src/main/java/com/isaqb/practice/plugin/StepDefinition.java`:

```java
package com.isaqb.practice.plugin;

import java.util.Map;

/** One entry in a pipeline: which plugin type to invoke, with what params. */
public record StepDefinition(String pluginId, Map<String, String> params) {

  public StepDefinition {
    params = Map.copyOf(params);
  }
}
```

`src/main/java/com/isaqb/practice/plugin/PipelineRunResult.java`:

```java
package com.isaqb.practice.plugin;

import java.util.List;

/** The outcome of running a whole pipeline: one result per step, in order. */
public record PipelineRunResult(List<StepResult> stepResults) {

  public PipelineRunResult {
    stepResults = List.copyOf(stepResults);
  }

  public boolean allSucceeded() {
    return stepResults.stream().allMatch(StepResult::success);
  }
}
```

## Step 2 — `PipelineRunner` (write the core logic yourself)

Create `src/main/java/com/isaqb/practice/plugin/PipelineRunner.java`:

```java
package com.isaqb.practice.plugin;

import java.util.List;

/**
 * Executes a pipeline: a list of {@link StepDefinition}s, resolved to behavior purely
 * through {@link PluginRegistry#lookup(String)}. Never references a concrete plugin
 * class — that's the whole point of this exercise.
 */
public class PipelineRunner {

  private final PluginRegistry registry;

  public PipelineRunner(PluginRegistry registry) {
    this.registry = registry;
  }

  /**
   * Runs each step in order. Stops at (and includes) the first failed step's result —
   * does not run steps after a failure. Returns the accumulated results either way.
   */
  public PipelineRunResult run(List<StepDefinition> steps) {
    // TODO: for each StepDefinition, look up its plugin via registry.lookup(pluginId),
    // build a StepContext from its params, call execute(), and collect the StepResult.
    // Stop as soon as one StepResult has success() == false; don't run later steps.
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

Fail-fast (stop on first failure) mirrors what a real pipeline runner does: there's
usually no point running "deploy" after "build" failed.

## Step 3 — tests (copy-paste, must pass once step 2 is done)

`src/test/java/com/isaqb/practice/plugin/PipelineRunnerTest.java`:

```java
package com.isaqb.practice.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PipelineRunnerTest {

  @Test
  void runsAllStepsInOrderWhenAllSucceed() {
    var registry = new PluginRegistry();
    registry.register(new EchoPlugin());

    var runner = new PipelineRunner(registry);
    var steps = List.of(
        new StepDefinition("echo", Map.of("message", "first")),
        new StepDefinition("echo", Map.of("message", "second")));

    var result = runner.run(steps);

    assertTrue(result.allSucceeded());
    assertEquals(2, result.stepResults().size());
    assertEquals("first", result.stepResults().get(0).message());
    assertEquals("second", result.stepResults().get(1).message());
  }

  @Test
  void unknownPluginIdPropagatesAsUnknownPluginException() {
    var runner = new PipelineRunner(new PluginRegistry());

    org.junit.jupiter.api.Assertions.assertThrows(
        UnknownPluginException.class,
        () -> runner.run(List.of(new StepDefinition("nope", Map.of()))));
  }
}
```

Write one more test yourself: two steps where the first fails (use `ShellStepPlugin`
with a blank `command`) and the second would succeed if it ran — assert the result
has exactly **one** `StepResult` (the failed one), proving the runner stopped early.

## Checkpoint

```bash
mvn -f patterns/09-plugin/pom.xml clean verify
```

All `PipelineRunnerTest` cases pass, including the fail-fast test you wrote. Grep
`PipelineRunner.java` for `import com.isaqb.practice.plugin.plugins` — it should find
nothing.

Next: [`05-add-a-plugin-without-touching-runner.md`](05-add-a-plugin-without-touching-runner.md).
