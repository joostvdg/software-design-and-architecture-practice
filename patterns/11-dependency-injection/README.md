# Dependency Injection

## 1. What it is

Dependency Injection (DI) is the *mechanism* by which a class receives the objects it
depends on (its collaborators) from the outside, rather than constructing them
itself. A class that needs a `NotificationChannel` declares that need — typically as a
constructor parameter typed to an interface — and something else, the **composition
root**, decides which concrete implementation to hand it and does the `new`-ing. The
class itself never writes `new SlackChannel()`.

It's important to keep two things distinct, since the exam draws this line
deliberately:

- **Dependency Inversion Principle (DIP)** — the *design principle*: high-level
  modules should depend on abstractions, not on low-level modules' concrete types.
  DIP is about which direction `import`/dependency arrows point.
- **Dependency Injection** — the *mechanism/pattern*: how a class actually gets an
  instance of that abstraction at runtime, without constructing it itself.

You can follow DIP without DI (e.g. a factory method the class calls itself, as long
as the factory returns an abstraction) — but DI is the most common and most testable
way to make DIP real, because the composition root becomes the *only* place that
needs to change when you swap an implementation, and tests can inject a fake/stub
without touching the class under test at all.

**No framework here.** Per this repo's JDK-only constraint, "wiring" in this module
means one class (`Main`, or a small `Wiring` helper) manually constructing concrete
types and passing them into constructors — exactly what a DI *framework* (Spring,
Guice, ...) automates via reflection/annotations/config. Doing it by hand once makes
clear what those frameworks are actually doing for you.

## 2. Common use cases

- Any class that needs to remain testable in isolation (inject a fake collaborator in
  tests, a real one in production) without a mocking framework reaching into private
  fields.
- Systems where a dependency has more than one plausible implementation over the
  system's life (a notification channel, a storage backend, a payment gateway) and
  swapping it shouldn't ripple through consumer code.
- Composition roots in general — the `main` method, an HTTP handler factory, a test's
  `@BeforeEach` — anywhere "which concrete types get wired together" needs one
  answer, not one per call site.

## 3. Trade-offs

| Helps | Hurts |
|---|---|
| Consumers are testable without touching real infrastructure (inject a fake) | Indirection: reading a class's constructor doesn't show you the *concrete* behavior, only the *shape* it depends on |
| Swapping an implementation touches the composition root only, not every call site | Manual wiring (no framework) means the composition root grows linearly with the object graph — fine for one module, painful at application scale |
| Makes DIP concrete and checkable: a consumer class's imports show it depends only on interfaces | DI frameworks (not used here, but common in the wild) trade this file's explicit wiring for reflection/annotation magic that's harder to trace by reading code |
| Encourages small, single-purpose constructors — a constructor with twelve parameters is a design smell DI makes visible early | Over-applying DI to types that will only ever have one implementation is ceremony without payoff |

## 4. When *not* to use it

- When a dependency is a stable, stateless utility with exactly one implementation
  that will never need to be faked (e.g. `Math`-like pure functions) — injecting it
  buys nothing.
- When constructing the "real" dependency is itself trivial and side-effect-free —
  DI's main payoff (swappability, testability against something expensive/impure)
  isn't there.
- When you're inside a framework that already owns object construction end-to-end
  (many web frameworks) and manual wiring on top would fight the framework's own DI
  container rather than complement it.

## 5. Case study: Release Notification Composer

- **Purpose:** When PipelineForge publishes a release, several channels need to be
  notified — Slack, email, and an internal audit log. The core use case,
  `ReleaseNotifier`, must not know or care which channels exist or how each one
  actually sends a message (an HTTP call to Slack's API, SMTP, a log append) — it
  only knows it has a list of `NotificationChannel`s and calls `send` on each. Which
  concrete channels exist, and which of them are active for a given release, is
  decided once, at the composition root.
- **Actors:**
  - **ReleaseNotifier** — the core/consumer. Depends only on
    `List<NotificationChannel>`, injected via its constructor.
  - **SlackChannel / EmailChannel / AuditLogChannel** — concrete implementations of
    `NotificationChannel`; none of them know `ReleaseNotifier` exists.
  - **Platform engineer** *(you)* — plays the composition root's author: `Main`
    manually constructs the concrete channels and injects them into
    `ReleaseNotifier`.
- **Scope of this exercise:** a single JVM process, one package, channels that
  simulate their side effect (append a formatted string to an in-memory sink) rather
  than making real network/SMTP calls — enough to prove the wiring and the "one
  channel's failure doesn't stop the others" resilience rule without needing real
  external services.

## 6. Milestones

1. [`00-setup.md`](milestones/00-setup.md) — confirm the module builds, tour the case
   study and target package layout.
2. [`01-notification-channel-port.md`](milestones/01-notification-channel-port.md) —
   the `NotificationChannel` interface and `ReleaseNotification` data type.
3. [`02-release-notifier-core.md`](milestones/02-release-notifier-core.md) — the
   `ReleaseNotifier` core, injected with its channels, with per-channel failure
   isolation.
4. [`03-concrete-channels.md`](milestones/03-concrete-channels.md) — `SlackChannel`,
   `EmailChannel`, `AuditLogChannel`.
5. [`04-composition-root.md`](milestones/04-composition-root.md) — `Main`: manual
   wiring, and a test proving swapping channels never changes `ReleaseNotifier`.
6. [`05-build-and-release.md`](milestones/05-build-and-release.md) — build, scan, and
   (practice-)release this module.

## For AI agents working in this folder

- Work one milestone at a time, in order. Don't jump ahead or generate a later
  milestone's content unprompted.
- Never generate a whole milestone's (or the whole pattern's) implementation in one
  shot. Prefer producing signatures, interfaces, and TODO-marked stubs, plus the test
  that defines correct behavior - then let the human write the method bodies.
- When asked "how would this work" or for a hint, give a short snippet or explanation,
  not the full solution.
- Explain *why* a step exists (which quality attribute it demonstrates), not just what
  to type.
- For the release/build milestone, don't invent a release process: follow
  `../../RELEASE.md`, `../../AGENTS-good-release.md`, and `../../AGENTS-record-release.md`.
