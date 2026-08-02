# Milestone 0 — Setup

## Goal

Confirm the module builds, understand the target two-service package layout, and get
oriented in the case study before writing any pattern code.

## Confirm the build is green

From the repo root:

```bash
mvn -f patterns/03-microservices/pom.xml clean verify
```

This should pass — right now the module only contains a placeholder test
(`SmokeTest`) and an empty `package-info.java`. That placeholder test is there so you
always have a green build to come back to; delete it once the registry package (next
milestone) has its own tests.

## Target layout

By the end of milestone 5 you'll have:

```
src/main/java/com/isaqb/practice/microservices/
  registry/
    Artifact.java                  # Registry-owned data: name, version, digest
    ArtifactStore.java             # Registry's in-memory store - it alone writes this
    ArtifactWireFormat.java        # Hand-rolled, non-JSON wire format (encode/decode)
    HttpResult.java                # (status, body) pair a request handler returns
    RegistryRequestHandler.java    # Core logic: parse request, validate, respond
    RegistryMain.java              # Composition root + HttpServer for the Registry
  orchestrator/
    RunStatus.java                 # RUNNING / COMPLETED
    PipelineRun.java                # Orchestrator-owned data: a completed run's record
    PipelineRunStore.java          # Orchestrator's in-memory store - a *different* one
    HttpResult.java                # Same shape as the registry's, deliberately not shared
    RegistryClient.java            # Orchestrator's HTTP client for calling the Registry
    OrchestratorRequestHandler.java
    OrchestratorMain.java          # Composition root + HttpServer for the Orchestrator
```

Notice there are **two `HttpResult` classes and two independent stores** — one pair per
package. That's not an oversight to "fix" by extracting a shared base class. In a real
system, `registry` and `orchestrator` would be two separate repositories built and
deployed by (possibly) two different teams; the only thing they'd agree on is the wire
contract (documented in milestone 2), never a shared Java type carrying business data.
Duplicating two tiny records is a small price for that independence — that trade-off
*is* the pattern.

## The case study, one more time

You're building two services from PipelineForge:

- The **Artifact Registry** owns build artifacts: their `name`, `version`, and
  content `digest`. It exposes a tiny HTTP API: register one, look one up by name.
- The **Pipeline Orchestrator** owns pipeline-run state. When a run finishes, it calls
  the Registry over HTTP to register the artifact that run produced.

Both run as separate `HttpServer` processes on separate ports, with separate in-memory
state — no shared Java objects, no shared database, no import from one package's
internals into the other's. The only inter-service coupling is the HTTP wire contract
you'll design in milestone 2.

## Checkpoint

- [ ] `mvn -f patterns/03-microservices/pom.xml clean verify` passes.
- [ ] You can explain, in one sentence, why `registry` and `orchestrator` each get
      their own `HttpResult` record instead of sharing one.

Next: [`01-registry-domain.md`](01-registry-domain.md).
