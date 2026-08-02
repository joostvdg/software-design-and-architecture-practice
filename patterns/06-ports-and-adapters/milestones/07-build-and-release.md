# Milestone 7 — Build, scan & release

## Goal

Take the finished module through the same build → SBOM → SonarQube → Snyk →
release-manifest pipeline every pattern in this repo uses.

This milestone doesn't repeat that process — follow **[`../../RELEASE.md`](../../RELEASE.md)**
step by step, substituting:

- `<module-dir>` = `06-ports-and-adapters`
- `<slug>` = `ports-and-adapters`
- `<artifactId>` = `ports-and-adapters`

Before publishing anything, check the result against
**[`../../AGENTS-good-release.md`](../../AGENTS-good-release.md)**'s definition of a
good deployable artifact, and use
**[`../../AGENTS-record-release.md`](../../AGENTS-record-release.md)** for the exact
`app-track-agent` invocations and the `release.manifest.json` schema.

## Module-specific notes

- The entry point for the manifest's jar is
  `com.isaqb.practice.portsandadapters.adapter.driving.cli.Main`, already set as
  `mainClass` in this module's `pom.xml` — `mvn package` produces a runnable
  `ports-and-adapters-<version>.jar`.
- `application_slug` for this module is `isaqb-practice-ports-and-adapters`.
- This module has no third-party runtime dependencies, so the SBOM and Snyk scan
  should both come back essentially empty — that's expected, not a sign something's
  missing.

## Checkpoint

- [ ] `mvn -f patterns/06-ports-and-adapters/pom.xml clean verify` is green.
- [ ] `target/sbom.cdx.json` exists and parses as JSON.
- [ ] `sonar-scanner` and `snyk test` have both been run from
      `patterns/06-ports-and-adapters/` (their pass/fail specifics depend on your local
      Sonar/Snyk setup, not on this guide).
- [ ] `release.manifest.json` exists in `patterns/06-ports-and-adapters/`, references a
      real jar digest, and passes `app-track-agent validate-manifest`.

That completes the Ports and Adapters pattern. Compare what you built against
[`../README.md`](../README.md) section 3 (trade-offs) — you now have concrete
evidence for each row: you *felt* the testability benefit in milestone 3 (a fully
tested use case before any adapter existed), and the "driven technology is swappable
by construction" benefit in milestone 6, where the same contract test proved two
unrelated storage technologies both satisfy `core`'s one port.
