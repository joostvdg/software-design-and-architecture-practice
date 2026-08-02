# Milestone 0 — Setup

## Goal

Confirm the module builds, understand the target package layout, and get oriented in
the case study before writing any pattern code.

## Confirm the build is green

From the repo root:

```bash
mvn -f patterns/07-cqrs/pom.xml clean verify
```

This should pass — right now the module only contains a placeholder test
(`SmokeTest`) and an empty `package-info.java`. That placeholder test is there so you
always have a green build to come back to; delete it once the write model (next
milestone) has its own tests.

## Target layout

By the end of milestone 4 you'll have:

```
src/main/java/com/isaqb/practice/cqrs/
  Main.java                              # composition root + CLI demo
  command/
    PipelineRun.java                     # write-side aggregate
    PipelineStage.java
    RunStatus.java
    StageStatus.java
    PipelineRunNotFoundException.java
    InvalidCommandException.java
    PipelineRunChangeListener.java       # the write→read seam (milestone 4)
    PipelineRunCommandService.java       # startRun / completeStage / finishRun
  query/
    PipelineRunSummary.java              # read-side DTO
    PipelineRunQueryService.java         # projection store + queries
    SynchronousProjectionUpdater.java    # implements PipelineRunChangeListener (milestone 4)
```

Notice the two packages, `command` and `query`, are the command/query split made
literal in the folder structure. `query` depends on `command` only in the direction of
reading `PipelineRun`'s public state (to build a `PipelineRunSummary` from it) — it
never mutates a `PipelineRun`. Nothing in `command` depends on `query` at all, except
through the `PipelineRunChangeListener` interface, which lives in `command` precisely
so that the command side can call out to *something* without knowing it's the query
side on the other end — that's the seam milestone 4 is about.

## The case study, one more time

You're building the **Pipeline Run Query/Command split**: the Pipeline Runner issues
commands (`startRun`, `completeStage`, `finishRun`) against a write-side `PipelineRun`
aggregate; the Dashboard issues queries against a completely separately-shaped
`PipelineRunSummary`, read from its own projection store. The two are kept in sync by
an explicit step after each command — not by the Dashboard reading the `PipelineRun`
object directly.

## Checkpoint

- [ ] `mvn -f patterns/07-cqrs/pom.xml clean verify` passes.
- [ ] You can explain, in one sentence, why `PipelineRunSummary` is a *separate* type
      from `PipelineRun` rather than just "the same object, read-only."

Next: [`01-write-model.md`](01-write-model.md).
