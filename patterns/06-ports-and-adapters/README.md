# Ports and Adapters (Hexagonal / Onion / Clean Architecture)

## 1. What it is

Ports and Adapters puts a single core — the domain model plus the use cases that
operate on it — at the center of the system, and forbids that core from depending on
anything outside itself. Everything the core needs *from* the outside world, or
exposes *to* it, is declared as an interface the core itself owns: a **port**. Concrete
technology — a CLI, an HTTP handler, a database, a message queue — lives in an
**adapter** that implements or calls a port, and adapters depend on the core, never the
reverse.

Ports come in two flavors, named for which side initiates the call:

- **Driving (primary) ports** — the core's entry points. Something outside *drives* the
  core through one of these, e.g. `RequestApprovalUseCase.decide(...)`. A CLI, an HTTP
  controller, or a scheduled job could all be driving adapters calling the same port.
- **Driven (secondary) ports** — things the core needs done but can't do itself. The
  core is in the *driver's seat*, calling out through an interface it defines, e.g.
  `ApprovalRepository.save(...)`. A database, a flat file, or an in-memory store are
  all driven adapters implementing the same port.

The pattern was introduced by Alistair Cockburn as "Hexagonal Architecture" — the
hexagon is just a drawing convenience for "one core, an arbitrary number of sides,
each side a pluggable adapter," not a claim that there are exactly six of anything.
Jeffrey Palermo's Onion Architecture and Robert C. Martin's Clean Architecture are
close relatives: same inward-only dependency rule, drawn as concentric rings instead of
a hexagon, with slightly different opinions about how many rings to name.

**Ports & Adapters vs. Layers.** Both patterns are, at heart, about controlling
dependency direction — which is why they're easy to conflate. The difference is what
the rule is *between*:

- **Layers'** dependency rule is *directional between named layers*: Presentation →
  Application → Domain → Infrastructure. Any given layer knows its position in a
  stack, and "the bottom" (Infrastructure) is still a first-class layer name that
  Domain code could technically reach for if nobody stops it.
- **Ports & Adapters'** dependency rule is *inward, symmetrically, toward one core*.
  There is no "top" or "bottom" — a driving adapter (CLI) and a driven adapter
  (database) are peers, both plugged into the core from the outside, and the core has
  no notion of "presentation" or "infrastructure" as architectural concepts at all. It
  only knows about its own ports.

Concretely: in `01-layers`, `Main` (Presentation) is the *only* class allowed to import
Infrastructure — the rule is enforced by layer position. Here, **no** class inside
`core` is ever allowed to import anything from `adapter`, full stop — not "except the
composition root," because the composition root itself (`Main`, in this exercise) lives
*outside* the core, in `adapter.driving.cli`. The core doesn't even know a composition
root exists; it just exposes and consumes interfaces.

This is also the precise answer to "isn't this just DIP with extra steps?" Using an
interface *somewhere* in your code is the Dependency Inversion Principle applied
locally. Ports & Adapters is DIP applied *systematically at the boundary of the whole
core*: every single thing the core depends on that isn't the core itself is an
interface the core owns, with zero exceptions, enforced at the architecture level
rather than left to individual judgment call by call.

## 2. Common use cases

- Business/domain logic that must be testable without spinning up a database, an HTTP
  server, or a message broker — you test the core directly, against fake driven
  adapters.
- Systems expected to gain a second driving channel over time (CLI today, HTTP or a
  chat-bot tomorrow) that must reuse identical business rules, not reimplement them
  per channel.
- Systems expected to change persistence or messaging technology over their lifetime
  without a domain-logic rewrite (e.g. in-memory during early development, a real
  database once it matters).
- Domain logic with a longer expected lifetime than any single framework or library
  choice — the core stays framework-free by construction.

## 3. Trade-offs

| Helps | Hurts |
|---|---|
| The core is unit-testable with zero I/O — fakes/stubs implement the driven ports | More interfaces and indirection than a system with only one adapter of each kind will ever need |
| Multiple driving adapters (CLI, HTTP, ...) can reuse the exact same use case with no duplicated business logic | Every port needs a translation layer in its adapters (CLI args ↔ port DTO, HTTP JSON ↔ port DTO, DB row ↔ port DTO) |
| Driven technology (storage, messaging) is swappable without touching the core, by construction, not by convention | The "hexagon" visual over-promises precision — it's not literally six sides, and newcomers sometimes look for exactly six |
| DIP is enforced structurally at the boundary, not left to case-by-case discipline | Without tooling (module boundaries, ArchUnit-style rules) nothing *stops* a core class from importing a concrete adapter — the discipline still has to be upheld somehow |

