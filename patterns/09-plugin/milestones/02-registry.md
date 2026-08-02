# Milestone 2 — Plugin registry

## Goal

Build the `PluginRegistry`: the lookup mechanism that lets `PipelineRunner` (milestone
4) resolve a step's type id to a `PipelineStepPlugin` instance without ever importing
a concrete plugin class. This is the piece that makes the extension point *usable* —
milestone 1's interface alone doesn't give the core a way to find implementations.

## Step 1 — exceptions (copy-paste)

`src/main/java/com/isaqb/practice/plugin/UnknownPluginException.java`:

```java
package com.isaqb.practice.plugin;

/** Thrown when looking up a plugin id that was never registered. */
public class UnknownPluginException extends RuntimeException {

  public UnknownPluginException(String id) {
    super("no plugin registered for id: " + id);
  }
}
```

`src/main/java/com/isaqb/practice/plugin/DuplicatePluginException.java`:

```java
package com.isaqb.practice.plugin;

/** Thrown when registering a plugin id that's already registered. */
public class DuplicatePluginException extends RuntimeException {

  public DuplicatePluginException(String id) {
    super("a plugin is already registered for id: " + id);
  }
}
```

## Step 2 — the registry (write this yourself)

Create `src/main/java/com/isaqb/practice/plugin/PluginRegistry.java`:

```java
package com.isaqb.practice.plugin;

/**
 * Holds {@link PipelineStepPlugin}s keyed by their {@link PipelineStepPlugin#id()}.
 * The only way {@link PipelineRunner} learns what step types exist.
 */
public class PluginRegistry {

  // TODO: back this with a Map<String, PipelineStepPlugin>.

  /**
   * Registers a plugin under its own {@code id()}.
   *
   * @throws DuplicatePluginException if a plugin is already registered under that id.
   */
  public void register(PipelineStepPlugin plugin) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  /**
   * Looks up the plugin registered under {@code id}.
   *
   * @throws UnknownPluginException if no plugin is registered under that id.
   */
  public PipelineStepPlugin lookup(String id) {
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

Think about *why* `register` rejects duplicates rather than silently overwriting: a
silent overwrite would let two independently-written plugins fight over the same id
with no error, and whichever registered last would win — invisibly.

## Step 3 — tests (copy-paste, must pass once step 2 is done)

`src/test/java/com/isaqb/practice/plugin/PluginRegistryTest.java`:

```java
package com.isaqb.practice.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PluginRegistryTest {

  private final PluginRegistry registry = new PluginRegistry();

  @Test
  void registersAndLooksUpById() {
    var echo = new EchoPlugin();
    registry.register(echo);

    assertSame(echo, registry.lookup("echo"));
  }

  @Test
  void unknownIdThrows() {
    assertThrows(UnknownPluginException.class, () -> registry.lookup("nope"));
  }

  @Test
  void duplicateIdThrows() {
    registry.register(new EchoPlugin());

    assertThrows(DuplicatePluginException.class, () -> registry.register(new EchoPlugin()));
  }
}
```

## Checkpoint

```bash
mvn -f patterns/09-plugin/pom.xml clean verify
```

All three `PluginRegistryTest` cases pass.

Next: [`03-builtin-plugins.md`](03-builtin-plugins.md).
