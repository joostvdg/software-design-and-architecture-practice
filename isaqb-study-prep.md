# Architectural Patterns Practice List (CPSA-F)

For each pattern, be able to answer:
1. What problem / context does it address?
2. What is the structure (building blocks + relationships)?
3. Which qualities does it help / hurt?
4. When would you *not* use it?
5. One example from your CI/CD / K8s / past Java work?

Legend: **R1** = exam must-apply · **R3** = know/explain · **Job** = high value for your work

---

## Tier 1 — Master these (R1)

### Layers
- **Understand:** Strict or relaxed layering; allowed dependency direction; what belongs in each layer; difference between logical layers and physical tiers.
- **Why:** Core CPSA pattern; most systems use some form of it. Forces clear thinking about coupling and change impact.

### Pipes and Filters
- **Understand:** Filters as independent processing steps; pipes as data connectors; batch vs stream; error handling and ordering.
- **Why:** R1 pattern; maps directly to CI pipelines, log processing, ETL. Easy to confuse with “workflow” — keep the data-flow essence clear.

### Microservices
- **Understand:** Independently deployable services; data ownership; inter-service communication (sync vs async); operational overhead; when a modular monolith is better.
- **Why:** R1 pattern; you already use the ideas — need precise trade-offs (autonomy vs complexity, consistency, latency).

---

## Tier 2 — Explain confidently (R3, high exam + job value)

### Broker
- **Understand:** Clients talk via an intermediary (message broker / ORB-like); decoupling of location and often of time; failure modes of the broker itself.
- **Why:** Explicit in CPSA list; central to event-driven CI/CD platforms you know.

### SOA (Service-Oriented Architecture)
- **Understand:** Business-aligned services, contracts, reuse across systems; how it differs from microservices (granularity, governance, ESB legacy baggage).
- **Why:** Exam contrast question magnet: SOA vs microservices vs modular monolith.

### Ports and Adapters (Hexagonal / Onion / Clean Architecture)
- **Understand:** Domain at the center; ports as interfaces; adapters for UI, DB, messaging; dependency rule inward.
- **Why:** Formalizes “natural boundaries” and DIP you’ve used in practice; excellent for explaining modularity.

### CQRS
- **Understand:** Separate write model (commands) from read model (queries); eventual consistency; when complexity is justified.
- **Why:** On CPSA list; common in scalable platforms and audit-heavy systems.

### Event Sourcing
- **Understand:** State as sequence of events; rebuild/projections; debugging and audit benefits; snapshotting; “not the same as event-driven.”
- **Why:** Often paired with CQRS; useful for pipeline/history/audit domains.

### Plugin
- **Understand:** Core + extension points; stable interfaces; discovery/loading; versioning and isolation of plugins.
- **Why:** Directly maps to CI tools, pipeline steps, K8s operators/extensions.

### MVC / MVVM / MVU / PAC (UI architectures)
- **Understand:** Separation of presentation vs domain; who owns state; update flow; which variant fits which UI style.
- **Why:** CPSA expects awareness; enough to explain structure and qualities, not deep frontend expertise.

### Dependency Injection
- **Understand:** Inversion of control for dependencies; wiring at composition root; testability; relation to DIP (principle vs pattern/mechanism).
- **Why:** Bridging design principle ↔ runtime structure; appears under architectural patterns in curriculum.

### Remote Procedure Call (RPC)
- **Understand:** Call semantics over the network; hiding distribution (and the danger of that); sync coupling; timeouts/retries as architectural concerns.
- **Why:** Contrast with messaging; clarifies distributed-systems coupling you’ve felt in platforms.

---

## Tier 3 — Know well enough to discuss (R3 / adjacent)

### Blackboard
- **Understand:** Shared knowledge space + specialists that opportunistically contribute; good for unclear solution strategies.
- **Why:** On CPSA list; rare in your day job but good “explain with example” exam coverage.

### Client–Server
- **Understand:** Request/response roles; thin vs thick client; scaling and stateful server issues.
- **Why:** Foundation pattern behind most networked systems; often assumed, rarely named.

### Peer-to-Peer
- **Understand:** Nodes as equals; discovery; consistency and trust challenges.
- **Why:** Contrast pattern vs client–server; useful conceptual clarity.

### Event-Driven Architecture (EDA)
- **Understand:** Events as facts; producers/consumers; choreography vs orchestration; coupling via event schemas.
- **Why:** Your CI/CD world; map Hohpe messaging patterns onto CPSA vocabulary.

### Messaging / Integration patterns (Hohpe selection)
Practice these specifically:
- **Publish–Subscribe** — one event, many consumers; fan-out.
- **Point-to-Point / Competing Consumers** — work queue; load distribution.
- **Content-Based Router** — route by message content.
- **Splitter / Aggregator** — break and recombine work.
- **Dead Letter Channel** — handle poison/failed messages.
- **Canonical Data Model** (optional) — shared integration language vs translator per pair.
- **Why:** Curriculum explicitly points at EIP; these are your strongest “prior art” to formalize.

### Modular Monolith
- **Understand:** Single deployable with strong module boundaries; stepping stone to microservices; enforcement of boundaries.
- **Why:** Not always named in CPSA lists, but critical for “small → large” growth — your stated weak spot.

### Sidecar / Ambassador / Adapter (deployment-adjacent)
- **Understand:** Companion process for cross-cutting concerns (proxy, mesh, log shipper); separation without changing app code.
- **Why:** Job-relevant on Kubernetes; connects cross-cutting concerns to structure. Treat as supporting patterns, not core CPSA R1.

---

## Practice worksheet (copy per pattern)