## 4. When *not* to use it

- Prototypes or short-lived spikes where only one driving adapter and one driven
  adapter will ever exist — the ports add ceremony with no payoff before the system's
  lifetime ends.
- Thin CRUD services with no real domain logic — if every "use case" just forwards a
  request to storage unchanged, the ports are pass-throughs with nothing to decouple,
  the same anemic-layer trap `01-layers` warns about, just wearing hexagon clothing.
- Teams without a way to *enforce* the core/adapter boundary (a module system, package
  visibility, or a static check) — the pattern's whole value is the inward-only rule,
  and an unenforced rule tends to erode the first time someone's in a hurry.
- Systems where Layers' simpler directional rule already gives you everything you
  need — if there's genuinely only ever going to be one UI and one datastore, the extra
  ceremony of naming every boundary a "port" buys you less than it costs.

## 5. Case study: Deployment Approval Service

- **Purpose:** Before a deployment to a production Kubernetes namespace proceeds in
  PipelineForge, someone — or some automated policy — must approve it. The
  **Deployment Approval Service** is the bounded context responsible for that
  decision: is this request policy-compliant (no self-approval, a real justification),
  and if so, what did the approver decide? The core must not care whether the request
  arrived via a CLI, an HTTP call, or a Slack bot, and must not care whether decisions
  end up in memory, a file, or a database — that indifference is the entire point of
  modeling it as a hexagon.
- **Actors:**
  - **Requester** — the engineer (or automated pipeline step) asking for a deployment
    to be approved.
  - **Approver** — the person granting or denying the request; in this exercise, the
    one running the CLI.
  - **Platform engineer** — can swap the driven adapter (in-memory today, file-backed
    tomorrow) without the core changing at all, which milestone 6 proves rather than
    just asserts.
- **Scope of this exercise:** a single JVM process, one driving adapter (a CLI,
  `Main`), and two driven adapters (`InMemoryApprovalRepository`, and a second one you
  build in milestone 6, `FileApprovalRepository`) implementing the same
  `ApprovalRepository` port. No real authentication, no real Kubernetes API calls —
  enough to see the ports-and-adapters boundary clearly without extra machinery
  getting in the way.

## 6. Milestones

1. [`00-setup.md`](milestones/00-setup.md) — confirm the module builds, tour the case
   study and the target `core` / `adapter` package layout.
2. [`01-core-domain-and-ports.md`](milestones/01-core-domain-and-ports.md) —
   `ApprovalRequest`, `ApprovalDecision`, and the two ports: `RequestApprovalUseCase`
   (driving) and `ApprovalRepository` (driven). Given/copy-paste — these are the
   contracts everything else plugs into.
3. [`02-approval-policy.md`](milestones/02-approval-policy.md) — `ApprovalPolicy` and
   `DefaultApprovalPolicy`: the core decision-rule logic (no self-approval, no blank
   justification). Zero dependency on anything outside `core`.
4. [`03-approval-service.md`](milestones/03-approval-service.md) — `ApprovalService`,
   the core's implementation of the driving port, orchestrating the policy and the
   (still-abstract) driven port. Tested with fakes only — no real adapter exists yet.
5. [`04-in-memory-adapter.md`](milestones/04-in-memory-adapter.md) —
   `InMemoryApprovalRepository`, the first driven adapter, plus a reusable
   *repository contract test* every `ApprovalRepository` implementation must pass.
6. [`05-cli-adapter.md`](milestones/05-cli-adapter.md) — `Main`, the CLI driving
   adapter and composition root: the only class allowed to name every concrete class
   in the module.
7. [`06-second-driven-adapter.md`](milestones/06-second-driven-adapter.md) —
   `FileApprovalRepository`, a second, genuinely different driven adapter, proven
   interchangeable with the in-memory one by running the *same* contract test against
   both, unmodified, with zero changes to `core`.
8. [`07-build-and-release.md`](milestones/07-build-and-release.md) — build, scan, and
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
