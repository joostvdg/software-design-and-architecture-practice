# Pipes and Filters

## 1. What it is

Pipes and Filters structures a system as a sequence of independent processing steps
(**filters**), each consuming data in one shape and producing data in another,
connected by **pipes** that pass output from one filter to the input of the next. A
filter knows nothing about what runs before or after it — only its own input/output
contract. A pipe is just a data connector: in this exercise, a plain Java `List` passed
from one filter's `apply` call to the next; in a real system it could be a message
queue, a Unix pipe, or an HTTP call, but the *shape of the composition* is the same.

The defining property is **independence**: each filter can be developed, tested,
reordered, replaced, or run in isolation without any of the others knowing or caring.
A pipeline is built by composing filters, not by writing one method that does
everything — that's the whole difference between this pattern and a plain "workflow"
or "script": a workflow is a sequence of *steps* that may share state, branch, and know
about each other; a pipeline is a sequence of *transformations* connected only by the
data flowing through them; only via clearly typed pipes.

**Batch vs. stream** is a real design choice, not an implementation detail: a *batch*
pipeline (this exercise) passes whole collections between filters — simple to reason
about and test, but a filter can't start work until the previous one has produced its
entire output, and everything must fit in memory. A *stream* pipeline passes elements
(or small chunks) one at a time, so filters run concurrently and can process
unbounded/continuous input — but ordering, backpressure, and error handling per element
become real design problems instead of afterthoughts. This exercise deliberately stays
batch (`List<I>` in, `List<O>` out) to keep the pattern's essence — composable,
independently testable transformations — visible without those extra concerns.

## 2. Common use cases

- Compiler pipelines: lexing → parsing → semantic analysis → code generation.
- CI/CD pipelines: checkout → build → test → package → deploy — each stage is a filter.
- Log/event processing and ETL: parse → filter/clean → enrich → aggregate → store.
- Image/audio/video processing chains, where each filter applies one transformation.
- Unix shell pipelines (`grep | sort | uniq -c`) — the pattern's original, literal home.

## 3. Trade-offs

| Helps | Hurts |
|---|---|
| Independent testability — each filter is a pure function you can test with no knowledge of its neighbors | Shared/global state between "steps" doesn't fit the pattern — that's a sign you actually want a workflow, not a pipeline |
| Reusability and reorderability — filters can be recombined into new pipelines | Per-item error handling and partial failure are awkward: what does the pipeline do with entry 500 of 10,000 that fails to parse? |
| Easy to reason about and extend — add/remove/replace a filter without touching the others | End-to-end latency in a batch pipeline is the sum of every filter's full pass, not the slowest one (that's a stream-pipeline benefit, not batch's) |
| Maps directly onto how CI/CD and data pipelines are already described and diagrammed | Data conversions between filters (serialize/deserialize, shape changes) add overhead that a monolithic function wouldn't pay |

## 4. When *not* to use it

- When steps genuinely need to share rich mutable state or make decisions based on
  more than "the data currently flowing through" — that's an orchestrated workflow,
  not a pipeline; forcing it into Pipes and Filters just hides the coupling.
- When there's only one transformation, or the "filters" would just be arbitrary
  cut points in what's naturally one algorithm — the composition overhead buys nothing.
- When strict, atomic, all-or-nothing processing of a single unit of work matters more
  than throughput/composability — a straight-line function with a transaction boundary
  is easier to reason about than a value bouncing through independently-failing stages.
- When true low-latency, one-item-at-a-time processing is required — a batch pipeline
  (as here) is the wrong shape; you'd want a streaming pipeline or reactive design
  instead.

## 5. Case study: Log Ingestion Pipeline

- **Purpose:** every PipelineForge build agent emits raw log lines while a pipeline
  runs. Before those lines are searchable in the platform's observability backend,
  they need to be parsed into structured entries, cleaned of debug noise, scrubbed of
  anything that looks like a leaked credential, and summarized by severity. Each of
  those is a separate, independently testable concern — which is exactly what Pipes
  and Filters is for.
- **Actors:**
  - **Build Agent** *(not built here, represented only as "the source of raw lines")*
    — produces the raw log text this pipeline consumes.
  - **Observability team** — consumes the filtered, redacted, aggregated output; cares
    that secrets never reach storage and that noise doesn't drown out real signal.
  - **Platform engineer** — configures which filters run and in what order (in this
    exercise, that's you, wiring filters together in `Main`).
- **Scope of this exercise:** a single JVM process, one package, a hand-rolled log line
  text format (`<ISO-8601 timestamp> <LEVEL> <message>`), and four filters running as a
  **batch** pipeline over `List<String>` / `List<LogEntry>` — no message queue, no
  concurrency, no persistence. Enough to see filters compose and to feel *why*
  independence makes each one trivially testable, without streaming/concurrency
  machinery obscuring the core idea.

## 6. Milestones

1. [`00-setup.md`](milestones/00-setup.md) — confirm the module builds, tour the case
   study and target package layout.
2. [`01-filter-and-parser.md`](milestones/01-filter-and-parser.md) — the `Filter<I, O>`
   contract, the `LogEntry` domain type, and `LogLineParser`: raw text in, structured
   entries out.
3. [`02-drop-debug-filter.md`](milestones/02-drop-debug-filter.md) — `DropDebugFilter`,
   the noise-reduction step.
4. [`03-redact-secrets-filter.md`](milestones/03-redact-secrets-filter.md) —
   `RedactSecretsFilter`, scrubbing anything that looks like a leaked token.
5. [`04-aggregator-and-pipeline.md`](milestones/04-aggregator-and-pipeline.md) —
   `SeverityCountAggregator`, the `Pipeline` composition helper, and wiring all four
   filters together end to end in `Main`.
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
