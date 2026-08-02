# Event Sourcing

## 1. What it is

Most systems store **current state**: a row that gets overwritten every time something
changes. If a pipeline run's status flips from `RUNNING` to `FAILED`, the old value is
gone — the row now just says `FAILED`, and nothing in the database can tell you it was
ever anything else, or when the change happened, or whether a stage was retried along
the way.

Event Sourcing inverts that. Instead of storing current state, you store **every fact
that ever happened**, in the order it happened, as an immutable, append-only log of
**events** (`RunStarted`, `StageFailed`, `RunFinished`, ...). Current state is never
stored directly — it is a **derived value**, computed on demand by **replaying** the
log from the beginning (or from a checkpoint, see snapshotting below) and folding each
event into a running result. You could delete every "current state" object in the
running process and rebuild it perfectly from the log; the log, not the object, is the
durable source of truth.

This gives you, essentially for free:

- **A complete audit trail.** Nothing is ever overwritten or deleted, so "what happened,
  in what order, and when" is always answerable, not just "what's the state now."
- **Point-in-time reconstruction.** Replaying only the first *N* events answers "what
  did we know after event *N*" — a debugging capability a mutable row simply cannot
  offer, because it only ever remembers its latest value.
- **New projections without touching the writer.** Any new way of looking at the same
  history (a new report, a new read model) is just a new fold over the same events — the
  code that appends events never has to change to support it.

The cost is that replaying grows more expensive as the log grows, which is what
**snapshotting** (milestone 4) exists to address: periodically cache the folded state
at some event count, then future replays only need to fold the events *after* that
checkpoint, not the whole history from event zero.

**This is not the Broker pattern.** [`../04-broker/README.md`](../04-broker/README.md)'s
case study also has "events" and a "Pipeline Runner", but it solves a different problem:
*dispatching* an in-flight event to whichever subscribers currently care about it,
synchronously, with no memory of what was published before. A broker's events are not
stored anywhere once `publish()` returns — if nobody was listening, the event is simply
gone. This exercise builds the opposite thing: an append-only log where storing the
event *is the point*, nobody needs to be "listening" for anything to happen, and state
is derived by reading the log back later, possibly much later, possibly by something
that didn't exist yet when the event was appended.

