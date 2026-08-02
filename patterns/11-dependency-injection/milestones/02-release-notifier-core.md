# Milestone 2 — The ReleaseNotifier core

## Goal

Build `ReleaseNotifier`: the consumer that receives its `NotificationChannel`s via
constructor injection and never constructs one itself. The core exercise here is
resilience: one channel throwing must not prevent the others from being tried.

## Step 1 — the result type (copy-paste)

`src/main/java/com/isaqb/practice/di/NotificationSummary.java`:

```java
package com.isaqb.practice.di;

import java.util.List;

/** The outcome of notifying every channel: which succeeded, which failed and why. */
public record NotificationSummary(List<String> succeededChannels, List<String> failedChannels) {

  public NotificationSummary {
    succeededChannels = List.copyOf(succeededChannels);
    failedChannels = List.copyOf(failedChannels);
  }

  public boolean allSucceeded() {
    return failedChannels.isEmpty();
  }
}
```

## Step 2 — `ReleaseNotifier` (write the core logic yourself)

Create `src/main/java/com/isaqb/practice/di/ReleaseNotifier.java`:

```java
package com.isaqb.practice.di;

import java.util.List;

/**
 * Notifies every injected channel about a release. Depends only on
 * {@link NotificationChannel} — never constructs one, never imports a concrete
 * channel type. Channels are supplied by whoever constructs this (the composition
 * root — see milestone 4).
 */
public class ReleaseNotifier {

  private final List<NotificationChannel> channels;

  public ReleaseNotifier(List<NotificationChannel> channels) {
    this.channels = List.copyOf(channels);
  }

  /**
   * Calls {@code send} on every channel. A channel throwing {@link ChannelSendException}
   * must not prevent the remaining channels from being tried — collect successes and
   * failures into the returned summary instead of letting the first failure abort the
   * rest.
   */
  public NotificationSummary notify(ReleaseNotification notification) {
    // TODO: iterate this.channels; for each, try channel.send(notification).
    // On success, record channel.name() in succeededChannels.
    // On ChannelSendException, record channel.name() in failedChannels and continue
    // to the next channel — do not rethrow.
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

Think about *why* this resilience rule matters here specifically: if `SlackChannel`
being down silently prevented `AuditLogChannel` (a compliance-relevant channel) from
ever running, that's a worse failure than Slack alone being unreachable.

## Step 3 — tests (copy-paste, must pass once step 2 is done)

`src/test/java/com/isaqb/practice/di/ReleaseNotifierTest.java`:

```java
package com.isaqb.practice.di;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ReleaseNotifierTest {

  private final ReleaseNotification release = new ReleaseNotification("1.2.3", "bugfix release");

  @Test
  void allChannelsReceiveTheNotificationWhenNoneFail() {
    var a = new RecordingChannel("a", false);
    var b = new RecordingChannel("b", false);
    var notifier = new ReleaseNotifier(List.of(a, b));

    var summary = notifier.notify(release);

    assertTrue(summary.allSucceeded());
    assertEquals(1, a.received().size());
    assertEquals(1, b.received().size());
  }

  @Test
  void oneFailingChannelDoesNotPreventOthersFromRunning() {
    var failing = new RecordingChannel("failing", true);
    var healthy = new RecordingChannel("healthy", false);
    var notifier = new ReleaseNotifier(List.of(failing, healthy));

    var summary = notifier.notify(release);

    assertEquals(List.of("failing"), summary.failedChannels());
    assertEquals(List.of("healthy"), summary.succeededChannels());
    assertEquals(1, healthy.received().size());
  }
}
```

## Checkpoint

```bash
mvn -f patterns/11-dependency-injection/pom.xml clean verify
```

Both `ReleaseNotifierTest` cases pass. Grep `ReleaseNotifier.java` for `new ` — it
should find nothing that constructs a `NotificationChannel` implementation; every
channel it uses came in through the constructor.

Next: [`03-concrete-channels.md`](03-concrete-channels.md).
