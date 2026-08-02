# Milestone 2 — Application layer

## Goal

Build the Application layer: the `ValidateConfigUseCase` that orchestrates "load a
config, run every rule against it, return the aggregated result" — and the
`ConfigSource` port it needs but does not implement. This is the layer where you'll
feel the strict-layering rule most directly: `application` may import `domain`, but
must *not* import `infrastructure`. Infrastructure will implement `ConfigSource` in the
next milestone; Application only needs the interface.

## Step 1 — the port and its exception (copy-paste)

`src/main/java/com/isaqb/practice/layers/application/ConfigSource.java`:

```java
package com.isaqb.practice.layers.application;

import com.isaqb.practice.layers.domain.PipelineConfig;
import java.nio.file.Path;

/**
 * A port: Application declares what it needs (load a config from somewhere),
 * Infrastructure decides how (a file, later maybe an API call). Application never
 * knows which implementation is wired in - see Main in milestone 4.
 */
public interface ConfigSource {

  PipelineConfig load(Path path) throws ConfigLoadException;
}
```

`src/main/java/com/isaqb/practice/layers/application/ConfigLoadException.java`:

```java
package com.isaqb.practice.layers.application;

public class ConfigLoadException extends Exception {

  public ConfigLoadException(String message) {
    super(message);
  }

  public ConfigLoadException(String message, Throwable cause) {
    super(message, cause);
  }
}
```

## Step 2 — the use case (write the body yourself)

`src/main/java/com/isaqb/practice/layers/application/ValidateConfigUseCase.java`:

```java
package com.isaqb.practice.layers.application;

import com.isaqb.practice.layers.domain.PipelineConfig;
import com.isaqb.practice.layers.domain.ValidationResult;
import com.isaqb.practice.layers.domain.ValidationRule;
import java.nio.file.Path;
import java.util.List;

/**
 * Orchestrates the "validate a submitted pipeline config" use case: load it via the
 * infrastructure-provided ConfigSource, then run every domain rule against it.
 */
public class ValidateConfigUseCase {

  private final ConfigSource configSource;
  private final List<ValidationRule> rules;

  public ValidateConfigUseCase(ConfigSource configSource, List<ValidationRule> rules) {
    this.configSource = configSource;
    this.rules = List.copyOf(rules);
  }

  public ValidationResult validate(Path configPath) throws ConfigLoadException {
    // TODO:
    //  1. Load the PipelineConfig via configSource.load(configPath).
    //  2. Run every rule in `rules` against it - ValidationRule.check returns
    //     Optional<ValidationError>.
    //  3. Collect the present errors into a new ValidationResult and return it.
    // This method should contain no business logic of its own, only orchestration -
    // that's the whole point of the Application layer.
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

If you want a hint on step 3: `PipelineConfig` -> `rules.stream().map(...).flatMap(Optional::stream).toList()`
gets you from "a rule per element" to "just the errors that are present" in one line -
but writing it as an explicit loop is equally correct and arguably more readable here.

## Step 3 — tests (copy-paste, must pass once step 2 is done)

`src/test/java/com/isaqb/practice/layers/application/ValidateConfigUseCaseTest.java`:

```java
package com.isaqb.practice.layers.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.isaqb.practice.layers.domain.PipelineConfig;
import com.isaqb.practice.layers.domain.ValidationError;
import com.isaqb.practice.layers.domain.ValidationRule;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ValidateConfigUseCaseTest {

  @Test
  void aggregatesNoErrorsWhenAllRulesPass() throws ConfigLoadException {
    ConfigSource source = path -> new PipelineConfig("nightly-build", List.of("compile"));
    List<ValidationRule> rules = List.of(config -> Optional.empty());
    var useCase = new ValidateConfigUseCase(source, rules);

    var result = useCase.validate(Path.of("irrelevant.conf"));

    assertTrue(result.isValid());
  }

  @Test
  void aggregatesErrorsFromFailingRules() throws ConfigLoadException {
    ConfigSource source = path -> new PipelineConfig("", List.of());
    List<ValidationRule> rules =
        List.of(
            config -> Optional.of(new ValidationError("rule-a", "boom")),
            config -> Optional.of(new ValidationError("rule-b", "also boom")));
    var useCase = new ValidateConfigUseCase(source, rules);

    var result = useCase.validate(Path.of("irrelevant.conf"));

    assertTrue(!result.isValid());
    assertEquals(2, result.errors().size());
  }
}
```

Notice the test never touches the filesystem: `ConfigSource` is a lambda here, not
`FileConfigSource`. That's the payoff of the port - Application is testable in
isolation, before Infrastructure even exists.

## Checkpoint

```bash
mvn -f patterns/01-layers/pom.xml clean verify
```

Both use-case tests pass, and nothing under `application/` imports anything from an
`infrastructure` package (it doesn't exist yet - it will after the next milestone, and
it must stay that way afterwards too).

Next: [`03-infrastructure-layer.md`](03-infrastructure-layer.md).
