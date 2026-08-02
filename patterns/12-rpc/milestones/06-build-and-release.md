# Milestone 6 — Build, scan & release

## Goal

Take the finished module through the same build → SBOM → SonarQube → Snyk →
release-manifest pipeline every pattern in this repo uses.

This milestone doesn't repeat that process — follow **[`../../RELEASE.md`](../../RELEASE.md)**
step by step, substituting:

- `<module-dir>` = `12-rpc`
- `<slug>` = `rpc`
- `<artifactId>` = `rpc`

Before publishing anything, check the result against
**[`../../AGENTS-good-release.md`](../../AGENTS-good-release.md)**'s definition of a
good deployable artifact, and use
**[`../../AGENTS-record-release.md`](../../AGENTS-record-release.md)** for the exact
`app-track-agent` invocations and the `release.manifest.json` schema.

## Module-specific notes

- This module has **two** runnable entry points, not one —
  `com.isaqb.practice.rpc.manager.FleetManagerMain` and
  `com.isaqb.practice.rpc.agent.NodeAgentMain` — which is why `pom.xml` deliberately
  has no `mainClass` manifest entry (see milestone 0). `mvn package` still produces a
  single `rpc-<version>.jar` containing both; which class you run with `java -cp` is
  a runtime choice, same as `03-microservices`.
- For the release manifest (`RELEASE.md` step 6), record the one jar as a single
  `DEPLOYABLE` artifact — same practice-repo simplification `03-microservices` uses,
  since this repo packages both processes' code into one module for the "one folder
  per pattern" layout. In a real system, the Fleet Manager and the Node Agent would
  very likely ship as two separate artifacts (a server image and an agent binary,
  respectively).
- `application_slug` for this module is `isaqb-practice-rpc`.
- This module has no third-party runtime dependencies, so the SBOM and Snyk scan
  should both come back essentially empty — that's expected, not a sign something's
  missing.

## Checkpoint

- [ ] `mvn -f patterns/12-rpc/pom.xml clean verify` is green.
- [ ] `target/sbom.cdx.json` exists and parses as JSON.
- [ ] `sonar-scanner` and `snyk test` have both been run from `patterns/12-rpc/`
      (their pass/fail specifics depend on your local Sonar/Snyk setup, not on this
      guide).
- [ ] `release.manifest.json` exists in `patterns/12-rpc/`, references a real jar
      digest, and passes `app-track-agent validate-manifest`.

That completes the RPC pattern. Compare what you built against
[`../README.md`](../README.md) section 3 (trade-offs) — you now have concrete
evidence: you *felt* the familiar call-site ergonomics in milestone 3
(`reportHeartbeat` reading like any other method), and the hidden-network-hop danger
in milestone 4, where the same call, against a hung server, would have blocked
indefinitely without an explicit timeout — the exact gap between "looks like a local
call" and "is a network call" that makes RPC's convenience also its biggest risk.
