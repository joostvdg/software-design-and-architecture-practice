# Milestone 6 — Build, scan & release

## Goal

Take the finished module through the same build → SBOM → SonarQube → Snyk →
release-manifest pipeline every pattern in this repo uses.

This milestone doesn't repeat that process — follow **[`../../RELEASE.md`](../../RELEASE.md)**
step by step, substituting:

- `<module-dir>` = `03-microservices`
- `<slug>` = `microservices`
- `<artifactId>` = `microservices`

Before publishing anything, check the result against
**[`../../AGENTS-good-release.md`](../../AGENTS-good-release.md)**'s definition of a
good deployable artifact, and use
**[`../../AGENTS-record-release.md`](../../AGENTS-record-release.md)** for the exact
`app-track-agent` invocations and the `release.manifest.json` schema.

## Module-specific notes

- `application_slug` for this module is `isaqb-practice-microservices`.
- **There is no single runnable jar `mainClass` for this module**, on purpose — this
  module's `pom.xml` deliberately omits the `maven-jar-plugin` manifest configuration
  that other patterns' `pom.xml` set. `mvn package` still produces
  `microservices-<version>.jar`, but it has no `Main-Class` manifest entry, because
  there is no single main class: there are two independent entry points, one per
  service. Run each one explicitly off the classpath:

  ```bash
  # Artifact Registry, port 8081
  java -cp target/classes com.isaqb.practice.microservices.registry.RegistryMain 8081

  # Pipeline Orchestrator, port 8080, pointed at the Registry
  java -cp target/classes com.isaqb.practice.microservices.orchestrator.OrchestratorMain 8080 http://localhost:8081
  ```

  (Substitute `target/microservices-<version>.jar` on the classpath instead of
  `target/classes` if you built the jar with `mvn package` rather than `mvn compile`.)

- **Two `DEPLOYABLE` artifacts, not one.** This repo's "one folder per pattern"
  constraint means both services live in a single Maven module — a practice-repo
  simplification, spelled out in this module's `README.md` §5. It does **not** mean
  they're one deployable in reality. If you were writing this module's
  `release.manifest.json` for a real release (following
  `../../AGENTS-record-release.md`'s schema), you would want **two** entries in
  `artifacts`, one per service — e.g. two `OCI_IMAGE` (or `BINARY`) rows, each with its
  own `name` (`pipeline-orchestrator`, `artifact-registry`), its own digest, and its own
  `uri`, both under the same `application_slug`/`version` if you're releasing them
  together, or under two different `application_slug`s if the Orchestrator and Registry
  are versioned and released independently in your real system — which, per the
  README's definition of independent deployability, is the more realistic setup. Either
  way, one artifact row that bundles both services' bytes together would misrepresent
  what actually gets deployed: two separate processes, on two separate schedules.
- This module has no third-party runtime dependencies (`java.net.http.HttpClient` and
  `com.sun.net.httpserver.HttpServer` ship with the JDK), so the SBOM and Snyk scan
  should come back essentially empty here too — same as `01-layers`. That's expected,
  not a gap.

## Checkpoint

- [ ] `mvn -f patterns/03-microservices/pom.xml clean verify` is green.
- [ ] `target/sbom.cdx.json` exists and parses as JSON.
- [ ] `sonar-scanner` and `snyk test` have both been run from `patterns/03-microservices/`
      (their pass/fail specifics depend on your local Sonar/Snyk setup, not on this
      guide).
- [ ] `release.manifest.json` exists in `patterns/03-microservices/`, references real
      jar digest(s) (see the two-artifact note above for what "real" means here), and
      passes `app-track-agent validate-manifest`.

That completes the Microservices pattern. Compare what you built against
[`../README.md`](../README.md) §3 (trade-offs) — you now have concrete evidence for
several rows: the fault-isolation and partial-failure rows from milestone 5's `502`
case, the data-ownership row from two separate stores never once referencing each
other's types, and the operational-overhead row from just how much more machinery two
tiny services needed compared to `01-layers`' single process.
