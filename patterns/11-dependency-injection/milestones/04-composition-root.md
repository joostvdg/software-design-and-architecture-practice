# Milestone 4 — The composition root

## Goal

Build `Main`: the one place in this module allowed to construct concrete
`NotificationChannel` implementations and inject them into `ReleaseNotifier`. Then
prove, with a test, that changing *which* channels are wired never requires changing
`ReleaseNotifier` itself — that's the payoff DI exists for.

## Step 1 — `Main` (copy-paste, then run it)

`src/main/java/com/isaqb/practice/di/Main.java`:

```java
package com.isaqb.practice.di;

import com.isaqb.practice.di.channels.AuditLogChannel;
import com.isaqb.practice.di.channels.EmailChannel;
import com.isaqb.practice.di.channels.SlackChannel;
import java.util.List;

/** Composition root: the only class allowed to construct concrete NotificationChannels. */
public final class Main {

  public static void main(String[] args) {
    var notifier = new ReleaseNotifier(List.of(
        new SlackChannel(),
        new EmailChannel(),
        new AuditLogChannel()));

    var summary = notifier.notify(new ReleaseNotification("1.2.3", "bugfix release"));

    System.out.println("succeeded: " + summary.succeededChannels());
    System.out.println("failed: " + summary.failedChannels());
  }

  private Main() {}
}
```

Run it:

```bash
mvn -f patterns/11-dependency-injection/pom.xml -q compile exec:java -Dexec.mainClass=com.isaqb.practice.di.Main
```

(If the `exec` goal isn't available in your local Maven setup, `mvn package` and
`java -cp target/classes com.isaqb.practice.di.Main` works the same way.)

## Step 2 — the proof: swap the wiring without touching the core (write this test yourself)

Write `src/test/java/com/isaqb/practice/di/CompositionRootTest.java` that:

- constructs a `ReleaseNotifier` with only `List.of(new AuditLogChannel())` (a
  "minimal" wiring — no Slack, no email) and asserts `notify(...)` still works and
  reports exactly one succeeded channel.
- constructs a *second* `ReleaseNotifier` in the same test with all three real
  channels plus your `RecordingChannel` test double from milestone 1 added in, and
  asserts it reports four succeeded channels.

The point of this test isn't the assertions themselves — it's that you write it
*without opening `ReleaseNotifier.java`*. If you find yourself needing to change that
file to make either wiring work, the DI boundary has a leak somewhere upstream.

## Checkpoint

- [ ] `mvn -f patterns/11-dependency-injection/pom.xml clean verify` is green,
      including `CompositionRootTest`.
- [ ] Running `Main` prints `succeeded: [slack, email, audit-log]` and
      `failed: []`.
- [ ] You did not modify `ReleaseNotifier.java` while writing `CompositionRootTest`.

Next: [`05-build-and-release.md`](05-build-and-release.md).
