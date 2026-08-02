# Microservices

## 1. What it is

Microservices structures a system as a set of small, **independently deployable**
services, each owning its own data and communicating only over the network (never via
shared memory, a shared database, or a shared library carrying business state). Where
Layers is a *source-code* organizing principle (which package may import which, inside
one process), Microservices is a *runtime/deployment* organizing principle: which
process boundaries exist, who can deploy across them independently, and who owns which
slice of data.

Three ideas make it what it is, not just "a distributed monolith":

- **Independent deployability** — each service can be built, tested, and deployed on
  its own schedule, by its own team, without coordinating a lockstep release with every
  other service. If you can't ship service A without also redeploying service B, you
  don't yet have two services — you have one deployment unit split across two
  repositories.
- **Data ownership** — each service owns its own data and is the only thing allowed to
  write it. Other services that need that data ask for it (a call, an event) rather
  than reaching into the owner's storage directly. This is what makes independent
  deployability possible: if two services shared a database schema, a change to one
  could silently break the other.
- **Communication over the network** — since services don't share memory, every
  interaction crosses a process (and usually a machine) boundary. This can be
  **synchronous** (a request-response call, e.g. HTTP — the caller waits) or
  **asynchronous** (a message/event — the caller doesn't wait for the receiver to
  finish). This exercise uses synchronous HTTP because it's the simplest to see and
  test end-to-end; asynchronous inter-service communication is the Broker pattern's
  territory (`../04-broker/`).

The size of a "microservice" is not really about lines of code — it's about the size of
the bounded context it owns. A service is too big if it has more than one reason to
change (multiple teams, multiple deploy cadences colliding); it's too small if most
requests need to fan out to two or three other services just to answer one question
(chatty, latency-dominated designs — a classic microservices anti-pattern).

## 2. Common use cases

- Independent scaling: one part of the system is read-heavy and needs to scale
  horizontally; another is write-heavy and needs strict consistency. Splitting them
  lets each scale on its own terms.
- Independent release cadence: one team ships daily, another ships weekly because its
  domain changes slowly — coupling them into one deployable would force one team to
  wait on the other.
- Organizational scaling ("you ship your org chart," Conway's Law): once a single
  monolith has more contributing teams than it can coordinate releases for, splitting
  along team/bounded-context lines removes the coordination bottleneck.
- Technology heterogeneity: different services can use different languages/runtimes
  where that's genuinely justified (rare in practice, and not exercised here, since
  this repo is Java-only by constraint).

## 3. Trade-offs

| Helps | Hurts |
|---|---|
| Team/service autonomy — independent deploy, independent scaling | Operational overhead — N services means N things to build, deploy, monitor, and keep alive, instead of one |
| Fault isolation — one service crashing doesn't necessarily take the others down | New failure mode: **partial failure**. A synchronous call can time out, refuse the connection, or return an error the caller must handle — a local method call in a monolith can't do any of that |
| Clear data ownership — no other service can corrupt your data by writing to your schema | Cross-service consistency becomes eventual, not transactional — no single ACID transaction spans two services' data (see milestone 5's registry-unreachable case) |
| Independent technology/scaling choices per service | Latency — what was an in-process call becomes a network round trip, every time |
| Smaller, easier-to-understand codebases per service | Distributed debugging/tracing is harder — a single user-visible failure can span several services' logs |

## 4. When *not* to use it

- When the team is small enough that one deployable is easier to coordinate than many —
  the operational tax (N services to build/deploy/observe) outweighs any autonomy gain.
  This is exactly the case for a **modular monolith**: strong internal module
  boundaries (Layers, Ports and Adapters) *inside* one deployable, with the option to
  extract a module into its own service later, once a real independent-scaling or
  independent-team need shows up.
- When the "services" would still need to deploy together to work correctly (shared
  schema migrations, lockstep API versioning) — that's a distributed monolith: all the
  network overhead of microservices, none of the independence.
