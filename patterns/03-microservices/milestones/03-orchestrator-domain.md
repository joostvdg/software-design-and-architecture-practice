# Milestone 3 — Orchestrator domain: the data the Orchestrator owns

## Goal

Build the data the Pipeline Orchestrator owns: `RunStatus`, the `PipelineRun` record,
and the in-memory `PipelineRunStore` that is the *only* thing allowed to hold or modify
run state. This is a deliberately separate store from `ArtifactStore` — same shape of
problem (in-memory, keyed lookup, overwrite-on-save), completely different data, owned
by a completely different service. That repetition is the point: two services, two
owners, two stores, zero shared Java objects between them.

## Step 1 — status and the run record (copy-paste)

`src/main/java/com/isaqb/practice/microservices/orchestrator/RunStatus.java`:

```java
package com.isaqb.practice.microservices.orchestrator;

/** The Orchestrator's own view of a pipeline run's lifecycle. */
public enum RunStatus {
  RUNNING,
  COMPLETED
}
```

`src/main/java/com/isaqb/practice/microservices/orchestrator/PipelineRun.java`:

```java
package com.isaqb.practice.microservices.orchestrator;

/**
 * A pipeline run as the Orchestrator knows it. Once COMPLETED, artifactName/
 * artifactVersion/artifactDigest describe what it produced - plain strings, not the
 * Registry's Artifact type, because the Orchestrator does not depend on the Registry's
 * internal types. It only knows the same wire-format fields the Registry does.
 */
public record PipelineRun(
    String id,
    RunStatus status,
    String artifactName,
    String artifactVersion,
    String artifactDigest) {

  /** A run that has just started, with no artifact produced yet. */
  public static PipelineRun running(String id) {
    return new PipelineRun(id, RunStatus.RUNNING, null, null, null);
  }
}
```

Notice `PipelineRun` never mentions `com.isaqb.practice.microservices.registry`
anywhere — that would be exactly the kind of shared-type coupling this exercise is
built to avoid. The Orchestrator and the Registry agree on field *names* (`name`,
`version`, `digest`) in the wire format, never on a shared Java class.

## Step 2 — the store shell (write the two methods yourself)

`src/main/java/com/isaqb/practice/microservices/orchestrator/PipelineRunStore.java`:

```java
package com.isaqb.practice.microservices.orchestrator;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Orchestrator's own in-memory data: pipeline runs, keyed by id. Saving under an
 * id that's already known overwrites it - e.g. a RUNNING run being saved again as
 * COMPLETED once it finishes.
 */
public final class PipelineRunStore {

  private final Map<String, PipelineRun> runs = new ConcurrentHashMap<>();

  /** Stores (or overwrites) a run, keyed by run.id(). */
  public void save(PipelineRun run) {
    // TODO: store `run` in `runs`, keyed by run.id().
    throw new UnsupportedOperationException("not implemented yet");
  }

  /** Returns the run for `id`, or Optional.empty() if unknown. */
  public Optional<PipelineRun> findById(String id) {
    // TODO: look `id` up in `runs`, wrapping the result in Optional.
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

## Step 3 — tests (copy-paste, must pass once step 2 is done)

`src/test/java/com/isaqb/practice/microservices/orchestrator/PipelineRunStoreTest.java`:

```java
package com.isaqb.practice.microservices.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PipelineRunStoreTest {

  private final PipelineRunStore store = new PipelineRunStore();

  @Test
  void findByIdIsEmptyWhenUnknown() {
    assertTrue(store.findById("run-1").isEmpty());
  }

  @Test
  void saveThenFindByIdReturnsIt() {
    var run = PipelineRun.running("run-1");

    store.save(run);

    assertEquals(run, store.findById("run-1").orElseThrow());
  }

  @Test
  void savingAgainUnderSameIdOverwrites() {
    store.save(PipelineRun.running("run-1"));
    var completed =
        new PipelineRun("run-1", RunStatus.COMPLETED, "web-app", "1.4.2", "sha256:abc123");

    store.save(completed);

    var found = store.findById("run-1").orElseThrow();
    assertEquals(RunStatus.COMPLETED, found.status());
    assertEquals("web-app", found.artifactName());
  }
}
```

## Checkpoint

```bash
mvn -f patterns/03-microservices/pom.xml clean verify
```

All three `PipelineRunStoreTest` cases pass. `registry/` and `orchestrator/` still
don't import each other — check it: `grep -r "microservices.registry" src/main/java/com/isaqb/practice/microservices/orchestrator/`
should print nothing.

Next: [`04-orchestrator-registry-client.md`](04-orchestrator-registry-client.md).
