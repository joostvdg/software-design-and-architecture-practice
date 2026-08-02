# Milestone 5 — Build, scan & release

## Goal

Take the finished module through the same build → SBOM → SonarQube → Snyk →
release-manifest pipeline every pattern in this repo uses.

This milestone doesn't repeat that process — follow **[`../../RELEASE.md`](../../RELEASE.md)**
step by step, substituting:

- `<module-dir>` = `07-cqrs`
- `<slug>` = `cqrs`
- `<artifactId>` = `cqrs`

Before publishing anything, check the result against
**[`../../AGENTS-good-release.md`](../../AGENTS-good-release.md)**'s definition of a
good deployable artifact, and use
**[`../../AGENTS-record-release.md`](../../AGENTS-record-release.md)** for the exact
`app-track-agent` invocations and the `release.manifest.json` schema.

## Module-specific notes

- The entry point for the manifest's jar is `com.isaqb.practice.cqrs.Main`, already
  set as `mainClass` in this module's `pom.xml` — `mvn package` produces a runnable
  `cqrs-<version>.jar`.
- `application_slug` for this module is `isaqb-practice-cqrs`.
- This module has no third-party runtime dependencies, so the SBOM and Snyk scan
  should both come back essentially empty — that's expected, not a sign something's
  missing.

## Checkpoint

- [ ] `mvn -f patterns/07-cqrs/pom.xml clean verify` is green.
- [ ] `target/sbom.cdx.json` exists and parses as JSON.
- [ ] `sonar-scanner` and `snyk test` have both been run from `patterns/07-cqrs/`
      (their pass/fail specifics depend on your local Sonar/Snyk setup, not on this
      guide).
- [ ] `release.manifest.json` exists in `patterns/07-cqrs/`, references a real jar
      digest, and passes `app-track-agent validate-manifest`.

That completes the CQRS pattern. Compare what you built against
[`../README.md`](../README.md) section 3 (trade-offs) — you now have concrete evidence
for each row: the read model you shaped exactly for the Dashboard in milestone 3, the
write side's invariants enforced in one place in milestone 2, and the staleness
trade-off you reasoned through (without having to build it) in milestone 4's thought
experiment.

If you want to keep going, `../08-event-sourcing/` (if present in your checkout) picks
up where this leaves off: instead of a mutable `PipelineRun` aggregate, state becomes an
append-only sequence of events, and this module's `updateProjection` becomes one
possible projection of that stream among several.
