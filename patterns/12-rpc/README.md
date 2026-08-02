# Remote Procedure Call (RPC)

## 1. What it is

RPC lets a caller invoke a function that runs on a different process — often a
different machine — through a signature that *looks* exactly like a local method
call. `client.reportHeartbeat(health)` reads no differently from any in-process call,
but underneath it serializes the arguments, sends them over the network, waits for a
reply, and deserializes the result. The defining trait of RPC, compared to Broker or
messaging patterns, is this **call semantics**: synchronous (or at least
call-and-await), point-to-point, and shaped like a procedure call rather than an
event being published to whoever happens to be listening.

The defining *danger* of RPC is exactly the same thing that makes it convenient:
**hiding distribution**. A local method call cannot time out, cannot partially fail,
cannot get a "connection refused," and cannot silently take ten seconds because a
network hop is congested. A call that merely *looks* like one of those can do all
four — and code written against the local-call illusion, with no explicit timeout or
retry policy, inherits every one of those failure modes without a plan for any of
them. This is sometimes called the "first fallacy of distributed computing": *the
network is reliable*. RPC's whole ergonomic appeal is built on pretending, at the
call site, that the fallacy is true — which is exactly why timeouts and retries have
to be treated as an explicit **architectural** concern, not an afterthought bolted on
when something times out in production.

## 2. Common use cases

- Request/response calls between services that need a result back before continuing
  (in contrast to fire-and-forget events — see Broker/EDA).
- Internal service-to-service calls where the two sides are versioned and deployed
  together often enough that a stable "interface" contract is worth maintaining
  (gRPC, Java RMI historically, this exercise's hand-rolled HTTP equivalent).
- Health/status checks and other small, latency-sensitive, synchronous queries.

## 3. Trade-offs

| Helps | Hurts |
|---|---|
| Familiar call-site ergonomics — reads like local code, easy to reason about happy-path logic | Hides a real network hop: timeouts, partial failure, and retries must be added explicitly or the "local call" illusion breaks catastrophically under load/outage |
| Strong contract between caller and callee (a method signature) | Tight temporal coupling — the caller blocks waiting for a reply, unlike a publish in Broker/EDA where the publisher doesn't wait for anyone |
| Straightforward request/response mental model for genuinely synchronous needs | Encourages over-use for things that should be async/fire-and-forget, because "just call the method" is the path of least resistance |
| Easy to unit-test the caller against a fake implementing the same interface | A caller and callee evolving independently can silently drift on the contract's meaning even while types still compile (e.g. what a timeout *means* semantically) |

## 4. When *not* to use it

- When the caller doesn't need a reply before proceeding — that's a fire-and-forget
  event, and Broker/messaging patterns decouple the caller from the callee's
  availability in a way RPC's blocking call cannot.
- When the callee might be slow, unreachable, or overloaded and the caller must
  remain responsive regardless — RPC without an explicit timeout turns "the callee is
  slow" into "the caller hangs," which milestone 4 makes concrete.
- When you need many-to-many fan-out (one event, many interested parties) — RPC is
  inherently point-to-point; Publish-Subscribe is the right shape for that.

## 5. Case study: Node Heartbeat RPC

- **Purpose:** Every PipelineForge build-agent Node periodically reports its health to
  the **Fleet Manager**, so the platform knows which nodes are alive and can be
  scheduled work. From the Node Agent's point of view, reporting health should read
  like a single, simple call: "tell the manager I'm healthy." That call is in fact an
  HTTP round trip that can fail or hang — this case study builds the RPC stub that
  hides that round trip, and then (the pattern-defining milestone) makes its hidden
  failure modes visible and handled.
- **Actors:**
  - **Node Agent** — the caller. Wants to report its health without knowing or caring
    that doing so means an HTTP POST under the hood.
  - **Fleet Manager** — the callee. Runs its own `HttpServer`, receives heartbeats,
    keeps the latest health per node in memory.
  - **Platform engineer** *(you)* — builds both sides, and in milestone 4, proves
    what happens to the Node Agent's call when the Fleet Manager is slow to respond.
- **Scope of this exercise:** two independently runnable entry points in one Maven
  module (`FleetManagerMain`, `NodeAgentMain`) — the same "one folder, two real
  processes" practice-repo simplification `03-microservices` uses, for the same
  reason: in a real system these would be two separately deployable processes. Each
  side implements its own hand-rolled `key=value&key=value` wire encode/decode
  independently (no shared Java types for the wire format) — they agree on field
  names only, exactly as `03-microservices`' Orchestrator and Registry do. No TLS, no
  auth, no real service discovery — enough to see RPC's call semantics and its hidden
  network hop clearly, without extra machinery obscuring either.

## 6. Milestones

1. [`00-setup.md`](milestones/00-setup.md) — confirm the module builds, tour the case
   study and the target two-process package layout.
2. [`01-manager-domain.md`](milestones/01-manager-domain.md) — the Fleet Manager's own
   data: `NodeHealth`, `HealthStatus`, and the in-memory `NodeHealthStore` it alone
   owns.
3. [`02-manager-http-api.md`](milestones/02-manager-http-api.md) — the hand-rolled
   wire format (manager side), `FleetManagerRequestHandler`, and `FleetManagerMain`
   wiring it to a real `HttpServer`.
4. [`03-rpc-client-stub.md`](milestones/03-rpc-client-stub.md) — `FleetManagerClient`:
   the RPC stub whose `reportHeartbeat` method looks like a local call but performs a
   real HTTP POST.
5. [`04-timeouts-and-retries.md`](milestones/04-timeouts-and-retries.md) — the
   pattern-defining milestone: explicit timeout and retry-with-backoff, and a test
   proving what happens without them.
6. [`05-node-agent-loop.md`](milestones/05-node-agent-loop.md) — `NodeAgentMain`: a
   periodic heartbeat loop using the client, and the full end-to-end two-process run.
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
