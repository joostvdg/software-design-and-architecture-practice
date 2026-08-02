# Layers

## 1. What it is

The Layers pattern organizes a system into horizontal slices — typically
Presentation, Application, Domain, and Infrastructure — where each layer only depends
on the layer(s) "below" it (or, in the relaxed variant, on any layer below it; in the
strict variant, only on the layer immediately below). Each layer has a distinct
responsibility:

- **Presentation** — talks to the outside world (CLI, HTTP, UI); no business rules.
- **Application** — orchestrates use cases: which domain objects to call, in what
  order, wrapped in what transaction/error-handling; no business rules of its own.
- **Domain** — the business rules and model themselves; knows nothing about how it's
  invoked or where its data comes from.
- **Infrastructure** — technical detail: file I/O, databases, network calls; implements
  interfaces the layers above depend on.

The critical distinction is **logical layers vs. physical tiers**: layers are a source-
code organizing principle (which package/module may `import` which), not necessarily
separate deployables or processes. You can — and this exercise does — run all four
layers in a single JVM process; what makes it "layered" is the *dependency direction*,
not the deployment topology.

**Strict vs. relaxed layering:** strict layering only allows a layer to call the layer
directly beneath it (Presentation → Application → Domain, never Presentation →
Domain). Relaxed layering allows skipping layers downward. This exercise uses strict
layering because it's the version that's easiest to verify by eye, and the version most
exam questions probe.

## 2. Common use cases

- Business/CRUD-shaped applications where "what are the steps to do X" (Application)
  is naturally distinct from "what does X mean" (Domain).
- Systems where you want to swap Infrastructure (a different database, a different
  transport) without touching business rules.
- The default starting structure for a modular monolith before any module extraction.

## 3. Trade-offs

| Helps | Hurts |
|---|---|
| Separation of concerns; each layer is independently understandable | Can encourage anemic layers that just pass data through (Presentation → Application → Domain with no logic anywhere but Domain) |
| Testability — Domain and Application are testable without I/O | Strict layering can force awkward indirection for genuinely cross-cutting concerns (logging, auth) |
| Change isolation — a new UI or a new database touches only one layer | Layer count and interface count add ceremony for small systems |
| Familiar, low-onboarding-cost structure | Doesn't by itself say anything about deployment, scaling, or team boundaries (that's Microservices' job) |

## 4. When *not* to use it

- When the "layers" would just be a thin pass-through with no distinct logic in most
  of them — that's ceremony without benefit.
- When the system's hard problem is deployment/scaling/team-autonomy, not code
  organization — Layers doesn't address that; see Microservices or Modular Monolith.
- When you actually need Domain to stay ignorant of *any* technical concern, including
  how it's invoked and persisted — Ports and Adapters expresses that dependency rule
  more precisely than Layers does (Layers still implicitly lets Infrastructure be "the
  bottom", which tempts Domain code to reach for it).

## 5. Case study: Build Config Validator

- **Purpose:** Before PipelineForge's orchestrator accepts a submitted pipeline
  configuration and schedules it, it must be validated: does it have a name, does it
  declare at least one stage, are stage names unique? The Build Config Validator is the
  small service responsible for that check. In production it would run as a step the
  orchestrator calls before accepting a submission; in this exercise it runs as a CLI
  a platform engineer can run locally against a config file.
- **Actors:**
  - **Platform engineer** — runs the CLI against a config file while authoring a new
    pipeline, before submitting it.
  - **Pipeline Orchestrator** *(not built here, represented only as "a future caller")*
    — would call the same Application-layer use case programmatically instead of via
    CLI, which is exactly the point of keeping Presentation thin.
  - **Rule author** — whoever maintains the validation rules (in this exercise, that's
    you, adding a `ValidationRule` implementation in the Domain layer).
- **Scope of this exercise:** a single JVM process, four packages (one per layer), a
  hand-rolled config text format (no YAML library — see milestone 3), and three
  validation rules. No persistence, no network calls — enough to see the dependency
  direction clearly without extra machinery getting in the way.

## 6. Milestones

1. [`00-setup.md`](milestones/00-setup.md) — confirm the module builds, tour the case
   study and target package layout.
2. [`01-domain-layer.md`](milestones/01-domain-layer.md) — `PipelineConfig`,
   `ValidationRule`, `ValidationError`, `ValidationResult`, and the three concrete
   rules. Zero dependencies on any other layer.
3. [`02-application-layer.md`](milestones/02-application-layer.md) — the
   `ValidateConfigUseCase` and the `ConfigSource` port it depends on (implemented by
   Infrastructure in the next milestone).
4. [`03-infrastructure-layer.md`](milestones/03-infrastructure-layer.md) —
   `FileConfigSource`, parsing the hand-rolled config format from disk.
5. [`04-presentation-layer.md`](milestones/04-presentation-layer.md) — the `Main` CLI
   entry point: the composition root that wires Infrastructure into Application and
   prints the result.
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
