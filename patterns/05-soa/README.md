# SOA (Service-Oriented Architecture)

## 1. What it is

SOA structures a system as a small number of **business-aligned services**, each
exposed through a **stable, versioned contract**, discovered and reused by **multiple,
unrelated consumers** through a central **service registry / catalog** rather than by
consumers constructing or depending on a concrete implementation directly. The building
blocks:

- **Service** — a coarse-grained unit of business capability (e.g. "decide whether a
  deployment may proceed," not "insert a row into the approvals table"). SOA services
  are typically sized around a business capability an enterprise wants to offer, reuse,
  and govern centrally — not around a single team's bounded context.
- **Contract** — the stable, versioned interface (operation signatures + message/DTO
  shapes) a service publishes. Consumers code against the contract, never the
  implementation. The contract, not the implementation, is the thing that must not
  break once consumers depend on it — this is why contracts are versioned (`v1`, `v2`,
  ...) rather than silently changed in place.
- **Service Registry / Catalog** — the governance and discovery mechanism: a central
  place that lists which services exist, under which contract versions, so a consumer
  can look one up by a stable name instead of hard-wiring a reference to one concrete
  class. This is the part that makes "reuse" real rather than aspirational: without a
  catalog, "reuse" degenerates into every consumer importing the same implementation
  class directly, which reintroduces exactly the coupling contracts are meant to avoid.
- **Consumers** — any caller authorized to invoke a service through its contract. Unlike
  Microservices, SOA does not assume consumers and the service they call are owned by
  the same team, or even built at the same time — a defining SOA scenario is an old
  consumer and a new consumer sharing one governed service instance, neither aware of
  the other.

Classic, "textbook" SOA implementations from the 2000s usually added an **Enterprise
Service Bus (ESB)**: a central infrastructure component that routed messages, mediated
protocol/schema differences, and often hosted orchestration logic between services. The
ESB is *not* what makes something SOA — contract-first design and catalog-mediated
reuse are — but the ESB is why SOA carries "legacy baggage" in most architects' minds:
ESBs frequently grew into a new kind of monolith (a shared, hard-to-change, often
proprietary integration layer that every service depended on), which is part of why the
industry moved toward Microservices' much lighter, point-to-point, no-shared-broker
default. This exercise deliberately skips the ESB (see section 5) so you can feel the
part of SOA that's still relevant — contract-first reuse and governance — without the
part that gave it a bad name.

**SOA vs. Microservices, precisely** (see `../03-microservices/README.md` for the full
Microservices writeup): Microservices optimizes for **independent deployability and
team autonomy** — small services, each owned and deployed by one team, communicating
over the network, never sharing a runtime instance. SOA optimizes for **enterprise-wide
reuse of business-aligned services under centralized governance** — typically fewer,
coarser-grained services, potentially consumed by many unrelated teams/applications
that neither own nor deploy the service themselves, discovered through a catalog rather
than each consumer being told out-of-band where to find it. Put differently: in
Microservices, "who can call this service" is usually "whoever we designed it for,
found via service discovery within one deployment platform"; in SOA, "who can call this
service" is often "any authorized consumer across the enterprise that looked it up in
the catalog," including consumers the service's owners never met. Neither pattern
requires networking by definition (this exercise is deliberately in-process — see
section 5) — the defining difference is granularity and governance model, not whether a
network hop is involved.

**SOA vs. Modular Monolith:** a modular monolith (see `../01-layers/README.md`'s "when
not to use" section) also concentrates business logic behind clean internal boundaries,
but those boundaries are *source-code* seams (packages/modules inside one deployable)
with no contract-versioning story and no registry — there's only ever one build, one
set of callers (other code in the same process), and refactoring a module's internals
is a compiler-checked, same-commit change. SOA's contract and catalog exist precisely
*because* its consumers are not all compiled and deployed together — a modular
monolith hasn't earned that machinery yet, and shouldn't build it prematurely (see
section 4).

## 2. Common use cases

- Exposing a core business capability (pricing, approval, provisioning, fraud check,
  ...) for reuse by many otherwise-unrelated applications across an enterprise, instead
  of every application reimplementing or copy-pasting the logic.
- Wrapping and gradually modernizing legacy systems: a stable contract lets consumers
  stop caring whether the implementation behind it is a 20-year-old mainframe job or a
  freshly rewritten service — the contract is the seam a modernization effort can work
  behind.
- Enterprise integration where governance matters: an organization needs one
  authoritative, versioned inventory of "what services exist, what do they do, who owns
  them" rather than tribal knowledge of which team to Slack.
- Cross-application workflows where the same business decision (e.g. "is this change
  approved?") must be evaluated identically no matter which system triggered it —
  centralizing that decision behind one contract avoids divergent, drifting
  reimplementations.

## 3. Trade-offs

| Helps | Hurts |
|---|---|
| Reuse — one governed service, many unrelated consumers, no duplicated business logic | Coarse granularity can bundle multiple concerns behind one contract, weakening cohesion compared to a well-sliced microservice |
| Centralized governance — one catalog shows what exists, which version, discoverable by name | The registry/catalog becomes a coordination bottleneck, and in a networked deployment, a discovery-time single point of failure |
| Contract stability — consumers depend on an interface, so the implementation can change internally without breaking anyone | Contract evolution is hard once many unrelated consumers depend on `v1` — breaking it breaks all of them, so versioning discipline becomes mandatory ceremony, not an afterthought |
| Works well for wrapping/integrating heterogeneous or legacy systems behind one stable seam | Classic ESB-centric SOA implementations added a heavyweight, often proprietary mediation layer whose complexity became notorious ("the ESB as a new monolith") |
| Business-aligned service boundaries map to capabilities architects/stakeholders can reason about at the enterprise level | No inherent story for independent deployability or team autonomy — many consumers can share one service instance/deployment, so one bad change can affect all of them at once, unlike Microservices' fault/deploy isolation |

## 4. When *not* to use it

- When independent deployability per team is the primary goal — use Microservices.
  SOA's instinct (one governed service instance serving many consumers) is nearly the
  opposite of Microservices' instinct (each team deploys and owns its own service,
  independently of everyone else).
- When you don't yet have multiple genuinely independent consumers who'd benefit from a
  shared contract — if there's only one caller, the catalog/registry indirection is
  pure ceremony with no reuse payoff; start with a plain method call or a module inside
  a modular monolith, and extract a real SOA contract only once a second, unrelated
  consumer actually shows up.
- When your organization can't sustain the governance overhead a catalog and versioned
  contracts require — a catalog nobody keeps accurate, or a contract nobody disciplines
  around versioning, gives you all of SOA's coordination cost with none of its reuse
  benefit.
- When you're tempted to reach for a heavyweight ESB just to "do SOA" — most of what an
  ESB promises (mediation, transformation, routing) can be achieved with much simpler
  contract-first design, as this exercise's in-process catalog demonstrates. Adding an
  ESB is a separate infrastructure decision from whether you're doing SOA at all, and
  reaching for one prematurely is how you end up recreating the "ESB as new monolith"
  failure mode the industry moved away from.
- When strict transactional consistency across a single unit of work is required and
  the "service" would just be an in-process call dressed up with catalog indirection —
  the indirection should earn its keep through real reuse or governance need, not be
  added out of pattern-fashion.

## 5. Case study: Deployment Services Catalog

- **Purpose:** PipelineForge exposes two small, coarse-grained, business-aligned
  services through stable, versioned contracts, registered in a central **Service
  Catalog**: a **Deployment Approval Service** (decide whether a deployment may
  proceed) and an **Environment Provisioning Service** (reserve/prepare a target
  environment). Two independent, unrelated callers reuse the *same* service
  implementations purely through their contracts and the catalog lookup — they never
  construct a service directly. This is the opposite instinct from `03-microservices`,
  where the Orchestrator and Registry are independently owned, independently deployed
  services each with their own store: here, the callers don't own or deploy the
  services at all, they just consume a stable, centrally-cataloged contract.
- **Actors:**
  - **Platform engineer** — runs a CLI (`Main`) by hand to check whether one deployment
    may proceed and, if so, provision its target environment.
  - **Scheduled Job** (`ScheduledJobRunner`) — a second, completely independent caller
    simulating an unattended nightly sweep over a fixed batch of pending deployments.
    It looks up and calls the exact same service contracts, through the exact same
    catalog, as the CLI — proving the services are reused, not reimplemented, per
    caller.
  - **Service Catalog** — the governance/discovery mechanism both callers depend on
    instead of depending on `DeploymentApprovalServiceImpl` or
    `EnvironmentProvisioningServiceImpl` directly.
- **Scope of this exercise:** deliberately **in-process, no networking** — a single JVM,
  one Maven module. Real-world SOA very often *does* involve network calls (and
  sometimes an ESB) between consumer and service; this exercise simplifies that away on
  purpose so you can concentrate on the pattern's defining trait — **contract-first
  reuse and governance through a catalog** — without transport-layer machinery
  distracting from it (contrast with `03-microservices`, which *does* use real HTTP,
  because independent deployability is the whole point there). Each service gets a
  `*Request`/`*Response` DTO pair and a small, stable interface (its contract), e.g.
  `DeploymentApprovalService { ApprovalResponse decide(ApprovalRequest request); }`.
  Both callers obtain the catalog from the same `CatalogFactory`, which stands in for
  "the one centrally-maintained catalog" a real deployment would reach over the network
  — a practice-repo simplification, called out again in milestone 5.

## 6. Milestones

1. [`00-setup.md`](milestones/00-setup.md) — confirm the module builds, tour the case
   study and the target package layout.
2. [`01-service-contracts.md`](milestones/01-service-contracts.md) — the DTOs and the
   two stable service interfaces (`DeploymentApprovalService`,
   `EnvironmentProvisioningService`) both services and both callers depend on.
3. [`02-service-catalog.md`](milestones/02-service-catalog.md) — the `ServiceCatalog`
   itself: register/lookup by a `(name, version)` contract key — the pattern's core
   governance/discovery mechanism.
4. [`03-deployment-approval-service.md`](milestones/03-deployment-approval-service.md)
   — `DeploymentApprovalServiceImpl`'s business rule.
5. [`04-environment-provisioning-service.md`](milestones/04-environment-provisioning-service.md)
   — `EnvironmentProvisioningServiceImpl`'s business rule.
6. [`05-callers-and-composition-root.md`](milestones/05-callers-and-composition-root.md)
   — `CatalogFactory`, `Main` (the platform engineer's CLI), and `ScheduledJobRunner`
   (the unattended second caller) — both reusing the same cataloged services.
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
