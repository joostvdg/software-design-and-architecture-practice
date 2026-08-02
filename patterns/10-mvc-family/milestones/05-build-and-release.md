# Milestone 5 — Build, scan & release

## Goal

Take the finished module through the same build → SBOM → SonarQube → Snyk →
release-manifest pipeline every pattern in this repo uses.

This milestone doesn't repeat that process — follow **[`../../RELEASE.md`](../../RELEASE.md)**
step by step, substituting:

- `<module-dir>` = `10-mvc-family`
- `<slug>` = `mvc-family`
- `<artifactId>` = `mvc-family`

Before publishing anything, check the result against
**[`../../AGENTS-good-release.md`](../../AGENTS-good-release.md)**'s definition of a
good deployable artifact, and use
**[`../../AGENTS-record-release.md`](../../AGENTS-record-release.md)** for the exact
`app-track-agent` invocations and the `release.manifest.json` schema.

## Module-specific notes

- The entry point for the manifest's jar is `com.isaqb.practice.mvcfamily.Main`,
  already set as `mainClass` in this module's `pom.xml` — `mvn package` produces a
  runnable `mvc-family-<version>.jar`. Note this jar reads from stdin interactively;
  that's fine for a practice release (the manifest records provenance, not usage
  mode), just don't expect it to do anything useful piped/backgrounded without input.
- `application_slug` for this module is `isaqb-practice-mvc-family`.
- This module has no third-party runtime dependencies, so the SBOM and Snyk scan
  should both come back essentially empty — that's expected, not a sign something's
  missing.

## Checkpoint

- [ ] `mvn -f patterns/10-mvc-family/pom.xml clean verify` is green.
- [ ] `target/sbom.cdx.json` exists and parses as JSON.
- [ ] `sonar-scanner` and `snyk test` have both been run from `patterns/10-mvc-family/`
      (their pass/fail specifics depend on your local Sonar/Snyk setup, not on this
      guide).
- [ ] `release.manifest.json` exists in `patterns/10-mvc-family/`, references a real
      jar digest, and passes `app-track-agent validate-manifest`.

That completes the MVC (family) pattern. Compare what you built against
[`../README.md`](../README.md) section 3 (trade-offs) — you now have concrete
evidence: you *felt* the Model's UI-independent testability in milestone 1's tests
(zero stdin, zero rendering involved), and the "explicit render call is easy to
forget" risk in milestone 3, where forgetting the `view.render(model)` call at the end
of any `DashboardController` method would have made `refresh()` return stale data —
exactly the failure mode MVVM's data-binding exists to prevent.
