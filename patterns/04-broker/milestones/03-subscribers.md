# Milestone 3 — Subscribers

## Goal

Write the two subscribers from the case study: the **Notification Service**, which
only cares about a run finishing, and the **Audit Logger**, which cares about every
event. Neither imports the other, and neither imports `PipelineRunner` - they only
depend on `Subscriber` and `event/`. That's the decoupling from section 1 of the
README made concrete: you could delete either class right now and the other, and the
broker, and `PipelineRunner`, would not need a single line changed.

## Step 1 — Notification Service (copy-paste)

`src/main/java/com/isaqb/practice/broker/subscriber/NotificationService.java`:

```java
package com.isaqb.practice.broker.subscriber;

import com.isaqb.practice.broker.Subscriber;
import com.isaqb.practice.broker.event.PipelineEvent;
import com.isaqb.practice.broker.event.RunFinished;
import java.util.ArrayList;
import java.util.List;

/**
 * Notifies whoever triggered a pipeline run once it finishes. In a real platform this
 * would send a Slack DM or an email; here it records the message that would have been
 * sent, so tests (and the demo Main) can inspect it. NotificationService has never
 * heard of AuditLogger, PipelineRunner, or InMemoryBroker's internals - it only
 * implements Subscriber.
 */
public class NotificationService implements Subscriber {

  private final List<String> sentMessages = new ArrayList<>();

  @Override
  public void onEvent(PipelineEvent event) {
    if (event instanceof RunFinished finished) {
      String outcome = finished.success() ? "succeeded" : "failed";
      sentMessages.add(
          "Notifying trigger user: run " + finished.runId() + " " + outcome + ".");
    }
    // Every other event kind is silently ignored - NotificationService is only ever
    // registered for RunFinished.class anyway (see Main, milestone 4), but ignoring
    // types it wasn't expecting also makes this class safe to register for more event
    // types later without a crash.
  }

  public List<String> sentMessages() {
    return List.copyOf(sentMessages);
  }
}
```

`src/test/java/com/isaqb/practice/broker/subscriber/NotificationServiceTest.java`:

```java
package com.isaqb.practice.broker.subscriber;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.isaqb.practice.broker.event.RunFinished;
import com.isaqb.practice.broker.event.RunStarted;
import org.junit.jupiter.api.Test;

class NotificationServiceTest {

  private final NotificationService service = new NotificationService();

  @Test
  void recordsAMessageWhenARunFinishes() {
    service.onEvent(new RunFinished("run-1", true));

    assertEquals(1, service.sentMessages().size());
    assertTrue(service.sentMessages().get(0).contains("run-1"));
  }

  @Test
  void ignoresEventTypesItDoesNotCareAbout() {
    service.onEvent(new RunStarted("run-1", "nightly-build"));

    assertTrue(service.sentMessages().isEmpty());
  }
}
```

## Step 2 — Audit Logger (write this yourself)

`src/main/java/com/isaqb/practice/broker/subscriber/AuditLogger.java`:

```java
package com.isaqb.practice.broker.subscriber;

import com.isaqb.practice.broker.Subscriber;
import com.isaqb.practice.broker.event.PipelineEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Records every pipeline event for a compliance trail. Unlike NotificationService,
 * AuditLogger genuinely needs to know about every PipelineEvent kind - that's fine,
 * it's a property of what an audit trail is for, not a leak of the broker's design.
 * The broker itself still never knows AuditLogger exists.
 */
public class AuditLogger implements Subscriber {

  private final List<String> entries = new ArrayList<>();

  @Override
  public void onEvent(PipelineEvent event) {
    // TODO: append exactly one entry to `entries` for every event, whatever its kind.
    // Use an exhaustive switch over the sealed PipelineEvent to produce a distinct,
    // human-readable line per event kind, e.g.:
    //
    //   String line = switch (event) {
    //     case RunStarted e -> "..." ;
    //     case StageCompleted e -> "...";
    //     case RunFinished e -> "...";
    //   };
    //
    // Every line must include the event's runId() - the tests below check for it.
    // Because PipelineEvent is sealed, this switch needs no default branch: if a
    // fourth event kind is ever added, this method fails to compile until you handle
    // it, instead of silently ignoring it at runtime.
    throw new UnsupportedOperationException("not implemented yet");
  }

  public List<String> entries() {
    return List.copyOf(entries);
  }
}
```

`src/test/java/com/isaqb/practice/broker/subscriber/AuditLoggerTest.java`:

```java
package com.isaqb.practice.broker.subscriber;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.isaqb.practice.broker.event.RunFinished;
import com.isaqb.practice.broker.event.RunStarted;
import com.isaqb.practice.broker.event.StageCompleted;
import org.junit.jupiter.api.Test;

class AuditLoggerTest {

  private final AuditLogger logger = new AuditLogger();

  @Test
  void recordsOneEntryPerEvent() {
    logger.onEvent(new RunStarted("run-1", "nightly-build"));
    logger.onEvent(new StageCompleted("run-1", "compile", true));
    logger.onEvent(new RunFinished("run-1", true));

    assertEquals(3, logger.entries().size());
  }

  @Test
  void everyEntryMentionsItsRunId() {
    logger.onEvent(new RunStarted("run-42", "nightly-build"));

    assertTrue(logger.entries().get(0).contains("run-42"));
  }

  @Test
  void distinguishesEventKindsInTheEntryText() {
    logger.onEvent(new RunStarted("run-1", "nightly-build"));
    logger.onEvent(new RunFinished("run-1", false));

    String startedEntry = logger.entries().get(0);
    String finishedEntry = logger.entries().get(1);

    assertTrue(!startedEntry.equals(finishedEntry));
  }
}
```

## Checkpoint

```bash
mvn -f patterns/04-broker/pom.xml clean verify
```

All `NotificationServiceTest` and `AuditLoggerTest` cases pass. Check: does either
class under `subscriber/` import the other, or import `PipelineRunner`? It shouldn't -
if it does, something has gone wrong with the decoupling this pattern is meant to give
you.

Next: [`04-main-and-demo.md`](04-main-and-demo.md).
