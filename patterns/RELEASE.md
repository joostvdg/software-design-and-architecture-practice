# Build, scan & release workflow (shared by every pattern module)

Every pattern's final milestone (`0N-build-and-release.md`) links here instead of
repeating these steps. This is the same sequence for all 12 modules — only the
`<module-dir>`, `<slug>`, and `<artifactId>` change.

It builds on top of the two release-process references already in the repo root:

- **`../AGENTS-good-release.md`** — what makes a deployable artifact *good*
  (content-addressed, locatable, tied to source, inventoried via SBOM, attributable,
  minimal privilege, no secrets). Check your release against its checklist before you
  consider it done.
- **`../AGENTS-record-release.md`** — how to record a release with `app-track-agent`,
  including the `release.manifest.json` schema used in step 6 below.

## Preconditions

- `mvn`, `sonar-scanner`, and `snyk` are on `PATH`.
- You're inside the git repo (release manifest needs a commit SHA).
- (Only needed if you're actually publishing to app-track, not just practicing the
  steps): `app-track-agent` is installed and your Publisher principal is bound, per
  `../AGENTS-record-release.md`'s preconditions.

## Steps

Run from the repo root, substituting your module's directory and slug.

### 1. Build, test, cover

```bash
mvn -f patterns/<module-dir>/pom.xml clean verify
```

This compiles `src/main/java`, runs the JUnit 5 tests in `src/test/java`, and produces
a JaCoCo coverage report at `patterns/<module-dir>/target/site/jacoco/jacoco.xml`
(consumed by Sonar in step 3). A release should never be cut from a module where this
fails.

### 2. Generate the SBOM

```bash
mvn -f patterns/<module-dir>/pom.xml org.cyclonedx:cyclonedx-maven-plugin:makeBom
```

Produces `patterns/<module-dir>/target/sbom.cdx.json` (CycloneDX JSON) — this is the
`SBOM` evidence file required in the manifest (step 6) and by
`AGENTS-good-release.md`'s "inventoried" requirement.

### 3. SonarQube scan

```bash
cd patterns/<module-dir>
sonar-scanner
cd -
```

Uses that module's `sonar-project.properties`. Requires `SONAR_HOST_URL` and
`SONAR_TOKEN` (or `sonar.host.url` / `sonar.token` passed as `-D` flags) to be set in
your environment.

### 4. Snyk scan

```bash
cd patterns/<module-dir>
snyk test --file=pom.xml
cd -
```

Since these modules have no runtime dependencies beyond the JDK, this mostly guards
against a milestone accidentally introducing one. Run `snyk code test` as well if you
want static analysis on the module's own source.

### 5. Compute the artifact digest

```bash
sha256sum patterns/<module-dir>/target/<artifactId>-<version>.jar
```

This is the `DEPLOYABLE` artifact's digest for the manifest. For a real deploy target
you'd publish the jar somewhere content-addressable (an OCI artifact, a package
registry, an HTTPS URL with the digest embedded) and use that URI; for local practice
a `file://` URI to the built jar is acceptable — just don't call it production-ready
per `AGENTS-good-release.md`'s "locatable" requirement, which expects a stable fetch
location, not a local build path.

### 6. Write the release manifest

Create `patterns/<module-dir>/release.manifest.json` following the schema in
`AGENTS-record-release.md`:

```json
{
  "application_slug": "isaqb-practice-<slug>",
  "version": "1.0.0",
  "idempotency_key": "<unique-per-attempt, e.g. a UUID or the git commit sha>",
  "artifacts": [
    {
      "role": "DEPLOYABLE",
      "artifact_type": "BINARY",
      "name": "<artifactId>",
      "digest": "sha256:<from step 5>",
      "uri": "file://<absolute path to the jar>"
    }
  ],
  "source": {
    "scm_uri": "<this repo's clone URL>",
    "commit_sha": "<from `git rev-parse HEAD`>",
    "tag": "isaqb-practice-<slug>-v1.0.0"
  },
  "evidence_files": [
    {
      "evidence_type": "SBOM",
      "path": "./target/sbom.cdx.json",
      "content_type": "application/vnd.cyclonedx+json"
    }
  ],
  "ci_metadata_components": []
}
```

### 7. Validate and (optionally) publish

```bash
cd patterns/<module-dir>
app-track-agent validate-manifest --manifest ./release.manifest.json
app-track-agent publish --manifest ./release.manifest.json --dry-run   # inspect first
app-track-agent publish --manifest ./release.manifest.json             # actually record
cd -
```

Only run the non-`--dry-run` publish if you actually intend to record this as a real
release in an app-track instance you control — for most practice runs, stopping after
`validate-manifest` (or the `--dry-run` publish) is enough to prove the pipeline works
end to end.

## Before you call it done

Walk `AGENTS-good-release.md`'s checklist:

- [ ] At least one `DEPLOYABLE` with a `sha256:…` digest and a URI
- [ ] `source.scm_uri` + `commit_sha` match what you actually built
- [ ] The CycloneDX SBOM file exists and parses
- [ ] The version is new (not reused with different content)
- [ ] Manifest validates
- [ ] No secrets anywhere in the artifact, SBOM, or manifest
