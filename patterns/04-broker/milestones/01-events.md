# Milestone 1 — Events

## Goal

Define the vocabulary the whole exercise is built on: the three kinds of event a
pipeline run can produce, and the common type that lets the broker (milestone 2) and
the Audit Logger (milestone 3) handle "any pipeline event" generically. This milestone
is entirely mechanical — events are plain data, and the interesting logic lives in how
the broker dispatches them, not in the events themselves — so everything here is
copy-paste.

## Step 1 — the common event type (copy-paste)

`src/main/java/com/isaqb/practice/broker/event/PipelineEvent.java`:

```java
package com.isaqb.practice.broker.event;

/**
 * Common supertype for everything the Pipeline Runner can publish. Sealed so that
 * every switch over a PipelineEvent (see AuditLogger in milestone 3) can be exhaustive
 * without a default branch - the compiler tells you if a new event kind is added
 * somewhere that doesn't yet handle it.
 */
public sealed interface PipelineEvent permits RunStarted, StageCompleted, RunFinished {

  /** The pipeline run this event belongs to. Every event kind carries this. */
  String runId();
}
```

## Step 2 — the three event kinds (copy-paste)

`src/main/java/com/isaqb/practice/broker/event/RunStarted.java`:

```java
package com.isaqb.practice.broker.event;

/** Published once, when the Pipeline Runner begins executing a run. */
public record RunStarted(String runId, String pipelineName) implements PipelineEvent {}
```

`src/main/java/com/isaqb/practice/broker/event/StageCompleted.java`:

```java
package com.isaqb.practice.broker.event;

/** Published once per stage, after that stage finishes (successfully or not). */
public record StageCompleted(String runId, String stageName, boolean success)
    implements PipelineEvent {}
```

`src/main/java/com/isaqb/practice/broker/event/RunFinished.java`:

```java
package com.isaqb.practice.broker.event;

/** Published once, after every stage has completed. */
public record RunFinished(String runId, boolean success) implements PipelineEvent {}
```

Notice each record automatically implements `runId()` because it declares a `runId`
component — no manual wiring needed.

## Checkpoint

```bash
mvn -f patterns/04-broker/pom.xml clean verify
```

Still green (`SmokeTest` is still the only test — that's expected, these are just data
types with nothing to assert on yet). Confirm the module compiles with the three new
files and nothing under `event/` imports anything else from this project.

Next: [`02-broker-core.md`](02-broker-core.md).
