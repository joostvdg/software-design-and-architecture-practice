# Milestone 2 — Event store

## Goal

Build the append-only log itself: the `EventStore` contract and `InMemoryEventStore`.
An event store deliberately supports exactly two operations — append a fact, read back
every fact recorded for a run, in order — and nothing else. There is no `update`, no
`delete`, no "replace event 3." That absence is the point: once a fact is recorded, it
is a permanent part of the history, which is what makes the log trustworthy as an audit
trail in the first place.

Delete `src/test/java/com/isaqb/practice/eventsourcing/SmokeTest.java` now — the tests
you add in this milestone replace it as your "is the build green" signal.

## Step 1 — the contract (copy-paste)

`src/main/java/com/isaqb/practice/eventsourcing/store/EventStore.java`:

```java
package com.isaqb.practice.eventsourcing.store;

import com.isaqb.practice.eventsourcing.event.PipelineRunEvent;
import java.util.List;

/**
 * An append-only log: the only two operations are "add a fact" and "read every fact
 * recorded so far for a run", in the order they were recorded. There is deliberately no
 * update or delete - once an event is appended, it stays. An in-memory Map/List is
 * enough for this exercise; a production event store would be a durable, append-only
 * table or log (a Kafka topic, EventStoreDB, an append-only SQL table with no UPDATE
 * grants), but the contract - append, read-in-order, never mutate - is identical.
 */
public interface EventStore {

  /** Records {@code event} as the next fact for its run. Never overwrites or removes
   * anything already recorded. */
  void append(PipelineRunEvent event);

  /** Every event recorded for {@code runId} so far, in the order it was appended.
   * Returns an empty list (never null) if nothing has been recorded for this runId. */
  List<PipelineRunEvent> eventsFor(String runId);
}
```

## Step 2 — the implementation (write this yourself)

`src/main/java/com/isaqb/practice/eventsourcing/store/InMemoryEventStore.java`:

```java
package com.isaqb.practice.eventsourcing.store;

import com.isaqb.practice.eventsourcing.event.PipelineRunEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An in-memory EventStore: every run's events are kept in a List, keyed by runId, for
 * the lifetime of this object. Not durable across process restarts - that's a real
 * limitation for production use, but irrelevant to what this exercise is teaching: the
 * append-only, replay-to-derive-state discipline works identically whether the log
 * lives in a HashMap or a distributed log.
 */
public class InMemoryEventStore implements EventStore {

  private final Map<String, List<PipelineRunEvent>> eventsByRunId = new HashMap<>();

  @Override
  public void append(PipelineRunEvent event) {
    // TODO: append `event` to the list kept for event.runId(), preserving append order.
    // Create the list on first use for a runId (Map.computeIfAbsent with a fresh
    // ArrayList<>() as the default is enough). This method only ever grows a run's log -
    // never remove or replace an existing entry.
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public List<PipelineRunEvent> eventsFor(String runId) {
    // TODO: return every event appended for `runId`, in append order, or an empty list
    // if nothing has ever been appended for this runId (never return null).
    // Important: the returned list must be a *defensive, unmodifiable copy* - callers
    // must never be able to mutate this store's internal log by mutating what
    // eventsFor() handed them. List.copyOf(...) on whatever you looked up (or List.of()
    // for an unknown runId) gives you that for free.
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

## Step 3 — tests (copy-paste, must pass once step 2 is done)

`src/test/java/com/isaqb/practice/eventsourcing/store/InMemoryEventStoreTest.java`:

```java
package com.isaqb.practice.eventsourcing.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.isaqb.practice.eventsourcing.event.PipelineRunEvent;
import com.isaqb.practice.eventsourcing.event.RunFinished;
import com.isaqb.practice.eventsourcing.event.RunStarted;
import com.isaqb.practice.eventsourcing.event.StageCompleted;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryEventStoreTest {

  private final EventStore store = new InMemoryEventStore();

  @Test
  void returnsAppendedEventsInAppendOrder() {
    store.append(new RunStarted("run-1", "nightly-build", Instant.parse("2026-01-01T00:00:00Z")));
    store.append(new StageCompleted("run-1", "compile", Instant.parse("2026-01-01T00:01:00Z")));
    store.append(new RunFinished("run-1", true, Instant.parse("2026-01-01T00:02:00Z")));

    List<PipelineRunEvent> events = store.eventsFor("run-1");

    assertEquals(3, events.size());
    assertTrue(events.get(0) instanceof RunStarted);
    assertTrue(events.get(1) instanceof StageCompleted);
    assertTrue(events.get(2) instanceof RunFinished);
  }

  @Test
  void unknownRunIdReturnsEmptyList() {
    assertEquals(List.of(), store.eventsFor("no-such-run"));
  }

  @Test
  void keepsDifferentRunsSeparate() {
    store.append(new RunStarted("run-1", "nightly-build", Instant.now()));
    store.append(new RunStarted("run-2", "release-build", Instant.now()));

    assertEquals(1, store.eventsFor("run-1").size());
    assertEquals(1, store.eventsFor("run-2").size());
  }

  @Test
  void eventsForResultCannotBeMutatedByTheCaller() {
    store.append(new RunStarted("run-1", "nightly-build", Instant.now()));

    List<PipelineRunEvent> events = store.eventsFor("run-1");

    assertThrows(
        UnsupportedOperationException.class,
        () -> events.add(new RunFinished("run-1", true, Instant.now())));
  }

  @Test
  void appendingLaterDoesNotAffectAPreviouslyReturnedList() {
    store.append(new RunStarted("run-1", "nightly-build", Instant.now()));
    List<PipelineRunEvent> firstRead = store.eventsFor("run-1");

    store.append(new RunFinished("run-1", true, Instant.now()));

    assertEquals(1, firstRead.size());
    assertEquals(2, store.eventsFor("run-1").size());
  }
}
```

That last test is worth pausing on: it's checking that `eventsFor` handed you a
snapshot, not a live view into the store's internals. If your implementation returns
the same list object the store mutates internally, this test will fail even though the
"happy path" tests pass — a subtle but important part of what "append-only, safely
readable" means in practice.

## Checkpoint

```bash
mvn -f patterns/08-event-sourcing/pom.xml clean verify
```

All five `InMemoryEventStoreTest` cases pass. Confirm nothing under `store/` imports
anything from `state/` or `projection/` — a store only knows about raw events, never
about derived state.

Next: [`03-projection.md`](03-projection.md).
