# Milestone 3 — Two built-in plugins

## Goal

Implement the first two real step types as plugins: `ShellStepPlugin` and
`NotifyStepPlugin`. Each is a self-contained implementation of `PipelineStepPlugin` —
neither knows the other exists, and neither will be referenced anywhere except `Main`
(milestone 4 onward).

## Step 1 — package (copy-paste)

Create the `plugins` sub-package: `src/main/java/com/isaqb/practice/plugin/plugins/`.

## Step 2 — `ShellStepPlugin` (write the body yourself)

Create `src/main/java/com/isaqb/practice/plugin/plugins/ShellStepPlugin.java`:

```java
package com.isaqb.practice.plugin.plugins;

import com.isaqb.practice.plugin.PipelineStepPlugin;
import com.isaqb.practice.plugin.StepContext;
import com.isaqb.practice.plugin.StepResult;

/**
 * Simulates running a shell command. This is a practice exercise, not a real process
 * launcher — it never calls {@code ProcessBuilder}; it only validates the {@code
 * command} param and reports success.
 */
public class ShellStepPlugin implements PipelineStepPlugin {

  @Override
  public String id() {
    return "shell";
  }

  @Override
  public StepResult execute(StepContext context) {
    // TODO: read the "command" param via context.require("command").
    // If it's blank (after trim), return StepResult.failed("command must not be blank").
    // Otherwise return StepResult.ok("ran: " + command).
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

## Step 3 — `NotifyStepPlugin` (write the body yourself)

Create `src/main/java/com/isaqb/practice/plugin/plugins/NotifyStepPlugin.java`, same
shape, `id()` returns `"notify"`. Its `execute` reads a `message` param and a
`channel` param (e.g. `"slack"`, `"email"`); if either is missing,
`context.require` already throws for you. On success, return
`StepResult.ok("notified " + channel + ": " + message)`.

## Step 4 — tests (write these yourself)

For each plugin, write a test class asserting:

- a successful call with valid params returns `StepResult.success() == true` and a
  `message()` containing the expected substring.
- `ShellStepPlugin` specifically: an empty/blank `command` param returns
  `StepResult.success() == false` (not an exception — a blank command is an expected,
  reportable failure, not a bug).

## Checkpoint

```bash
mvn -f patterns/09-plugin/pom.xml clean verify
```

Both plugins' tests pass. Confirm neither plugin class imports the other, and neither
imports `PluginRegistry` — plugins don't know about the registry; only `Main` does.

Next: [`04-pipeline-runner.md`](04-pipeline-runner.md).
