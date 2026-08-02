# Milestone 3 — Concrete channels

## Goal

Implement the three real `NotificationChannel`s: `SlackChannel`, `EmailChannel`, and
`AuditLogChannel`. Each simulates its side effect (no real network/SMTP calls — this
is a practice exercise) by appending a formatted string to an in-memory sink you can
assert against in tests.

## Step 1 — package (copy-paste)

Create the `channels` sub-package:
`src/main/java/com/isaqb/practice/di/channels/`.

## Step 2 — `SlackChannel` (write the body yourself)

Create `src/main/java/com/isaqb/practice/di/channels/SlackChannel.java`:

```java
package com.isaqb.practice.di.channels;

import com.isaqb.practice.di.ChannelSendException;
import com.isaqb.practice.di.NotificationChannel;
import com.isaqb.practice.di.ReleaseNotification;
import java.util.ArrayList;
import java.util.List;

/** Simulates posting to a Slack channel by appending to an in-memory "sent" sink. */
public class SlackChannel implements NotificationChannel {

  private final List<String> sentMessages = new ArrayList<>();

  @Override
  public String name() {
    return "slack";
  }

  @Override
  public void send(ReleaseNotification notification) {
    // TODO: format a message like "[1.2.3] bugfix release" from notification's
    // releaseVersion() and summary(), and add it to sentMessages.
    // (No failure condition needed here — that's EmailChannel's job below.)
    throw new UnsupportedOperationException("not implemented yet");
  }

  /** Test/inspection hook — what would have been posted to Slack. */
  public List<String> sentMessages() {
    return List.copyOf(sentMessages);
  }
}
```

## Step 3 — `EmailChannel` (write the body yourself)

Create `src/main/java/com/isaqb/practice/di/channels/EmailChannel.java`, same shape
as `SlackChannel` (`name()` returns `"email"`, a `sentMessages()` inspection hook),
but this time also model a failure condition: if the notification's `summary()` is
blank, throw `new ChannelSendException("email", "summary must not be blank")` instead
of sending — email notifications in this exercise require a non-empty subject line.

## Step 4 — `AuditLogChannel` (write the body yourself)

Create `src/main/java/com/isaqb/practice/di/channels/AuditLogChannel.java`, same
shape, `name()` returns `"audit-log"`. No failure condition — an audit log append is
modeled as always succeeding in this exercise, since it's the channel most important
not to silently drop (tie this back to milestone 2's resilience rule).

## Step 5 — tests (write these yourself)

For each channel, write a test class asserting:

- a successful `send` adds exactly one entry to `sentMessages()` containing the
  release version and summary.
- `EmailChannel` specifically: `send` with a blank `summary` throws
  `ChannelSendException`, and does **not** add anything to `sentMessages()`.

## Checkpoint

```bash
mvn -f patterns/11-dependency-injection/pom.xml clean verify
```

All three channels' tests pass. Confirm none of the three channel classes import
`ReleaseNotifier` — channels don't know their consumer exists, only the
`NotificationChannel` contract they implement.

Next: [`04-composition-root.md`](04-composition-root.md).
