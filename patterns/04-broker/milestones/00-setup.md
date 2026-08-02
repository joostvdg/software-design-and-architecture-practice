# Milestone 0 — Setup

## Goal

Confirm the module builds, understand the target package layout, and get oriented in
the case study before writing any pattern code.

## Confirm the build is green

From the repo root:

```bash
mvn -f patterns/04-broker/pom.xml clean verify
```

This should pass — right now the module only contains a placeholder test
(`SmokeTest`) and an empty `package-info.java`. That placeholder test is there so you
always have a green build to come back to; delete it once `InMemoryBroker` (milestone
2) has its own tests.

## Target layout

By the end of milestone 4 you'll have:

```
src/main/java/com/isaqb/practice/broker/
  Main.java                          # composition root + demo entry point
  Broker.java                        # the broker contract: subscribe + publish
  Subscriber.java                    # the subscriber contract
  InMemoryBroker.java                # the broker implementation - this is the pattern's core
  PipelineRunner.java                # the publisher
  event/
    PipelineEvent.java                # sealed interface, common to all event kinds
    RunStarted.java
    StageCompleted.java
    RunFinished.java
  subscriber/
    NotificationService.java
    AuditLogger.java
```

Notice the dependency shape as you build this: `event/` depends on nothing else in this
project — it's pure data. `Broker` and `Subscriber` depend only on `event/`. Each
class under `subscriber/` depends on `Broker`/`Subscriber`/`event/` but never on each
other, and never on `PipelineRunner`. `PipelineRunner` depends only on `Broker` — it
never imports anything under `subscriber/`. `Main` is the only class allowed to import
everything at once, because wiring concrete publishers and subscribers together is
exactly what a composition root is for.

## The case study, one more time

You're building the **Pipeline Event Broker**: as a PipelineForge pipeline run
progresses, the **Pipeline Runner** publishes one event per lifecycle step —
`RunStarted` when it begins, one `StageCompleted` per stage, `RunFinished` when it's
done. Two independent subscribers react: the **Notification Service** messages whoever
triggered the run once it finishes, and the **Audit Logger** records every event for a
compliance trail. Neither subscriber knows the other exists, and the Pipeline Runner
doesn't know either of them exists — it only ever talks to the `Broker`.

That's the whole point of the pattern: try, as you build this, to keep a mental count
of how many classes would need to change if a platform engineer added a third
subscriber (say, a Metrics Collector) tomorrow. By milestone 4 the answer should be
"one: `Main`" — and that's the evidence you're asked to point at in the final
milestone.

## Checkpoint

- [ ] `mvn -f patterns/04-broker/pom.xml clean verify` passes.
- [ ] You can explain, in one sentence, why `PipelineRunner` is not allowed to import
      anything from the `subscriber/` package.

Next: [`01-events.md`](01-events.md).
