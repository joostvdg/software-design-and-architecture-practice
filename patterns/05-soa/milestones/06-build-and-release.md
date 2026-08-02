# Milestone 6 — Build, scan & release

## Goal

Take the finished module through the same build → SBOM → SonarQube → Snyk →
release-manifest pipeline every pattern in this repo uses.

This milestone doesn't repeat that process — follow **[`../../RELEASE.md`](../../RELEASE.md)**
step by step, substituting:

- `<module-dir>` = `05-soa`
- `<slug>` = `soa`
- `<artifactId>` = `soa`

Before publishing anything, check the result against
**[`../../AGENTS-good-release.md`](../../AGENTS-good-release.md)**'s definition of a
good deployable artifact, and use
**[`../../AGENTS-record-release.md`](../../AGENTS-record-release.md)** for the exact
`app-track-agent` invocations and the `release.manifest.json` schema.

## Module-specific notes

- The entry point for the manifest's jar is `com.isaqb.practice.soa.Main` (the platform
  engineer's CLI), already set as `mainClass` in this module's `pom.xml` — `mvn package`
  produces a runnable `soa-<version>.jar`. `ScheduledJobRunner` is a second entry point
  in the same jar, not wired into the manifest — run it via
  `java -cp target/classes com.isaqb.practice.soa.ScheduledJobRunner` as in milestone 5,
  step 6.
- `application_slug` for this module is `isaqb-practice-soa`.
- This module has no third-party runtime dependencies, so the SBOM and Snyk scan should
  both come back essentially empty — that's expected, not a sign something's missing.

## Checkpoint

- [ ] `mvn -f patterns/05-soa/pom.xml clean verify` is green.
- [ ] `target/sbom.cdx.json` exists and parses as JSON.
- [ ] `sonar-scanner` and `snyk test` have both been run from `patterns/05-soa/` (their
      pass/fail specifics depend on your local Sonar/Snyk setup, not on this guide).
- [ ] `release.manifest.json` exists in `patterns/05-soa/`, references a real jar digest,
      and passes `app-track-agent validate-manifest`.

That completes the SOA pattern. Compare what you built against
[`../README.md`](../README.md) sections 3–4 (trade-offs, when not to use it) — you now
have concrete evidence for the reuse claim: milestone 5's `Main` and
`ScheduledJobRunner` are two completely independent callers, yet neither one names
`DeploymentApprovalServiceImpl` or `EnvironmentProvisioningServiceImpl` — both went
through the same `ServiceCatalog`, under the same `"v1"` contract, and got the same
governed behavior. Then compare against `../03-microservices/README.md`: notice that
nothing here was independently deployed, no service owned its own datastore, and no
network call was made — the granularity and governance model, not the transport, is
what made this SOA rather than Microservices.
