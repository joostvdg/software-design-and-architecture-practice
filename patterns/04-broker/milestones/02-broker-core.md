# Milestone 2 — Broker core

## Goal

Build the pattern itself: the `Subscriber` and `Broker` contracts, and
`InMemoryBroker`, the class that keeps track of who is subscribed to what and forwards
published events to the right subscribers. This is where the whole exercise's payoff
lives — get this right and every subscriber you add later (milestone 3) plugs in
without `PipelineRunner` ever changing.

Delete `src/test/java/com/isaqb/practice/broker/SmokeTest.java` now — the test you add
in this milestone replaces it as your "is the build green" signal.

## Step 1 — the contracts (copy-paste)

`src/main/java/com/isaqb/practice/broker/Subscriber.java`:

```java
package com.isaqb.practice.broker;

import com.isaqb.practice.broker.event.PipelineEvent;

/**
 * Something that wants to react to pipeline events. A Subscriber never knows who
 * published the event it receives, and never knows what other subscribers exist -
 * it only implements this one method.
 */
@FunctionalInterface
public interface Subscriber {

  void onEvent(PipelineEvent event);
}
```

`src/main/java/com/isaqb/practice/broker/Broker.java`:

```java
package com.isaqb.practice.broker;

import com.isaqb.practice.broker.event.PipelineEvent;

/**
 * The intermediary: publishers publish events without knowing who (if anyone) is
 * listening; subscribers register interest in an event type without knowing who (if
 * anyone) will ever publish one. Neither side ever references the other directly -
 * they only ever reference Broker.
 */
public interface Broker {

  /**
   * Registers {@code subscriber} to be notified of every future event whose runtime
   * type is exactly {@code eventType}. Multiple subscribers may register for the same
   * event type; all of them must be notified, in the order they subscribed.
   */
  void subscribe(Class<? extends PipelineEvent> eventType, Subscriber subscriber);

  /**
   * Notifies every subscriber currently registered for {@code event}'s runtime type.
   * If nobody is subscribed, this is a silent no-op - not an error. Callers of publish
   * never find out who (if anyone) received the event.
   */
  void publish(PipelineEvent event);
}
```

## Step 2 — the implementation (write the dispatch logic yourself)

`src/main/java/com/isaqb/practice/broker/InMemoryBroker.java`:

```java
package com.isaqb.practice.broker;

import com.isaqb.practice.broker.event.PipelineEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple, in-process Broker: subscriptions are held in memory, and publish()
 * dispatches synchronously, on the calling thread, before returning. This decouples
 * *location* (publishers and subscribers never reference each other) but not *time*
 * (there is no queue - an event published to nobody is simply lost, and a subscriber
 * that isn't registered yet will never see events published before it subscribed).
 */
public class InMemoryBroker implements Broker {

  private final Map<Class<? extends PipelineEvent>, List<Subscriber>> subscribersByType =
      new HashMap<>();

  @Override
  public void subscribe(Class<? extends PipelineEvent> eventType, Subscriber subscriber) {
    // TODO: register `subscriber` under `eventType` in `subscribersByType`. Multiple
    // subscribers for the same eventType must all be kept (don't overwrite), and must
    // stay in the order they were registered. Hint: Map.computeIfAbsent with a fresh
    // ArrayList<>() as the default is enough - you don't need anything more elaborate.
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public void publish(PipelineEvent event) {
    // TODO:
    //  1. Look up the subscribers registered for event.getClass() (there may be none -
    //     that's fine, treat it as an empty list, not an error).
    //  2. Call onEvent(event) on each one, in registration order.
    //  3. If a subscriber's onEvent throws, catch the exception right there (a plain
    //     `catch (RuntimeException e)` is fine - print something to System.err with
    //     the subscriber and the exception) and keep going with the remaining
    //     subscribers. One broken subscriber must never stop other subscribers from
    //     being notified, and must never propagate back out of publish() to whatever
    //     called it - the Pipeline Runner (milestone 4) has no idea subscribers exist
    //     at all, and a subscriber's bug is not the Pipeline Runner's problem.
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

This is the pattern's core: everything else in this exercise (the events, the two
subscribers, the demo `Main`) exists to exercise this class. The failure-isolation
requirement in step 2.3 is not incidental - "failure modes of the broker itself" is
explicitly called out as an exam-relevant concern for this pattern, and this is where
you implement the concrete version of it: a broker that lets one bad subscriber take
down every other subscriber (or the publisher) has a design flaw, not just a bug.

## Step 3 — tests (copy-paste, must pass once step 2 is done)

`src/test/java/com/isaqb/practice/broker/InMemoryBrokerTest.java`:

```java
package com.isaqb.practice.broker;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.isaqb.practice.broker.event.PipelineEvent;
import com.isaqb.practice.broker.event.RunFinished;
import com.isaqb.practice.broker.event.RunStarted;
import com.isaqb.practice.broker.event.StageCompleted;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryBrokerTest {

  @Test
  void deliversEventOnlyToSubscribersOfMatchingType() {
    Broker broker = new InMemoryBroker();
    List<PipelineEvent> runStartedReceived = new ArrayList<>();
    List<PipelineEvent> stageCompletedReceived = new ArrayList<>();
    broker.subscribe(RunStarted.class, runStartedReceived::add);
    broker.subscribe(StageCompleted.class, stageCompletedReceived::add);

    broker.publish(new RunStarted("run-1", "nightly-build"));

    assertEquals(1, runStartedReceived.size());
    assertTrue(stageCompletedReceived.isEmpty());
  }

  @Test
  void supportsMultipleSubscribersForTheSameEventType() {
    Broker broker = new InMemoryBroker();
    List<String> received = new ArrayList<>();
    broker.subscribe(RunFinished.class, event -> received.add("first"));
    broker.subscribe(RunFinished.class, event -> received.add("second"));

    broker.publish(new RunFinished("run-1", true));

    assertEquals(List.of("first", "second"), received);
  }

  @Test
  void publishingWithNoSubscribersIsASilentNoOp() {
    Broker broker = new InMemoryBroker();

    assertDoesNotThrow(() -> broker.publish(new RunFinished("run-1", true)));
  }

  @Test
  void aSubscriberThrowingDoesNotStopOtherSubscribersOrPropagate() {
    Broker broker = new InMemoryBroker();
    List<String> received = new ArrayList<>();
    broker.subscribe(RunStarted.class, event -> { throw new RuntimeException("boom"); });
    broker.subscribe(RunStarted.class, event -> received.add(event.runId()));

    assertDoesNotThrow(() -> broker.publish(new RunStarted("run-1", "nightly-build")));

    assertEquals(List.of("run-1"), received);
  }
}
```

That last test is the one worth pausing on: it's asserting the broker's own
failure-mode behavior, not the pattern's happy path. Run it before you're confident
your `publish` implementation actually catches the exception - it's easy to write a
version that passes the first three tests and still fails this one.

## Checkpoint

```bash
mvn -f patterns/04-broker/pom.xml clean verify
```

All four `InMemoryBrokerTest` cases pass, deterministically, on every run (dispatch is
synchronous, so there's no timing to get flaky).

Next: [`03-subscribers.md`](03-subscribers.md).
