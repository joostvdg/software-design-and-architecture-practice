# Milestone 5 — Build, scan & release

## Goal

Take the finished module through the same build → SBOM → SonarQube → Snyk →
release-manifest pipeline every pattern in this repo uses.

This milestone doesn't repeat that process — follow **[`../../RELEASE.md`](../../RELEASE.md)**
step by step, substituting:

- `<module-dir>` = `11-dependency-injection`
- `<slug>` = `dependency-injection`
- `<artifactId>` = `dependency-injection`

Before publishing anything, check the result against
**[`../../AGENTS-good-release.md`](../../AGENTS-good-release.md)**'s definition of a
good deployable artifact, and use
**[`../../AGENTS-record-release.md`](../../AGENTS-record-release.md)** for the exact
`app-track-agent` invocations and the `release.manifest.json` schema.

## Module-specific notes

- The entry point for the manifest's jar is `com.isaqb.practice.di.Main`, already set
  as `mainClass` in this module's `pom.xml` — `mvn package` produces a runnable
  `dependency-injection-<version>.jar`.
- `application_slug` for this module is `isaqb-practice-dependency-injection`.
- This module has no third-party runtime dependencies (no DI framework, per the
  repo-wide constraint — see `../PATTERN-TEMPLATE.md`), so the SBOM and Snyk scan
  should both come back essentially empty — that's expected, not a sign something's
  missing.

## Checkpoint

- [ ] `mvn -f patterns/11-dependency-injection/pom.xml clean verify` is green.
- [ ] `target/sbom.cdx.json` exists and parses as JSON.
- [ ] `sonar-scanner` and `snyk test` have both been run from
      `patterns/11-dependency-injection/` (their pass/fail specifics depend on your
      local Sonar/Snyk setup, not on this guide).
- [ ] `release.manifest.json` exists in `patterns/11-dependency-injection/`,
      references a real jar digest, and passes `app-track-agent validate-manifest`.

That completes the Dependency Injection pattern. Compare what you built against
[`../README.md`](../README.md) section 3 (trade-offs) — you now have concrete
evidence: you *felt* the testability payoff in milestone 2 (testing per-channel
failure isolation with `RecordingChannel`, no real Slack/email involved), and the
"swap without touching the core" payoff in milestone 4's `CompositionRootTest`, which
rewired `ReleaseNotifier` twice without ever opening its source file.
