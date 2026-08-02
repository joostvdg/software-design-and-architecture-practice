# Milestone 1 — Domain layer

## Goal

Build the Domain layer: the config model, the validation-rule contract, and three
concrete rules. This layer must not import anything from `application`,
`infrastructure`, or `com.isaqb.practice.layers` (the Presentation package) — it knows
nothing about CLIs, files, or use-case orchestration, only about what makes a pipeline
config valid.

Delete `src/test/java/com/isaqb/practice/layers/SmokeTest.java` now — the tests you add
in this milestone replace it as your "is the build green" signal.

## Step 1 — the config model (copy-paste)

`src/main/java/com/isaqb/practice/layers/domain/PipelineConfig.java`:

```java
package com.isaqb.practice.layers.domain;

import java.util.List;

/** A pipeline configuration as submitted for validation, before any rule has run. */
public record PipelineConfig(String name, List<String> stages) {

  public PipelineConfig {
    stages = List.copyOf(stages);
  }
}
```

## Step 2 — error and result types (copy-paste)

`src/main/java/com/isaqb/practice/layers/domain/ValidationError.java`:

```java
package com.isaqb.practice.layers.domain;

/** One rule violation: which rule failed, and a human-readable reason. */
public record ValidationError(String rule, String message) {}
```

`src/main/java/com/isaqb/practice/layers/domain/ValidationResult.java`:

```java
package com.isaqb.practice.layers.domain;

import java.util.List;

public record ValidationResult(List<ValidationError> errors) {

  public ValidationResult {
    errors = List.copyOf(errors);
  }

  public static ValidationResult valid() {
    return new ValidationResult(List.of());
  }

  public boolean isValid() {
    return errors.isEmpty();
  }
}
```

## Step 3 — the rule contract (copy-paste)

`src/main/java/com/isaqb/practice/layers/domain/ValidationRule.java`:

```java
package com.isaqb.practice.layers.domain;

import java.util.Optional;

/** One independent piece of validation logic. Returns an error iff the rule fails. */
public interface ValidationRule {

  Optional<ValidationError> check(PipelineConfig config);
}
```

## Step 4 — three concrete rules (write these yourself)

Create `src/main/java/com/isaqb/practice/layers/domain/rules/NameMustNotBeBlank.java`,
implementing `ValidationRule`:

```java
package com.isaqb.practice.layers.domain.rules;

import com.isaqb.practice.layers.domain.PipelineConfig;
import com.isaqb.practice.layers.domain.ValidationError;
import com.isaqb.practice.layers.domain.ValidationRule;
import java.util.Optional;

public class NameMustNotBeBlank implements ValidationRule {

  @Override
  public Optional<ValidationError> check(PipelineConfig config) {
    // TODO: if config.name() is null or blank (after trim), return
    // Optional.of(new ValidationError("name-must-not-be-blank", "<your message>")).
    // Otherwise return Optional.empty().
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

Then, same package, same shape:

- `MustHaveAtLeastOneStage` — fails when `config.stages()` is empty.
- `StageNamesMustBeUnique` — fails when `config.stages()` contains a duplicate (case-
  sensitive is fine). Hint: `java.util.Set` is enough; you don't need to report *which*
  stage duplicated for this exercise, just that one does.

Pick your own `rule` identifier string and message for each — the tests below check
*that* an error is returned, not its exact wording, except where noted.

## Step 5 — tests (copy-paste, must pass once step 4 is done)

`src/test/java/com/isaqb/practice/layers/domain/rules/NameMustNotBeBlankTest.java`:

```java
package com.isaqb.practice.layers.domain.rules;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.isaqb.practice.layers.domain.PipelineConfig;
import java.util.List;
import org.junit.jupiter.api.Test;

class NameMustNotBeBlankTest {

  private final NameMustNotBeBlank rule = new NameMustNotBeBlank();

  @Test
  void failsOnBlankName() {
    var config = new PipelineConfig("   ", List.of("compile"));
    assertTrue(rule.check(config).isPresent());
  }

  @Test
  void passesOnNonBlankName() {
    var config = new PipelineConfig("nightly-build", List.of("compile"));
    assertTrue(rule.check(config).isEmpty());
  }
}
```

Write the equivalent test classes for `MustHaveAtLeastOneStageTest` and
`StageNamesMustBeUniqueTest` yourself — same shape: one case that must fail, one that
must pass. For the duplicate-stage rule, add a third case with unique stages that
passes, to make sure your `Set`-based check isn't accidentally rejecting valid configs.

## Checkpoint

```bash
mvn -f patterns/01-layers/pom.xml clean verify
```

All domain tests pass, and `src/main/java/.../domain/**` has zero `import` statements
pointing outside `com.isaqb.practice.layers.domain`.

Next: [`02-application-layer.md`](02-application-layer.md).
