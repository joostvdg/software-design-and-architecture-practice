# Milestone 3 — Infrastructure layer

## Goal

Implement `ConfigSource` for real: `FileConfigSource` reads the hand-rolled config text
format from disk and turns it into a `PipelineConfig`. This is the only class in the
whole module that's allowed to know the file format exists — Domain and Application
only ever see `PipelineConfig` objects.

Recall the format from the case study:

```
name: nightly-build
stage: compile
stage: test
stage: package
```

## Step 1 — the class shell (file I/O given, parsing left to you)

`src/main/java/com/isaqb/practice/layers/infrastructure/FileConfigSource.java`:

```java
package com.isaqb.practice.layers.infrastructure;

import com.isaqb.practice.layers.application.ConfigLoadException;
import com.isaqb.practice.layers.application.ConfigSource;
import com.isaqb.practice.layers.domain.PipelineConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Reads the `name: ...` / `stage: ...` config format described in the case study. The
 * only class in this module that knows this file format exists.
 */
public class FileConfigSource implements ConfigSource {

  @Override
  public PipelineConfig load(Path path) throws ConfigLoadException {
    List<String> lines;
    try {
      lines = Files.readAllLines(path);
    } catch (IOException e) {
      throw new ConfigLoadException("could not read config file: " + path, e);
    }

    // TODO: parse `lines` into a PipelineConfig:
    //  - a line "name: X" sets the config's name (last one wins if there are several)
    //  - a line "stage: Y" appends Y to the stage list, in file order
    //  - blank lines are ignored
    //  - any other non-blank line is malformed input: throw a ConfigLoadException
    //  - trim whitespace around the value after the colon
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

Notice the constructor of `ConfigLoadException` you already wrote in milestone 2 gives
you both a plain-message form and a message+cause form — you'll want the plain-message
one for the "malformed line" case and the message+cause one for the `IOException` case
already handled above.

## Step 2 — tests (copy-paste, must pass once step 1 is done)

`src/test/java/com/isaqb/practice/layers/infrastructure/FileConfigSourceTest.java`:

```java
package com.isaqb.practice.layers.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.isaqb.practice.layers.application.ConfigLoadException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileConfigSourceTest {

  private final FileConfigSource source = new FileConfigSource();

  @Test
  void parsesNameAndStagesInOrder(@TempDir Path tempDir)
      throws IOException, ConfigLoadException {
    Path configFile = tempDir.resolve("pipeline.conf");
    Files.writeString(
        configFile,
        """
        name: nightly-build
        stage: compile
        stage: test
        stage: package
        """);

    var config = source.load(configFile);

    assertEquals("nightly-build", config.name());
    assertEquals(List.of("compile", "test", "package"), config.stages());
  }

  @Test
  void rejectsMalformedLines(@TempDir Path tempDir) throws IOException {
    Path configFile = tempDir.resolve("pipeline.conf");
    Files.writeString(configFile, "this is not a valid line\n");

    assertThrows(ConfigLoadException.class, () -> source.load(configFile));
  }

  @Test
  void wrapsMissingFileAsConfigLoadException() {
    Path missing = Path.of("does-not-exist.conf");

    assertThrows(ConfigLoadException.class, () -> source.load(missing));
  }
}
```

## Checkpoint

```bash
mvn -f patterns/01-layers/pom.xml clean verify
```

All three `FileConfigSourceTest` cases pass. Take a moment to check: does anything
under `domain/` or `application/` import `infrastructure`? It shouldn't — the
dependency only goes the other way.

Next: [`04-presentation-layer.md`](04-presentation-layer.md).
