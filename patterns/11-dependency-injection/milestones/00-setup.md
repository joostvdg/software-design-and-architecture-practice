# Milestone 0 — Setup

## Goal

Confirm the module builds, understand the target package layout, and get oriented in
the case study before writing any pattern code.

## Confirm the build is green

From the repo root:

```bash
mvn -f patterns/11-dependency-injection/pom.xml clean verify
```

This should pass — right now the module only contains a placeholder test
(`SmokeTest`) and an empty `package-info.java`. That placeholder test is there so you
always have a green build to come back to; delete it once the notification channel
port (next milestone) has its own tests.

## Target layout

By the end of milestone 4 you'll have:

```
src/main/java/com/isaqb/practice/di/
  Main.java                  # composition root: constructs concrete channels, injects them
  NotificationChannel.java   # the port ReleaseNotifier depends on
  ReleaseNotification.java   # data carried to send()
  ReleaseNotifier.java       # the core: depends only on List<NotificationChannel>
  ChannelSendException.java
  channels/
    SlackChannel.java
    EmailChannel.java
    AuditLogChannel.java
```

Notice the dependency direction: `ReleaseNotifier` imports `NotificationChannel` and
`ReleaseNotification` — never a class under `channels/`. Only `Main` imports concrete
channel classes, to construct them and inject them into `ReleaseNotifier`'s
constructor. That's the pattern in one sentence: the consumer's source code never
changes to support a new channel; only the composition root's wiring does.

## The case study, one more time

You're building the **Release Notification Composer**: when a release is published,
`ReleaseNotifier.notify(release)` sends a `ReleaseNotification` to every channel it
was constructed with. It doesn't know whether that means a Slack API call, an SMTP
send, or an audit log append — it only knows `NotificationChannel.send(...)`.

## Checkpoint

- [ ] `mvn -f patterns/11-dependency-injection/pom.xml clean verify` passes.
- [ ] You can explain, in one sentence, the difference between the Dependency
      Inversion *Principle* and Dependency *Injection* as a mechanism (see README
      section 1) — you'll need this distinction if the exam asks you to contrast
      them.

Next: [`01-notification-channel-port.md`](01-notification-channel-port.md).