- When the domain doesn't have a natural seam — splitting a tightly cohesive piece of
  business logic across a network boundary just to look "microservices-y" trades a
  cheap in-process call for an expensive, fallible network one, for no benefit.
- When strong transactional consistency across the split data is a hard requirement —
  microservices push you toward eventual consistency; if the domain can't tolerate
  that, keep the data (and the transaction) in one service.

## 5. Case study: Pipeline Orchestrator + Artifact Registry

- **Purpose:** PipelineForge splits two concerns that used to live in one deployable
  into two independently deployable services. The **Pipeline Orchestrator** owns
  pipeline-run state and decides what happened to a run. The **Artifact Registry** owns
  built artifacts and their metadata (name, version, digest) — nothing else is allowed
  to write that data. When a pipeline run finishes, the Orchestrator calls the Registry
  over HTTP to register the artifact the run produced. Each service can be deployed,
  scaled, and (in a real system) operated by a different team, independently of the
  other.
- **Actors:**
  - **Pipeline Orchestrator service** — receives "this run finished, here's what it
    built" notifications, owns pipeline-run state, and calls the Artifact Registry
    synchronously to register the produced artifact.
  - **Artifact Registry service** — owns artifact metadata; exposes a small HTTP API to
    register an artifact and to look one up by name.
  - **Platform engineer** — runs both services locally, on different ports, and drives
    them with `curl` (or any HTTP client) to watch a real inter-service call happen.
- **Scope of this exercise:** one Maven module, two sub-packages
  (`orchestrator` and `registry`), each with its own `Main` class starting its own
  `com.sun.net.httpserver.HttpServer` on its own port, each with its own in-memory
  store — **no shared Java objects between them**. The only thing the two packages
  agree on is a tiny hand-rolled, non-JSON wire format (`key=value&key=value`, see
  milestone 2) — exactly as two independently-owned real services would agree on an API
  contract without sharing implementation code. Packaging both into one Maven module is
  a **practice-repo simplification** for this repo's "one folder per pattern" layout —
  in a real system these would be two separately deployable/scalable artifacts (see
  milestone 6 for what that means for the release manifest). This exercise models only
  the "a run finished, register what it built" slice of the Orchestrator's lifecycle,
  not full run scheduling — there's no separate "start a run" step.

## 6. Milestones

1. [`00-setup.md`](milestones/00-setup.md) — confirm the module builds, tour the case
   study and the target two-service package layout.
2. [`01-registry-domain.md`](milestones/01-registry-domain.md) — the Artifact Registry's
   own data: the `Artifact` record and the in-memory `ArtifactStore` it alone owns.
3. [`02-registry-http-api.md`](milestones/02-registry-http-api.md) — the hand-rolled
   wire format, the `RegistryRequestHandler` (the pattern's core logic: parse, validate,
   respond), and `RegistryMain` wiring it to a real `HttpServer`.
4. [`03-orchestrator-domain.md`](milestones/03-orchestrator-domain.md) — the
   Orchestrator's own data: `PipelineRun`, `RunStatus`, and the in-memory
   `PipelineRunStore` it alone owns (a *different* store from the Registry's).
5. [`04-orchestrator-registry-client.md`](milestones/04-orchestrator-registry-client.md)
   — `RegistryClient`: the Orchestrator's side of the real HTTP call to the Registry,
   using `java.net.http.HttpClient`.
6. [`05-orchestrator-http-api.md`](milestones/05-orchestrator-http-api.md) —
   `OrchestratorRequestHandler` and `OrchestratorMain`: the Orchestrator's own
   `HttpServer`, wiring the store and the registry client together, and the full
   end-to-end run: two real processes talking over real HTTP.
7. [`06-build-and-release.md`](milestones/06-build-and-release.md) — build, scan, and
   (practice-)release this module — including the two-`DEPLOYABLE`-artifact note.

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
