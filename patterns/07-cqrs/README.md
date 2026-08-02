# CQRS (Command Query Responsibility Segregation)

## 1. What it is

CQRS splits a model into two: a **command side** that accepts state-changing
operations (commands) and enforces the rules for how state is allowed to change, and a
**query side** that serves reads from a model shaped for the reader, not for the
writer. The two sides are genuinely separate types — not the same object exposed
through two facades. The command side owns the source of truth; the query side owns
one or more **read models** (sometimes called projections) that are *derived from* the
write side's changes, kept up to date by some explicit synchronization step.

That synchronization step is the crux of the pattern. In the simplest form (this
exercise), it's a direct, synchronous call: right after a command completes, something
rebuilds the affected read model from the new write-side state, in the same thread,
before the command returns. In production systems at scale, that step is usually
**asynchronous** — the write side publishes that something changed, and one or more
independent consumers update their own read models later, on their own schedule. That
shift is what introduces **eventual consistency**: a query issued immediately after a
command may briefly return stale data, because the read model hasn't caught up yet.
CQRS does not require eventual consistency (synchronous projection updates, like this
exercise uses, are perfectly valid CQRS) — but the pattern's usual payoff (scaling
reads and writes independently, choosing different storage technology per side) only
shows up once you go async, and eventual consistency is the price of admission for
that payoff.

CQRS is frequently paired with **Event Sourcing** (state as an append-only sequence of
events, with read models being one of possibly several projections of that event
stream) — see `../08-event-sourcing/` if it exists in your checkout. They are
independent patterns: you can do CQRS with a conventional mutable write store (as this
exercise does — a `PipelineRun` aggregate, not an event log), and you can do event
sourcing without ever splitting reads from writes. This exercise deliberately keeps
them apart so you feel what CQRS alone buys you.

## 2. Common use cases

- Dashboards and reporting views whose ideal shape (denormalized, pre-aggregated, easy
  to render) is nothing like the shape the write side needs to enforce invariants.
- Systems where read volume and write volume scale very differently (many dashboard
  refreshes per command issued) and you want to scale/cache/replicate them separately.
- Audit-heavy or compliance-heavy domains where the write side must be strict about
  what transitions are legal, while several different read shapes are needed for
  different consumers (a summary view, a detail view, an export).
- Systems already doing Event Sourcing, where read models (projections) are a natural
  by-product of replaying the event stream.

## 3. Trade-offs

| Helps | Hurts |
|---|---|
| Read model can be shaped exactly for its consumer (denormalized, precomputed) instead of a compromise between writer and reader needs | Two models to keep in sync instead of one — more code, more to reason about |
| Write side stays focused on enforcing invariants, not on convenience fields only reads need | If projection updates are asynchronous, callers must tolerate eventual consistency — a read can be stale for a window after a write |
| Read and write sides can scale independently, and even use different storage technology, once decoupled via async messaging | Debugging is harder: "why does the dashboard show the old state" now has two possible answers (bug, or just propagation lag) |
| Testable in isolation: the write side's business rules and the read side's projection logic can each be tested without the other | Introduces real complexity (a projection step, possibly a queue) that is pure overhead if read and write shapes were never going to diverge |

**On eventual consistency specifically:** it's worth the complexity when read and write
load genuinely need to scale independently, or when the read-optimal storage
technology is different from the write-optimal one (e.g. a search index or a wide
denormalized table fed from a transactional write store). It is *not* worth it for a
single-process service with modest load, where a synchronous projection update (this
exercise's approach) gives you the same shape-separation benefit without the staleness
window or the operational cost of a queue. Milestone 4 makes this concrete: the
projection update in this exercise runs synchronously, in the same thread, right after
each command — and the README/milestone text calls out exactly what you'd change to
make it genuinely eventually consistent, without actually needing you to stand up a
message queue to finish the exercise.

## 4. When *not* to use it

- When the write and read shapes are already the same, or nearly so — CQRS just adds a
  second model and a synchronization step for no payoff.
- When the team/system can't tolerate *any* staleness and there's no budget to keep
  everything synchronous — synchronous CQRS is fine, but the moment someone reaches for
  "let's make projection updates async for scale" without discussing the staleness
  trade-off with consumers of the read side, you get confusing bugs.
- For small CRUD services where a single model read and written through the same
  repository is simpler and nobody has asked for independent scaling.
- As a default "because it's more scalable" choice — CQRS is justified by a concrete
  divergence between read and write needs, not by itself being a best practice.

## 5. Case study: Pipeline Run Query/Command split

- **Purpose:** PipelineForge's pipeline runner mutates pipeline-run state constantly
  while a run is in progress — starting it, marking each stage complete, finishing it.
  PipelineForge's dashboard, meanwhile, wants to *read* pipeline-run state in a
  completely different shape: a denormalized summary with the current status, how many
  stages are done out of how many, and how long the run has been going. Modeling both
  needs as one shared `PipelineRun` object tends to produce either a bloated write
  model dragging along read-only convenience fields (a `stagesCompletedCount` field
  the write side has to remember to keep in sync with the stage list on every mutation)
  or a read model that's awkward to query efficiently (recomputing stage counts and
  duration on every dashboard refresh from the same object the write side locks for
  mutations). This bounded context keeps the two apart on purpose.
- **Actors:**
  - **Pipeline Runner** *(not built here, represented as the caller of the command
    side)* — issues commands: start a run, complete a stage, finish a run. It never
    reads `PipelineRunSummary`; it only cares that its commands are accepted or
    rejected.
  - **Dashboard** *(not built here, represented as the caller of the query side)* —
    issues queries only, never commands, against `PipelineRunSummary`, a shape
    optimized for display rather than for enforcing invariants.
- **Scope of this exercise:** a single JVM process, in-memory storage on both sides (no
  database), and a **synchronous** projection update (the query side's projection is
  rebuilt in the same thread, immediately after each command, not via a queue or
  background consumer). That keeps the exercise deterministic and easy to test while
  still teaching the structural split; the trade-off of *not* going fully async here is
  discussed explicitly in milestone 4 and section 3 above.

## 6. Milestones

1. [`00-setup.md`](milestones/00-setup.md) — confirm the module builds, tour the case
   study and target package layout.
2. [`01-write-model.md`](milestones/01-write-model.md) — the write-side `PipelineRun`
   aggregate, `PipelineStage`, and the status enums. Zero dependency on the query side.
3. [`02-command-service.md`](milestones/02-command-service.md) — `PipelineRunCommandService`:
   `startRun`, `completeStage`, `finishRun`, and the business rules each one enforces.
4. [`03-read-model.md`](milestones/03-read-model.md) — the read-side `PipelineRunSummary`
   and `PipelineRunQueryService`, including the projection logic that turns a
   `PipelineRun` into a `PipelineRunSummary`.
5. [`04-wiring-and-eventual-consistency.md`](milestones/04-wiring-and-eventual-consistency.md)
   — wire the command side to the query side through an explicit, swappable seam; the
   `Main` composition root; and the thought experiment on what changes if that seam
   becomes asynchronous.
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
