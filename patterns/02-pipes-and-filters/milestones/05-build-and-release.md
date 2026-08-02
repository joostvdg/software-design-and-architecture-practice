# Milestone 5 — Build, scan & release

## Goal

Take the finished module through the same build → SBOM → SonarQube → Snyk →
release-manifest pipeline every pattern in this repo uses.

This milestone doesn't repeat that process — follow **[`../../RELEASE.md`](../../RELEASE.md)**
step by step, substituting:

- `<module-dir>` = `02-pipes-and-filters`
- `<slug>` = `pipes-and-filters`
- `<artifactId>` = `pipes-and-filters`

Before publishing anything, check the result against
**[`../../AGENTS-good-release.md`](../../AGENTS-good-release.md)**'s definition of a
good deployable artifact, and use
**[`../../AGENTS-record-release.md`](../../AGENTS-record-release.md)** for the exact
`app-track-agent` invocations and the `release.manifest.json` schema.

## Module-specific notes

- The entry point for the manifest's jar is
  `com.isaqb.practice.pipesandfilters.Main`, already set as `mainClass` in this
  module's `pom.xml` — `mvn package` produces a runnable
  `pipes-and-filters-<version>.jar`.
- `application_slug` for this module is `isaqb-practice-pipes-and-filters`.
- This module has no third-party runtime dependencies, so the SBOM and Snyk scan
  should both come back essentially empty — that's expected, not a sign something's
  missing.

## Checkpoint

- [ ] `mvn -f patterns/02-pipes-and-filters/pom.xml clean verify` is green.
- [ ] `target/sbom.cdx.json` exists and parses as JSON.
- [ ] `sonar-scanner` and `snyk test` have both been run from
      `patterns/02-pipes-and-filters/` (their pass/fail specifics depend on your local
      Sonar/Snyk setup, not on this guide).
- [ ] `release.manifest.json` exists in `patterns/02-pipes-and-filters/`, references a
      real jar digest, and passes `app-track-agent validate-manifest`.

That completes the Pipes and Filters pattern. Compare what you built against
[`../README.md`](../README.md) section 3 (trade-offs) — you now have concrete evidence
for each row: you *felt* the independent-testability benefit every time you wrote a
filter's test without touching any other filter, and the reorderability benefit in
milestone 4's checkpoint (swapping the order of two filters in `Main` without changing
either filter's class).
