# Milestone 0 — Setup

## Goal

Confirm the module builds, understand the target package layout, and get oriented in
the case study before writing any pattern code.

## Confirm the build is green

From the repo root:

```bash
mvn -f patterns/10-mvc-family/pom.xml clean verify
```

This should pass — right now the module only contains a placeholder test
(`SmokeTest`) and an empty `package-info.java`. That placeholder test is there so you
always have a green build to come back to; delete it once the Model (next milestone)
has its own tests.

## Target layout

By the end of milestone 4 you'll have:

```
src/main/java/com/isaqb/practice/mvcfamily/
  Main.java                   # CLI driving adapter: reads commands, drives the Controller
  PipelineRun.java            # one run's state: id, status, stagesCompleted, stagesTotal
  RunStatus.java              # enum: RUNNING, FINISHED, FAILED
  PipelineRunModel.java       # owns all runs; the mutation rules live here
  UnknownRunException.java
  InvalidTransitionException.java
  PipelineDashboardView.java  # renders the Model's current state as text
  DashboardController.java    # maps commands to Model mutations, triggers View renders
```

Notice the dependency direction: `PipelineRunModel` imports nothing from this
project. `PipelineDashboardView` imports only `PipelineRunModel`'s *read* surface (it
renders, it never mutates). `DashboardController` imports both, and is the only class
that calls a Model mutation method followed by a View render call — that pairing is
the essence of classic MVC. `Main` imports `DashboardController` only.

## The case study, one more time

You're building the **Pipeline Run Status Dashboard**: a CLI that shows a text table
of pipeline runs and lets a platform engineer advance or fail them. Sample rendered
output (you'll produce exactly this shape in milestone 2):

```
ID        STATUS    PROGRESS
run-1     RUNNING   2/4
run-2     FINISHED  3/3
```

Three commands drive it: `advance <runId>` (complete the next stage), `fail <runId>`
(mark the run failed), and `refresh` (just re-render — useful once the input loop in
milestone 4 exists).

## Checkpoint

- [ ] `mvn -f patterns/10-mvc-family/pom.xml clean verify` passes.
- [ ] You can explain, in one sentence, why `PipelineDashboardView` must not have a
      method that mutates a `PipelineRun`.

Next: [`01-model.md`](01-model.md).
