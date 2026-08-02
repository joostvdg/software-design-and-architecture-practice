# Milestone 0 — Setup

## Goal

Confirm the module builds, understand the target package layout, and get oriented in
the case study before writing any pattern code.

## Confirm the build is green

From the repo root:

```bash
mvn -f patterns/08-event-sourcing/pom.xml clean verify
```

This should pass — right now the module only contains a placeholder test
(`SmokeTest`) and an empty `package-info.java`. That placeholder test is there so you
always have a green build to come back to; delete it once the event store (milestone
2) has its own tests.

## Target layout

By the end of milestone 5 you'll have:

```
src/main/java/com/isaqb/practice/eventsourcing/
  Main.java                          # composition root + demo entry point
  PipelineRunSimulator.java          # the Pipeline Runner actor: appends events only
  Auditor.java                       # the Auditor actor: replays full history
  Dashboard.java                     # the Dashboard actor: computes current state on demand
  event/
    PipelineRunEvent.java             # sealed interface, common to all event kinds
    RunStarted.java
    StageStarted.java
    StageCompleted.java
    StageFailed.java
    RunFinished.java
  store/
    EventStore.java                   # the append-only log contract
    InMemoryEventStore.java           # in-memory implementation - this exercise's "durable" store
  state/
    PipelineRunState.java             # the derived value - never stored, always computed
    RunStatus.java
    StageState.java
    StageStatus.java
  projection/
    PipelineRunProjector.java         # the pattern's core: fold(events) -> state
  snapshot/
    Snapshot.java                     # a cached checkpoint: a state + how many events it accounts for
    SnapshotAssistedProjector.java    # replay that resumes from a Snapshot instead of event 0
```

Notice the dependency shape as you build this: `event/` depends on nothing else in this
project — it's pure data, exactly like a broker's events would be, except here nobody
ever dispatches them anywhere. `store/` depends only on `event/` — it just appends and
returns them. `state/` depends on nothing in this project either — it's pure data
describing a *result*, never how that result was computed. `projection/` is where
`event/` and `state/` meet: it's the only code in the module that knows how to turn one
into the other, and it's a pure function — no I/O, no dependency on `store/` at all
(you can unit-test it with nothing but a hand-built `List<PipelineRunEvent>`).
`snapshot/` depends on `state/`, `event/`, and `projection/`, reusing the fold instead
of duplicating it. `Main`, `PipelineRunSimulator`, `Auditor`, and `Dashboard` are the
only classes allowed to depend on everything at once — they're the composition root and
the three case-study actors built on top of the pattern.

## The case study, one more time

You're building the **Pipeline Run Audit Trail**: as a PipelineForge pipeline run
progresses, every fact about it — it started, a stage started, a stage failed, that
stage was retried, it eventually succeeded, the whole run finished — is appended as an
immutable event to an `EventStore`. Nobody ever stores "the current status of run X"
anywhere; instead, whoever wants to know that (the **Dashboard**) computes it by
replaying every event for that run, in order, through a fold function. The **Auditor**
does the same replay, but stops at different points along the way, to answer "what did
we know after event N" for a compliance review — a question a mutable "current status"
row could never answer, because it only ever remembers its latest value.

As you build this, keep a mental note of a question you'll be asked to answer
concretely by the final milestone: if the whole PipelineForge process crashed and
restarted right now, losing every `PipelineRunState` object that had ever been computed
in memory, what would the Dashboard's next answer look like? (Spoiler, and the whole
point of the pattern: exactly the same as before the crash — because it was never
storing that answer, only recomputing it from the log.)

## Checkpoint

- [ ] `mvn -f patterns/08-event-sourcing/pom.xml clean verify` passes.
- [ ] You can explain, in one sentence, why `PipelineRunProjector` (milestone 3) is not
      allowed to depend on `EventStore`.

Next: [`01-events.md`](01-events.md).
