# Milestone 1 — The plugin contract

## Goal

Define the extension point itself: the interface every plugin must implement, and the
two data types that carry information across it. Once this milestone is done, this
contract must never change shape just to accommodate one plugin's needs — that's the
signal a later plugin is leaking implementation detail into the core.

Delete `src/test/java/com/isaqb/practice/plugin/SmokeTest.java` now — the tests you
add in this milestone replace it as your "is the build green" signal.

## Step 1 — step context and result (copy-paste)

`src/main/java/com/isaqb/practice/plugin/StepContext.java`:

```java
package com.isaqb.practice.plugin;

import java.util.Map;

/** Everything a plugin needs to execute one step: its declared parameters. */
public record StepContext(Map<String, String> params) {

  public StepContext {
    params = Map.copyOf(params);
  }

  public String require(String key) {
    var value = params.get(key);
    if (value == null) {
      throw new IllegalArgumentException("missing required param: " + key);
    }
    return value;
  }
}
```

`src/main/java/com/isaqb/practice/plugin/StepResult.java`:

```java
package com.isaqb.practice.plugin;

/** What a plugin reports after executing one step. */
public record StepResult(boolean success, String message) {

  public static StepResult ok(String message) {
    return new StepResult(true, message);
  }

  public static StepResult failed(String message) {
    return new StepResult(false, message);
  }
}
```

## Step 2 — the extension-point interface (copy-paste)

`src/main/java/com/isaqb/practice/plugin/PipelineStepPlugin.java`:

```java
package com.isaqb.practice.plugin;

/**
 * The stable contract every pipeline step type implements. The {@link PipelineRunner}
 * (milestone 4) depends on this interface and on {@link PluginRegistry} only — never
 * on a concrete implementation.
 */
public interface PipelineStepPlugin {

  /** The type id this plugin registers under, e.g. {@code "shell"}. Must be stable. */
  String id();

  /** Executes one step. Must not throw for expected failures — return a failed result. */
  StepResult execute(StepContext context);
}
```

## Step 3 — a test double plugin and a contract test (write these yourself)

Create `src/test/java/com/isaqb/practice/plugin/EchoPlugin.java`, a minimal test-only
plugin implementing `PipelineStepPlugin`:

- `id()` returns `"echo"`.
- `execute(context)` returns `StepResult.ok(context.require("message"))`.

This isn't a "real" step type — it exists purely so milestones 1-2 can test the
contract and the registry before any production plugin exists.

Then write `src/test/java/com/isaqb/practice/plugin/EchoPluginTest.java` asserting:

- calling `execute` with `params = {"message": "hi"}` returns a successful
  `StepResult` whose `message()` is `"hi"`.
- calling `execute` with an empty params map throws `IllegalArgumentException` (from
  `StepContext.require`).

## Checkpoint

```bash
mvn -f patterns/09-plugin/pom.xml clean verify
```

`EchoPluginTest` passes. You can explain, in one sentence, why `StepContext` and
`StepResult` are plain data (no behavior) while `PipelineStepPlugin` is the only piece
of the contract with behavior.

Next: [`02-registry.md`](02-registry.md).
