# Milestone 4 — Main and demo

## Goal

Write `PipelineRunner` (the publisher) and `Main` (the composition root), wire
everything from the previous three milestones together, and watch the whole system run
end to end. Both classes are copy-paste this time - there's no new pattern logic here,
only wiring, and wiring is exactly what a composition root is for.

## Step 1 — the publisher (copy-paste)

`src/main/java/com/isaqb/practice/broker/PipelineRunner.java`:

```java
package com.isaqb.practice.broker;

import com.isaqb.practice.broker.event.RunFinished;
import com.isaqb.practice.broker.event.RunStarted;
import com.isaqb.practice.broker.event.StageCompleted;
import java.util.List;

/**
 * The publisher in this case study. Runs a pipeline's stages in order and publishes
 * one event per lifecycle step. PipelineRunner depends only on Broker - it has never
 * heard of NotificationService or AuditLogger, and won't need to change no matter how
 * many more subscribers a platform engineer adds later.
 */
public class PipelineRunner {

  private final Broker broker;

  public PipelineRunner(Broker broker) {
    this.broker = broker;
  }

  /** Runs every stage in {@code stageNames}, in order, all succeeding, and publishes
   * the corresponding RunStarted / StageCompleted / RunFinished events along the way.
   * This exercise keeps "running a stage" trivial (it always succeeds) because the
   * point of this case study is the broker, not a real build executor. */
  public void run(String runId, String pipelineName, List<String> stageNames) {
    broker.publish(new RunStarted(runId, pipelineName));

    for (String stageName : stageNames) {
      boolean success = true;
      broker.publish(new StageCompleted(runId, stageName, success));
    }

    broker.publish(new RunFinished(runId, true));
  }
}
```

## Step 2 — the composition root (copy-paste)

`src/main/java/com/isaqb/practice/broker/Main.java`:

```java
package com.isaqb.practice.broker;

import com.isaqb.practice.broker.event.RunFinished;
import com.isaqb.practice.broker.event.RunStarted;
import com.isaqb.practice.broker.event.StageCompleted;
import com.isaqb.practice.broker.subscriber.AuditLogger;
import com.isaqb.practice.broker.subscriber.NotificationService;
import java.util.List;

/**
 * Composition root and demo entry point: the only class in this module allowed to
 * know about every layer at once. Wires an InMemoryBroker, subscribes
 * NotificationService and AuditLogger to the events each cares about, then runs a
 * demo pipeline through PipelineRunner and prints what each subscriber observed.
 *
 * To add a third subscriber (say, a Metrics Collector), this is the only file that
 * changes: write the class under subscriber/, instantiate it here, call
 * broker.subscribe(...) for whichever event types it cares about. PipelineRunner,
 * NotificationService, and AuditLogger stay untouched.
 */
public final class Main {

  private Main() {}

  public static void main(String[] args) {
    Broker broker = new InMemoryBroker();

    var notificationService = new NotificationService();
    broker.subscribe(RunFinished.class, notificationService);

    var auditLogger = new AuditLogger();
    broker.subscribe(RunStarted.class, auditLogger);
    broker.subscribe(StageCompleted.class, auditLogger);
    broker.subscribe(RunFinished.class, auditLogger);

    var runner = new PipelineRunner(broker);
    runner.run("run-42", "nightly-build", List.of("compile", "test", "package"));

    System.out.println("--- Notification Service ---");
    notificationService.sentMessages().forEach(System.out::println);

    System.out.println("--- Audit Log ---");
    auditLogger.entries().forEach(System.out::println);
  }
}
```

## Step 3 — integration test (copy-paste, must pass once milestones 2 and 3 are done)

`src/test/java/com/isaqb/practice/broker/PipelineRunnerTest.java`:

```java
package com.isaqb.practice.broker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.isaqb.practice.broker.event.RunFinished;
import com.isaqb.practice.broker.event.RunStarted;
import com.isaqb.practice.broker.event.StageCompleted;
import com.isaqb.practice.broker.subscriber.AuditLogger;
import com.isaqb.practice.broker.subscriber.NotificationService;
import java.util.List;
import org.junit.jupiter.api.Test;

class PipelineRunnerTest {

  @Test
  void notifiesEachSubscriberAccordingToItsOwnInterest() {
    Broker broker = new InMemoryBroker();
    var notificationService = new NotificationService();
    var auditLogger = new AuditLogger();
    broker.subscribe(RunFinished.class, notificationService);
    broker.subscribe(RunStarted.class, auditLogger);
    broker.subscribe(StageCompleted.class, auditLogger);
    broker.subscribe(RunFinished.class, auditLogger);

    new PipelineRunner(broker).run("run-1", "nightly-build", List.of("compile", "test"));

    // NotificationService only cares about RunFinished - exactly one message.
    assertEquals(1, notificationService.sentMessages().size());
    assertTrue(notificationService.sentMessages().get(0).contains("run-1"));

    // AuditLogger cares about everything: 1 RunStarted + 2 StageCompleted + 1 RunFinished.
    assertEquals(4, auditLogger.entries().size());
  }

  @Test
  void aSubscriberAddedForOneEventTypeNeverSeesOtherTypes() {
    Broker broker = new InMemoryBroker();
    var notificationService = new NotificationService();
    broker.subscribe(RunFinished.class, notificationService);

    new PipelineRunner(broker).run("run-2", "nightly-build", List.of("compile"));

    // RunStarted and StageCompleted were published too, but notificationService was
    // never subscribed to them - only the one RunFinished message should appear.
    assertEquals(1, notificationService.sentMessages().size());
  }
}
```

## Step 4 — try it for real

```bash
mvn -f patterns/04-broker/pom.xml clean package
java -jar patterns/04-broker/target/broker-1.0.0-SNAPSHOT.jar
```

You should see the Notification Service's one message for `run-42` finishing, followed
by the Audit Logger's four entries covering the whole run lifecycle.

## Checkpoint

- [ ] `mvn -f patterns/04-broker/pom.xml clean verify` passes, every test green.
- [ ] Running the jar prints both sections as described in step 4.
- [ ] You can point to the exact lines in `Main` you'd add for a third subscriber, and
      confirm `PipelineRunner`, `NotificationService`, and `AuditLogger` would not need
      a single line changed.

Next: [`05-build-and-release.md`](05-build-and-release.md).
