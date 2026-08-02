# Milestone 6 — Build, scan & release

## Goal

Take the finished module through the same build → SBOM → SonarQube → Snyk →
release-manifest pipeline every pattern in this repo uses.

This milestone doesn't repeat that process — follow **[`../../RELEASE.md`](../../RELEASE.md)**
step by step, substituting:

- `<module-dir>` = `08-event-sourcing`
- `<slug>` = `event-sourcing`
- `<artifactId>` = `event-sourcing`

Before publishing anything, check the result against
**[`../../AGENTS-good-release.md`](../../AGENTS-good-release.md)**'s definition of a
good deployable artifact, and use
**[`../../AGENTS-record-release.md`](../../AGENTS-record-release.md)** for the exact
`app-track-agent` invocations and the `release.manifest.json` schema.

## Module-specific notes

- The entry point for the manifest's jar is `com.isaqb.practice.eventsourcing.Main`,
  already set as `mainClass` in this module's `pom.xml` — `mvn package` produces a
  runnable `event-sourcing-<version>.jar`.
- `application_slug` for this module is `isaqb-practice-event-sourcing`.
- This module has no third-party runtime dependencies, so the SBOM and Snyk scan
  should both come back essentially empty — that's expected, not a sign something's
  missing.

## Checkpoint

- [ ] `mvn -f patterns/08-event-sourcing/pom.xml clean verify` is green.
- [ ] `target/sbom.cdx.json` exists and parses as JSON.
- [ ] `sonar-scanner` and `snyk test` have both been run from
      `patterns/08-event-sourcing/` (their pass/fail specifics depend on your local
      Sonar/Snyk setup, not on this guide).
- [ ] `release.manifest.json` exists in `patterns/08-event-sourcing/`, references a real
      jar digest, and passes `app-track-agent validate-manifest`.

That completes the Event Sourcing pattern. Compare what you built against
[`../README.md`](../README.md) section 3 (trade-offs) — you now have concrete evidence
for each row: you *felt* the audit-trail benefit in the Auditor's per-event replay
(milestone 5), the "new projections without touching the writer" benefit in how
`Dashboard` and `Auditor` both read the same log without `PipelineRunSimulator` ever
knowing either exists, and the snapshotting trade-off in milestone 4's equivalence
test — a cached checkpoint that changes *how much work* a replay does, never *what
answer* it gives.
