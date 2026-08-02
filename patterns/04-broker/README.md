# Broker

## 1. What it is

The Broker pattern introduces an intermediary between components that need to
communicate but should not know about each other directly. Instead of a publisher
calling a subscriber's method (or a client calling a server's), both sides only ever
talk to the **broker**: publishers **publish** something (an event, a message, a
request) addressed to a topic or event type, and subscribers **register interest**
(`subscribe`) in a topic or event type. The broker is the only component that knows
about both sides; it looks up who is currently interested in what just arrived and
forwards it on.

This buys two kinds of decoupling:

- **Location decoupling** — a publisher never holds a reference to a subscriber (or
  vice versa). Subscribers can be added, removed, or replaced without the publisher's
  code changing at all, and without the publisher even being aware it happened.
- **Time decoupling** (potentially) — a *networked, durable* broker (a message queue,
  an ORB, a pub/sub service) can hold a message until a currently-offline subscriber
  comes back online. A **simple in-process broker — which is what this exercise
  builds — decouples location but not time**: if nobody is subscribed when
  `publish` is called, the event is simply never seen by anyone. There is no queue,
  no persistence, no redelivery. That distinction matters for the exam and matters
  more in practice: don't claim time-decoupling for an architecture that doesn't
  actually have it.

Structurally, a broker-based system has three kinds of building block: **publishers**
(produce messages/events, know nothing about who consumes them), **subscribers**
(consume messages/events they've registered interest in, know nothing about who
produced them or who else is also subscribed), and the **broker** itself (the runtime
component doing registration bookkeeping and dispatch — and, critically, the one place
in the system where a single misbehaving component *can* affect everyone else, which is
why the broker's own failure modes are part of what you're expected to reason about,
not just the happy path).

## 2. Common use cases

- Event-driven systems where several independent consumers need to react to the same
  occurrence (a domain event, a UI event, a hardware interrupt) without the producer
  enumerating them.
- Messaging middleware / message queues (RabbitMQ, Kafka, cloud pub/sub services) as
  the networked, durable realization of this pattern.
- Object request brokers (CORBA-style ORBs) — the historical origin of the name — where
  the broker also hides the network location of the object being called.
- In-process event buses inside a single application (GUI event dispatch, plugin
  systems reacting to lifecycle events, an internal audit/notification fan-out — this
  exercise's case study).

## 3. Trade-offs

| Helps | Hurts |
|---|---|
| Decoupling — publishers and subscribers never reference each other; either side can change independently | The broker is a new component that can itself fail, and when it does, *everyone* connected to it is affected |
| Extensibility — a new subscriber can be added without touching the publisher, or any other subscriber | Harder to trace "what happens when X is published" by reading the code — the call graph isn't visible from the publisher alone (it's implicit, at runtime, in whoever happened to subscribe) |
| Supports fan-out (many subscribers per event) and fan-in (many publishers per topic) naturally | A networked broker adds latency, a new deployment unit to operate, and (for durable brokers) delivery-semantics questions — at-most-once vs. at-least-once vs. exactly-once |
| Can decouple *time* too, if the broker is durable/queuing (not the in-process variant built here) | Ordering and consistency get harder to reason about once dispatch is asynchronous or multi-consumer |

## 4. When *not* to use it

- When there is exactly one consumer and it's always known at compile time — a direct
  method call is simpler, is type-checked, and gives you a stack trace that shows the
  whole call chain; the broker's indirection buys nothing here.
- When you need a guaranteed, ordered, exactly-once request/response with a single
  destination — that's RPC's problem to solve, not Broker's (see the RPC pattern).
- When you need durable, time-decoupled delivery but you've reached for an in-process
  broker like this one — that's a networked message queue's job, not a `HashMap` in a
  singleton.
- When the *number* of possible subscribers is small, fixed, and unlikely to change —
  the indirection is ceremony without payoff; a short list of direct calls is more
  debuggable.

## 5. Case study: Pipeline Event Broker

- **Purpose:** As a PipelineForge pipeline run progresses (it starts, each stage
  finishes, the run finishes), several independent things need to react: a
  Notification Service messages whoever triggered the run, an Audit Logger records the
  event for compliance, and a (hypothetical, not built here) Metrics Collector would
  count stage durations. None of these should know about each other, and the thing
  publishing pipeline events — the Pipeline Runner — shouldn't know who, if anyone, is
  listening. The **Pipeline Event Broker** is the in-process intermediary that makes
  that possible.
- **Actors:**
  - **Pipeline Runner** — the publisher. Runs a pipeline's stages in order and
    publishes one event per lifecycle step (`RunStarted`, `StageCompleted`,
    `RunFinished`). It depends only on `Broker`.
  - **Notification Service** — an independent subscriber. Cares only about
    `RunFinished`.
  - **Audit Logger** — an independent subscriber. Cares about every event type, for a
    compliance trail.
  - **Platform engineer** *(you)* — can add a new subscriber (or even a new event
    type) without touching the Pipeline Runner or either existing subscriber. You'll
    feel this directly in milestone 3.
- **Scope of this exercise:** a single JVM process, an in-process (not networked)
  broker with synchronous, same-thread dispatch — deliberately the simplest possible
  dispatch strategy, so behavior is deterministic and every test can assert on results
  immediately after calling `publish`, with no waiting, polling, or `CountDownLatch`
  needed. No persistence, no retries, no across-process messaging — see section 1's
  point about location vs. time decoupling for why that's a real limitation, not
  laziness.

## 6. Milestones

1. [`00-setup.md`](milestones/00-setup.md) — confirm the module builds, tour the case
   study and target package layout.
2. [`01-events.md`](milestones/01-events.md) — the `PipelineEvent` sealed interface and
   its three record implementations: `RunStarted`, `StageCompleted`, `RunFinished`.
3. [`02-broker-core.md`](milestones/02-broker-core.md) — the `Subscriber` and `Broker`
   interfaces, and `InMemoryBroker`: the pattern's core `subscribe`/`publish` dispatch
   logic, including what happens when a subscriber throws.
4. [`03-subscribers.md`](milestones/03-subscribers.md) — `NotificationService` (given)
   and `AuditLogger` (yours to write): two subscribers that never reference each other
   or the Pipeline Runner.
5. [`04-main-and-demo.md`](milestones/04-main-and-demo.md) — `PipelineRunner` and
   `Main`: the composition root that wires everything together and runs a demo pipeline
   end to end.
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
