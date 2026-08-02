# Milestone 4 — Presentation layer

## Goal

Write `Main`: the CLI entry point *and* the composition root. This is the only class
in the module allowed to import from every other layer at once, because wiring
concrete implementations into abstractions is exactly what a composition root is for.
Everywhere else, the dependency direction has been one-way; `Main` is where it all gets
tied together.

## Step 1 — wiring and CLI plumbing (copy-paste)

`src/main/java/com/isaqb/practice/layers/Main.java`:

```java
package com.isaqb.practice.layers;

import com.isaqb.practice.layers.application.ConfigLoadException;
import com.isaqb.practice.layers.application.ValidateConfigUseCase;
import com.isaqb.practice.layers.domain.ValidationResult;
import com.isaqb.practice.layers.domain.ValidationRule;
import com.isaqb.practice.layers.domain.rules.MustHaveAtLeastOneStage;
import com.isaqb.practice.layers.domain.rules.NameMustNotBeBlank;
import com.isaqb.practice.layers.domain.rules.StageNamesMustBeUnique;
import com.isaqb.practice.layers.infrastructure.FileConfigSource;
import java.nio.file.Path;
import java.util.List;

/**
 * Composition root: the only class allowed to know about every layer at once. Wires
 * the infrastructure ConfigSource and the domain rules into the application use case,
 * then runs it as a CLI.
 */
public final class Main {

  private Main() {}

  public static void main(String[] args) {
    if (args.length != 1) {
      System.err.println("usage: layers <path-to-config-file>");
      System.exit(2);
      return;
    }

    List<ValidationRule> rules =
        List.of(
            new NameMustNotBeBlank(),
            new MustHaveAtLeastOneStage(),
            new StageNamesMustBeUnique());
    var useCase = new ValidateConfigUseCase(new FileConfigSource(), rules);

    try {
      ValidationResult result = useCase.validate(Path.of(args[0]));
      System.out.println(formatResult(result));
      System.exit(result.isValid() ? 0 : 1);
    } catch (ConfigLoadException e) {
      System.err.println("failed to load config: " + e.getMessage());
      System.exit(2);
    }
  }

  // See step 2 below.
  static String formatResult(ValidationResult result) {
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

Notice `NameMustNotBeBlank`, `MustHaveAtLeastOneStage`, `StageNamesMustBeUnique`, and
`FileConfigSource` are the *only* concrete classes named anywhere outside their own
layer - and they're named here, in the composition root, and nowhere else. If you
wanted to add a fourth rule, you'd write the class in `domain/rules/` and add one line
here; `ValidateConfigUseCase` never changes.

## Step 2 — output formatting (write this yourself)

Implement `formatResult` above:

- If `result.isValid()`, return a single line such as `"VALID: config has no
  violations."`
- Otherwise, return a header line like `"INVALID: N violation(s):"` followed by one
  line per error formatted as `"- <rule>: <message>"` (join with `\n`).

## Step 3 — test (copy-paste, must pass once step 2 is done)

`src/test/java/com/isaqb/practice/layers/MainTest.java`:

```java
package com.isaqb.practice.layers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.isaqb.practice.layers.domain.ValidationError;
import com.isaqb.practice.layers.domain.ValidationResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class MainTest {

  @Test
  void formatsValidResult() {
    String formatted = Main.formatResult(ValidationResult.valid());

    assertTrue(formatted.toLowerCase().contains("valid"));
  }

  @Test
  void formatsInvalidResultWithEachError() {
    var result =
        new ValidationResult(
            List.of(new ValidationError("name-must-not-be-blank", "name is blank")));

    String formatted = Main.formatResult(result);

    assertTrue(formatted.contains("name-must-not-be-blank"));
    assertTrue(formatted.contains("name is blank"));
  }
}
```

## Step 4 — try it for real

Create two example config files (outside `src/`, e.g. in an `examples/` folder next to
`pom.xml`):

`examples/valid-pipeline.conf`:

```
name: nightly-build
stage: compile
stage: test
stage: package
```

`examples/invalid-pipeline.conf`:

```
name:
stage: compile
stage: compile
```

Build the jar and run it against both:

```bash
mvn -f patterns/01-layers/pom.xml clean package
java -jar patterns/01-layers/target/layers-1.0.0-SNAPSHOT.jar patterns/01-layers/examples/valid-pipeline.conf
java -jar patterns/01-layers/target/layers-1.0.0-SNAPSHOT.jar patterns/01-layers/examples/invalid-pipeline.conf
```

The first run should print your "valid" message and exit `0`; the second should print
two violations (blank name, duplicate stage) and exit `1`. Check the exit code with
`echo $?` right after each run.

## Checkpoint

- [ ] `mvn -f patterns/01-layers/pom.xml clean verify` passes, all layers' tests green.
- [ ] Both example files behave as described in step 4.
- [ ] You can point to the one line in `Main` that would change if `FileConfigSource`
      were swapped for, say, an `HttpConfigSource` — and confirm nothing in
      `application/` or `domain/` would need to change.

Next: [`05-build-and-release.md`](05-build-and-release.md).
