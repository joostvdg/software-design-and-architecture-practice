# AGENTS: Record a release with app-track

> **Copy this file** into any project where an AI agent should publish release provenance to app-track.
> Companion: `AGENTS-good-release.md` (quality bar for artifacts). CLI docs: keep `app-track-agent` on `PATH` (see the app-track `agent/README.md`).

## Goal

When you produce a shippable version of software, **record it** in app-track so others can prove what was built, from which commit, with which SBOM, without trusting mutable registry tags alone.

## Preconditions

1. `app-track-agent` is installed and on `PATH` (`which app-track-agent`).
2. An app-track API is reachable (`APP_TRACK_URL`, default `http://127.0.0.1:8080`).
3. The target **application** exists and your Publisher principal is bound (once per app):

```bash
app-track-agent doctor
app-track-agent register-app --name "<Name>" --slug "<slug>" --org-slug fixture-team
app-track-agent bind-publisher --application-slug "<slug>"
```

Use project-specific API keys via env vars when not using DEV fixtures:
`APP_TRACK_ORGADMIN_KEY`, `APP_TRACK_PUBLISHER_KEY`, `APP_TRACK_VIEWER_KEY`.

## Required workflow (do this every release)

1. **Build** the deployable artifact(s) and compute their **content digests** (`sha256:<hex>`).
2. **Generate a CycloneDX JSON SBOM** for the release (file on disk).
3. **Write a release manifest** JSON (schema below) next to the SBOM.
4. **Validate**, then **publish**:

```bash
app-track-agent validate-manifest --manifest ./release.manifest.json
app-track-agent publish --manifest ./release.manifest.json
# Optional: inspect the signed payload without writing:
# app-track-agent publish --manifest ./release.manifest.json --dry-run
```

5. Confirm inventory if useful:

```bash
app-track-agent inventory --purl 'pkg:…@…'
```

6. Report the JSON `release` object (id, version, digests) back to the human.

## Manifest schema

Save as e.g. `release.manifest.json`:

```json
{
  "application_slug": "my-app",
  "version": "1.2.3",
  "idempotency_key": "unique-per-attempt-or-ci-run-id",
  "artifacts": [
    {
      "role": "DEPLOYABLE",
      "artifact_type": "OCI_IMAGE",
      "name": "my-app",
      "digest": "sha256:…",
      "uri": "oci://registry.example.com/my-app@sha256:…"
    }
  ],
  "source": {
    "scm_uri": "https://…/my-app.git",
    "commit_sha": "…full or long git sha…",
    "tag": "v1.2.3"
  },
  "evidence_files": [
    {
      "evidence_type": "SBOM",
      "path": "./sbom.cdx.json",
      "content_type": "application/vnd.cyclonedx+json"
    }
  ],
  "ci_metadata_components": []
}
```

Rules the CLI enforces:

- At least one **artifact** with `digest` + `uri` (no artifact bytes in the API).
- Artifact `role` is `DEPLOYABLE` or `SUPPORTING`.
- `source.scm_uri` and `source.commit_sha` required.
- At least one `evidence_files` entry with `evidence_type` = `SBOM`.
- Paths in `evidence_files` are relative to the **manifest file’s directory**.
- `idempotency_key` required (1–128 chars). Reuse the same key + same payload to safely retry; different payload with same key fails.

## What you must NOT do

- Do **not** invent envelope signatures by hand — `app-track-agent publish` signs them.
- Do **not** put large artifact bytes into the manifest; only digest + URI.
- Do **not** skip the SBOM when recording a trusted release.
- Do **not** use DEV fixture API keys / envelope keys outside local or agreed CI.

## Artifact types (common)

| `artifact_type` | When |
|-----------------|------|
| `OCI_IMAGE` | Container image |
| `BINARY` | Native binary / archive |
| `CONFIG` | Config pack |
| `DB_SCHEMA` | Migration/schema bundle |
| `OTHER` | Anything else (explain in `name` / annotations) |

Prefer at least one `DEPLOYABLE`. Supporting materials (SBOM is evidence, not an artifact row) use `SUPPORTING` when they are separate downloadable refs.

## Failure handling

- Read stderr + any JSON `"ok": false`.
- `409` / conflict: version already published or idempotency mismatch — choose a new version or reuse the exact prior manifest + key.
- `403`: Publisher not bound for this app — run `bind-publisher`.
- `400` validation: fix manifest / SBOM; re-run `validate-manifest`.

## Success criteria

Publishing succeeded when stdout includes `"ok": true` and a `release` object with `id`, `version`, and `content_digest`. Prefer quoting that JSON in your final answer to the user.
