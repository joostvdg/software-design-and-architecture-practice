# Milestone 1 — The notification channel port

## Goal

Define the abstraction `ReleaseNotifier` will depend on: `NotificationChannel`, plus
the data type sent through it. This is the "port" side of the story — the interface
the core owns and depends on, which concrete channels (milestone 3) will implement.

Delete `src/test/java/com/isaqb/practice/di/SmokeTest.java` now — the tests you add in
this milestone replace it as your "is the build green" signal.

## Step 1 — the notification data (copy-paste)

`src/main/java/com/isaqb/practice/di/ReleaseNotification.java`:

```java
package com.isaqb.practice.di;

/** What gets sent to every channel when a release is published. */
public record ReleaseNotification(String releaseVersion, String summary) {}
```

## Step 2 — the port (copy-paste)

`src/main/java/com/isaqb/practice/di/NotificationChannel.java`:

```java
package com.isaqb.practice.di;

/**
 * The abstraction {@link ReleaseNotifier} depends on. Concrete implementations (Slack,
 * email, audit log — milestone 3) live in the {@code channels} sub-package and are
 * never referenced by {@link ReleaseNotifier} directly.
 */
public interface NotificationChannel {

  /** A short name for this channel, used in failure reporting (e.g. "slack"). */
  String name();

  /**
   * Sends the notification through this channel.
   *
   * @throws ChannelSendException if the send fails.
   */
  void send(ReleaseNotification notification);
}
```

## Step 3 — the exception (copy-paste)

`src/main/java/com/isaqb/practice/di/ChannelSendException.java`:

```java
package com.isaqb.practice.di;

/** Thrown by a NotificationChannel when it fails to send. */
public class ChannelSendException extends RuntimeException {

  public ChannelSendException(String channelName, String reason) {
    super(channelName + ": " + reason);
  }
}
```

## Step 4 — a test double channel (write this yourself)

Create `src/test/java/com/isaqb/practice/di/RecordingChannel.java`, a test-only
implementation of `NotificationChannel` that:

- has a `name()` you pass in via constructor.
- records every `ReleaseNotification` it receives into a `List<ReleaseNotification>`
  you can inspect after the test (expose it via a getter, e.g. `received()`).
- optionally throws `ChannelSendException` on `send` if constructed with a
  `shouldFail = true` flag — you'll need this in milestone 2 to test per-channel
  failure isolation.

This isn't a production channel — it exists purely so later milestones can test
`ReleaseNotifier` without a real Slack/email/log implementation.

## Checkpoint

```bash
mvn -f patterns/11-dependency-injection/pom.xml clean verify
```

The module still compiles (no tests exercise `RecordingChannel` yet — that happens in
milestone 2). Confirm `NotificationChannel.java` has zero imports besides
`ReleaseNotification` and `ChannelSendException` — the port itself must stay narrow.

Next: [`02-release-notifier-core.md`](02-release-notifier-core.md).
