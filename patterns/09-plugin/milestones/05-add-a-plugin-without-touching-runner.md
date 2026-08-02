# Milestone 5 — Add a plugin without touching the runner

## Goal

This is the milestone that *proves* the extension point works, rather than just
asserting it in the README. Add a third step type, `DockerBuildStepPlugin`, and wire
a demo `Main` that runs a pipeline mixing all three plugin types — without changing a
single line of `PipelineRunner.java`, `StepDefinition.java`, `StepResult.java`, or
either existing plugin.

## Step 1 — `DockerBuildStepPlugin` (write the body yourself)

Create `src/main/java/com/isaqb/practice/plugin/plugins/DockerBuildStepPlugin.java`,
same shape as `ShellStepPlugin`:

- `id()` returns `"docker-build"`.
- `execute(context)` reads `image` and `tag` params (via `context.require`), and
  returns `StepResult.ok("built image " + image + ":" + tag)`.

## Step 2 — `Main`, the composition root (copy-paste, then extend)

`src/main/java/com/isaqb/practice/plugin/Main.java`:

```java
package com.isaqb.practice.plugin;

import com.isaqb.practice.plugin.plugins.DockerBuildStepPlugin;
import com.isaqb.practice.plugin.plugins.NotifyStepPlugin;
import com.isaqb.practice.plugin.plugins.ShellStepPlugin;
import java.util.List;
import java.util.Map;

/** Composition root: this is the only class allowed to import a concrete plugin. */
public final class Main {

  public static void main(String[] args) {
    var registry = new PluginRegistry();
    registry.register(new ShellStepPlugin());
    registry.register(new NotifyStepPlugin());
    registry.register(new DockerBuildStepPlugin());

    var runner = new PipelineRunner(registry);
    var steps = List.of(
        new StepDefinition("shell", Map.of("command", "./gradlew build")),
        new StepDefinition("docker-build", Map.of("image", "pipelineforge/build-validator", "tag", "1.0.0")),
        new StepDefinition("notify", Map.of("channel", "slack", "message", "build finished")));

    var result = runner.run(steps);
    result.stepResults().forEach(r -> System.out.println((r.success() ? "OK   " : "FAIL ") + r.message()));
    System.out.println("all succeeded: " + result.allSucceeded());
  }

  private Main() {}
}
```

Run it:

```bash
mvn -f patterns/09-plugin/pom.xml -q compile exec:java -Dexec.mainClass=com.isaqb.practice.plugin.Main
```

(If the `exec` goal isn't available in your local Maven setup, `mvn package` and
`java -cp target/classes com.isaqb.practice.plugin.Main` works the same way.)

## Step 3 — the proof (write this test yourself)

Write `src/test/java/com/isaqb/practice/plugin/DockerBuildStepPluginTest.java`
covering `DockerBuildStepPlugin` the same way you tested `ShellStepPlugin` in
milestone 3.

Then, as the actual "proof" step: run `git diff` (or your editor's change view)
scoped to `PipelineRunner.java`, `StepDefinition.java`, `StepResult.java`,
`ShellStepPlugin.java`, and `NotifyStepPlugin.java`. Confirm none of them changed in
this milestone — only a new file (`DockerBuildStepPlugin.java`) and `Main.java`'s
registration list did.

## Checkpoint

- [ ] `mvn -f patterns/09-plugin/pom.xml clean verify` is green, including
      `DockerBuildStepPluginTest`.
- [ ] Running `Main` prints three `OK` lines and `all succeeded: true`.
- [ ] You confirmed (by diff, not by memory) that adding the third plugin touched zero
      existing core or plugin files besides `Main`.

Next: [`06-build-and-release.md`](06-build-and-release.md).
