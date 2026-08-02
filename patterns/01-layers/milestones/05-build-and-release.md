# Milestone 5 — Build, scan & release

## Goal

Take the finished module through the same build → SBOM → SonarQube → Snyk →
release-manifest pipeline every pattern in this repo uses.

This milestone doesn't repeat that process — follow **[`../../RELEASE.md`](../../RELEASE.md)**
step by step, substituting:

- `<module-dir>` = `01-layers`
- `<slug>` = `layers`
- `<artifactId>` = `layers`

Before publishing anything, check the result against
**[`../../AGENTS-good-release.md`](../../AGENTS-good-release.md)**'s definition of a
good deployable artifact, and use
**[`../../AGENTS-record-release.md`](../../AGENTS-record-release.md)** for the exact
`app-track-agent` invocations and the `release.manifest.json` schema.

## Module-specific notes

- The entry point for the manifest's jar is `com.isaqb.practice.layers.Main`, already
  set as `mainClass` in this module's `pom.xml` — `mvn package` produces a runnable
  `layers-<version>.jar`.
- `application_slug` for this module is `isaqb-practice-layers`.
- This module has no third-party runtime dependencies, so the SBOM and Snyk scan
  should both come back essentially empty — that's expected, not a sign something's
  missing. It's still worth generating them: it proves the pipeline itself works
  before a later, dependency-carrying pattern (e.g. `03-microservices`) makes the SBOM
  actually interesting.

## Checkpoint

- [ ] `mvn -f patterns/01-layers/pom.xml clean verify` is green.
- [ ] `target/sbom.cdx.json` exists and parses as JSON.
- [ ] `sonar-scanner` and `snyk test` have both been run from `patterns/01-layers/`
      (their pass/fail specifics depend on your local Sonar/Snyk setup, not on this
      guide).
- [ ] `release.manifest.json` exists in `patterns/01-layers/`, references a real jar
      digest, and passes `app-track-agent validate-manifest`.

That completes the Layers pattern. Compare what you built against
[`../README.md`](../README.md) section 3 (trade-offs) — you now have concrete evidence
for each row: you *felt* the testability benefit in milestone 2's tests (no filesystem
needed), and the "change isolation" benefit in milestone 4's checkpoint (swapping
`FileConfigSource` touches one line).
