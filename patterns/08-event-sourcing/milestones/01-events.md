# Milestone 1 — Events

## Goal

Define the vocabulary the whole exercise is built on: the five kinds of fact a
pipeline run can produce, and the common type that lets the event store (milestone 2)
and the projector (milestone 3) handle "any pipeline event" generically. This
milestone is entirely mechanical — events are plain, immutable data, and the
interesting logic lives in what happens *after* an event is appended, not in the
events themselves — so everything here is copy-paste.

Every event carries a `runId` (which run it belongs to) and an `occurredAt` timestamp
(when it happened) — that timestamp is what lets the Auditor (milestone 6) later
answer "when did this happen," not just "did it happen."

## Step 1 — the common event type (copy-paste)

`src/main/java/com/isaqb/practice/eventsourcing/event/PipelineRunEvent.java`:

```java
package com.isaqb.practice.eventsourcing.event;

import java.time.Instant;

/**
 * Common supertype for every fact that can be recorded about a pipeline run. Sealed so
 * that any exhaustive switch over a PipelineRunEvent (see PipelineRunProjector,
 * milestone 3) is checked by the compiler - if a sixth event kind is ever added, every
 * such switch fails to compile until it's handled, instead of silently ignoring it at
 * runtime.
 */
public sealed interface PipelineRunEvent
    permits RunStarted, StageStarted, StageCompleted, StageFailed, RunFinished {

  /** The pipeline run this event belongs to. Every event kind carries this. */
  String runId();

  /** When this fact was recorded. Part of what makes the log an audit trail, not just
   * a sequence - "when" matters as much as "what" and "in what order". */
  Instant occurredAt();
}
```

## Step 2 — the five event kinds (copy-paste)

`src/main/java/com/isaqb/practice/eventsourcing/event/RunStarted.java`:

```java
package com.isaqb.practice.eventsourcing.event;

import java.time.Instant;

/** Published once, when a pipeline run begins. */
public record RunStarted(String runId, String pipelineName, Instant occurredAt)
    implements PipelineRunEvent {}
```

`src/main/java/com/isaqb/practice/eventsourcing/event/StageStarted.java`:

```java
package com.isaqb.practice.eventsourcing.event;

import java.time.Instant;

/**
 * Published every time a stage starts - including retries. A stage that fails and is
 * retried produces a second StageStarted with the same stageName; that's how a retry
 * is represented in this exercise, not as a separate event kind.
 */
public record StageStarted(String runId, String stageName, Instant occurredAt)
    implements PipelineRunEvent {}
```

`src/main/java/com/isaqb/practice/eventsourcing/event/StageCompleted.java`:

```java
package com.isaqb.practice.eventsourcing.event;

import java.time.Instant;

/** Published once a stage finishes successfully. */
public record StageCompleted(String runId, String stageName, Instant occurredAt)
    implements PipelineRunEvent {}
```

`src/main/java/com/isaqb/practice/eventsourcing/event/StageFailed.java`:

```java
package com.isaqb.practice.eventsourcing.event;

import java.time.Instant;

/** Published once a stage finishes unsuccessfully. May be followed by another
 * StageStarted for the same stageName if the stage is retried. */
public record StageFailed(String runId, String stageName, String reason, Instant occurredAt)
    implements PipelineRunEvent {}
```

`src/main/java/com/isaqb/practice/eventsourcing/event/RunFinished.java`:

```java
package com.isaqb.practice.eventsourcing.event;

import java.time.Instant;

/** Published once, after every stage has reached a final outcome. */
public record RunFinished(String runId, boolean success, Instant occurredAt)
    implements PipelineRunEvent {}
```

Notice each record automatically implements `runId()` and `occurredAt()` because it
declares components with those names — no manual wiring needed.

## Checkpoint

```bash
mvn -f patterns/08-event-sourcing/pom.xml clean verify
```

Still green (`SmokeTest` is still the only test — that's expected, these are just data
types with nothing to assert on yet). Confirm the module compiles with the six new
files and nothing under `event/` imports anything else from this project.

Next: [`02-event-store.md`](02-event-store.md).