**This is also not "event-driven architecture" (EDA) in general**, even though the
vocabulary overlaps. EDA is about *how components communicate* — producers publish
events, consumers react, choreography vs. orchestration, coupling via event schemas
(that's Broker's territory, and Tier 3's EDA entry). Event Sourcing is about *how state
is persisted* — the event log is the durable record an aggregate's state is derived
from. A system can do either without the other: this exercise's `EventStore` never
notifies anyone of anything (no dispatch, no subscribers — append and read-back only),
and the Broker exercise never persists an event past the `publish()` call that
delivered it. They're orthogonal decisions that happen to share the word "event."

**Event Sourcing is often paired with CQRS** (Command Query Responsibility
Segregation, `../07-cqrs/` if you've built it — this exercise does not require or
depend on it): CQRS separates the write model (which handles commands and decides what
happened) from the read model (which serves queries, optimized independently of how
writes are stored). Event Sourcing is a natural way to implement CQRS's write side —
append an event instead of mutating a row — while one or more projections serve the
read side, each optimized for a different query shape. This exercise's read side is
deliberately minimal: "replay the log into one state shape," nothing more elaborate. If
you want multiple independently-optimized read models fed by the same event log, and a
real command/query split, that's what the CQRS pattern (if present) builds on top of
what you build here.

## 2. Common use cases

- Compliance and audit domains where "what happened, in what order, and when" is a
  legal or contractual requirement, not just a nice-to-have.
- Financial ledgers and anything where "how did we get to this number" must always be
  reconstructable (an account balance is a projection over a list of transactions, not
  a stored number, in most real accounting systems).
- Debugging production incidents: replaying a shorter prefix of a real event log to see
  the exact state at the moment something went wrong.
- Domain-Driven Design aggregates whose lifecycle is naturally a sequence of meaningful
  business events, not a sequence of field updates.
- As the write-side implementation underneath CQRS, when a system needs several
  differently-shaped read models over the same underlying history.

## 3. Trade-offs

| Helps | Hurts |
|---|---|
| Full audit trail, for free — every fact is retained, nothing is silently overwritten | Replaying grows more expensive as the log grows — untreated, this is a genuine scalability problem (motivates snapshotting) |
| Point-in-time / "as of event N" reconstruction, directly useful for debugging | Modeling "current state" now requires an explicit fold/projection step — more upfront design than a mutable row that just holds the latest values |
| New projections (new reports, new read models) can be added later without changing how events are written | Past events' shapes must be handled forever, or explicitly migrated/upcast — you can't just `ALTER TABLE` a fact that already happened |
| Natural fit for compliance/audit domains and for pairing with CQRS's write side | Querying "all runs currently FAILED" isn't a simple row scan against the log — you need a projection/read-model built for that query, which is extra machinery |

## 4. When *not* to use it

- When nobody will ever need history, audit, or point-in-time debugging — if only
  "what's the state right now" ever matters, a mutable row is simpler and cheaper, and
  Event Sourcing's benefits are wasted overhead.
- When write volume is very high and the value of retaining every fact is low — storing
  (and eventually snapshotting) a full history has a real storage and complexity cost
  that needs to be justified by an actual audit/debugging/reconstruction need.
- When the team isn't ready to design and version an event schema — unlike a mutable
  column you can migrate, past events are facts about the past and, in a real system,
  are far harder to retroactively change.
- When you actually need real-time reaction to events as they happen (notify these
  three services the moment a run finishes) — that's Broker/EDA's job; Event Sourcing
  by itself has no notion of "notify anyone," only "remember, and let anyone replay
  later."

## 5. Case study: Pipeline Run Audit Trail

- **Purpose:** For compliance and debugging, PipelineForge needs to know not just "what
  is the current state of run X" but "exactly what happened to run X, in what order,
  and when" — which stage started when, whether it was retried, what the final outcome
  was. Instead of a mutable "current state" row that gets overwritten (losing history),
  every fact about a run is appended as an immutable event to a log; current state is a
  *derived* value, computed by replaying that log. This gives a free audit trail and
  lets you answer "what was the state at event N" for debugging, not just "what is it
  now."
- **Actors:**
  - **Pipeline Runner** *(simulated in this exercise by `PipelineRunSimulator`)* —
    appends one event per lifecycle step (`RunStarted`, `StageStarted`,
    `StageCompleted`, `StageFailed`, `RunFinished`) as a run progresses. It never reads
    or computes state — only appends facts.
  - **Auditor** — replays a run's full event history, event by event, for compliance
    review; can answer "what was the state after event N," not only "what is it now."
  - **Dashboard** — wants the current state of a run. It computes that by replaying the
    log to the end, on demand, rather than reading a stored "current status" field —
    because no such field exists anywhere in this module.
- **Scope of this exercise:** a single JVM process, an in-memory `EventStore` (a
  `Map<runId, List<Event>>` is enough — this is a practice exercise, not a durable
  store) that only supports appending and reading back, never mutating or deleting; a
  sealed `PipelineRunEvent` hierarchy; a pure fold/replay function that turns a list of
  events into a `PipelineRunState`; and one milestone on snapshotting a
  `PipelineRunState` so replay can resume from a checkpoint instead of always starting
  at event zero. No persistence beyond the JVM's memory, no networking, no dispatch to
  subscribers — see section 1 for why that last one is a deliberate, pattern-defining
  omission, not a missing feature.

## 6. Milestones

1. [`00-setup.md`](milestones/00-setup.md) — confirm the module builds, tour the case
   study and target package layout.
2. [`01-events.md`](milestones/01-events.md) — the `PipelineRunEvent` sealed interface
   and its five record implementations: `RunStarted`, `StageStarted`, `StageCompleted`,
   `StageFailed`, `RunFinished`.
3. [`02-event-store.md`](milestones/02-event-store.md) — the `EventStore` contract and
   `InMemoryEventStore`: append-only, defensive-copy reads, no mutation or deletion.
4. [`03-projection.md`](milestones/03-projection.md) — `PipelineRunState` and
   `PipelineRunProjector`: the pattern's core, a pure fold that turns an ordered event
   list into a derived state.
5. [`04-snapshotting.md`](milestones/04-snapshotting.md) — `Snapshot` and
   `SnapshotAssistedProjector`: resume a replay from a cached checkpoint instead of
   always folding from event zero.
6. [`05-main-and-demo.md`](milestones/05-main-and-demo.md) — `PipelineRunSimulator`,
   `Auditor`, `Dashboard`, and `Main`: wire everything together and watch the Auditor
   and Dashboard views, and a snapshot-assisted replay, all agree.
7. [`06-build-and-release.md`](milestones/06-build-and-release.md) — build, scan, and
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
