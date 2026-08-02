# Milestone 1 — Registry domain: the data the Registry owns

## Goal

Build the data the Artifact Registry owns: the `Artifact` record and the in-memory
`ArtifactStore` that is the *only* thing in this whole module allowed to hold or modify
that data. Nothing outside the `registry` package ever touches `ArtifactStore`
directly — the Orchestrator will only ever reach it over HTTP, starting in
milestone 4. That's data ownership, the second defining idea from the README, made
concrete: one owner, one writer, everyone else asks over the network.

Delete `src/test/java/com/isaqb/practice/microservices/SmokeTest.java` now — the tests
you add in this milestone replace it as your "is the build green" signal.

## Step 1 — the `Artifact` record (copy-paste)

`src/main/java/com/isaqb/practice/microservices/registry/Artifact.java`:

```java
package com.isaqb.practice.microservices.registry;

/**
 * A built artifact as the Registry knows it: which pipeline output it is (name),
 * which version, and the content digest that identifies the exact bytes that were
 * built. This is the Registry's own data - nothing outside this package should ever
 * need to construct one directly except through ArtifactStore.
 */
public record Artifact(String name, String version, String digest) {}
```

## Step 2 — the store shell (write the two methods yourself)

`src/main/java/com/isaqb/practice/microservices/registry/ArtifactStore.java`:

```java
package com.isaqb.practice.microservices.registry;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Registry's own in-memory data. Keyed by artifact name; registering an artifact
 * under a name that's already known overwrites it - "latest registered version wins"
 * is this exercise's simplification of a real registry's full version history.
 *
 * This class is intentionally the only place in the module that holds artifact data.
 * HTTP handlers call it; nothing calls into it from outside the registry package.
 */
public final class ArtifactStore {

  private final Map<String, Artifact> byName = new ConcurrentHashMap<>();

  /**
   * Registers (or overwrites) an artifact, keyed by its name. After this call,
   * findByName(artifact.name()) must return this exact artifact.
   */
  public void register(Artifact artifact) {
    // TODO: store `artifact` in `byName`, keyed by artifact.name().
    throw new UnsupportedOperationException("not implemented yet");
  }

  /**
   * Looks up the most recently registered artifact for the given name.
   * Returns Optional.empty() if nothing has ever been registered under that name.
   */
  public Optional<Artifact> findByName(String name) {
    // TODO: look `name` up in `byName`, wrapping the result in Optional.
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

Why `ConcurrentHashMap` rather than `HashMap`: an `HttpServer` (milestone 2) dispatches
each request on its own thread by default, so two `register` calls could race. This is
a small taste of an operational concern that simply doesn't exist in a single-threaded
CLI like the Layers exercise — another way "independently deployable, networked
service" changes what you have to think about.

## Step 3 — tests (copy-paste, must pass once step 2 is done)

`src/test/java/com/isaqb/practice/microservices/registry/ArtifactStoreTest.java`:

```java
package com.isaqb.practice.microservices.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ArtifactStoreTest {

  private final ArtifactStore store = new ArtifactStore();

  @Test
  void findByNameIsEmptyWhenNothingRegistered() {
    assertTrue(store.findByName("web-app").isEmpty());
  }

  @Test
  void registerThenFindByNameReturnsIt() {
    var artifact = new Artifact("web-app", "1.4.2", "sha256:abc123");

    store.register(artifact);

    assertEquals(artifact, store.findByName("web-app").orElseThrow());
  }

  @Test
  void registeringAgainUnderSameNameOverwrites() {
    store.register(new Artifact("web-app", "1.4.2", "sha256:abc123"));
    store.register(new Artifact("web-app", "1.5.0", "sha256:def456"));

    var found = store.findByName("web-app").orElseThrow();

    assertEquals("1.5.0", found.version());
    assertEquals("sha256:def456", found.digest());
  }
}
```

## Checkpoint

```bash
mvn -f patterns/03-microservices/pom.xml clean verify
```

All three `ArtifactStoreTest` cases pass, and nothing outside `registry/` imports
`ArtifactStore` or `Artifact` yet (there's nothing else to import it from — the
Orchestrator package doesn't exist yet either).

Next: [`02-registry-http-api.md`](02-registry-http-api.md).
