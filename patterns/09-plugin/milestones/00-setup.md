# Milestone 0 — Setup

## Goal

Confirm the module builds, understand the target package layout, and get oriented in
the case study before writing any pattern code.

## Confirm the build is green

From the repo root:

```bash
mvn -f patterns/09-plugin/pom.xml clean verify
```

This should pass — right now the module only contains a placeholder test
(`SmokeTest`) and an empty `package-info.java`. That placeholder test is there so you
always have a green build to come back to; delete it once the plugin contract (next
milestone) has its own tests.

## Target layout

By the end of milestone 5 you'll have:

```
src/main/java/com/isaqb/practice/plugin/
  Main.java                    # wires the registry, registers plugins, runs a demo pipeline
  PipelineStepPlugin.java      # the extension-point interface
  StepContext.java             # input to a plugin's execute()
  StepResult.java              # output of a plugin's execute()
  PluginRegistry.java          # register-by-id / look-up-by-id
  UnknownPluginException.java
  DuplicatePluginException.java
  PipelineRunner.java          # the core: executes a list of steps via the registry only
  StepDefinition.java
  PipelineRunResult.java
  plugins/
    ShellStepPlugin.java
    NotifyStepPlugin.java
    DockerBuildStepPlugin.java  # added in milestone 5, without touching PipelineRunner
```

Notice the dependency direction as you build this: `PipelineRunner` imports
`PipelineStepPlugin`, `PluginRegistry`, `StepDefinition`, and `StepContext`/
`StepResult` — never a concrete class under `plugins/`. Only `Main` (the composition
root) imports concrete plugin classes, to construct and register them. That's the
whole pattern: the core's source code never changes to support a new plugin, only
`Main`'s registration list does.

## The case study, one more time

You're building the **Pipeline Step Plugin Loader**: a `PipelineRunner` that executes
an ordered list of pipeline steps, where each step's *type* (`"shell"`, `"notify"`,
later `"docker-build"`) is resolved to behavior through a `PluginRegistry`, not a
hardcoded `switch`. A step definition looks like this (you'll model this as a small
record in milestone 4):

```
type: shell
param: command=./gradlew build
```

The runner doesn't know what `"shell"` means — it asks the registry for the plugin
registered under that id, and calls its `execute` method.

## Checkpoint

- [ ] `mvn -f patterns/09-plugin/pom.xml clean verify` passes.
- [ ] You can explain, in one sentence, why `Main` is the only class allowed to import
      a concrete class under `plugins/`.

Next: [`01-plugin-contract.md`](01-plugin-contract.md).
