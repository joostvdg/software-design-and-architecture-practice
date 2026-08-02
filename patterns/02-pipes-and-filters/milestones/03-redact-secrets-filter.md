# Milestone 3 — Redact-secrets filter

## Goal

Build `RedactSecretsFilter`: the third stage, which scrubs anything that looks like a
leaked token from a log entry's message before it's stored or searched. Real build
agents leak credentials into logs more often than anyone would like — this filter is
the safety net that stops that from reaching the Observability team's search index.

Writing a robust secret-detection regex is a rabbit hole of its own and isn't the point
of this exercise; the pattern to match is given below. Applying it correctly *inside
the filter's contract* — same input/output shape as `DropDebugFilter`, different
transformation — is what you're practicing.

## Step 1 — the class shell (pattern given, redaction logic left to you)

`src/main/java/com/isaqb/practice/pipesandfilters/RedactSecretsFilter.java`:

```java
package com.isaqb.practice.pipesandfilters;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Filter 3: redacts anything that looks like a credential before entries are stored or
 * searched. Same Filter<LogEntry, LogEntry> shape as DropDebugFilter - a different
 * transformation, wired the same way.
 */
public class RedactSecretsFilter implements Filter<LogEntry, LogEntry> {

  // Matches "token=<value>", where <value> is a run of letters, digits, '-' or '_'.
  private static final Pattern TOKEN_PATTERN = Pattern.compile("token=[A-Za-z0-9_-]+");

  @Override
  public List<LogEntry> apply(List<LogEntry> input) {
    // TODO: for each entry in `input`, replace every match of TOKEN_PATTERN in its
    // message with the literal text "token=***REDACTED***", leaving the timestamp and
    // level unchanged. Return a new list of LogEntry with the redacted messages, same
    // order as the input.
    // Hint: LogEntry has no setters (it's a record) - build a *new* LogEntry per
    // entry rather than trying to mutate one. Matcher#replaceAll (or
    // String#replaceAll(TOKEN_PATTERN.pattern(), ...)) does the substitution itself.
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

## Step 2 — tests (copy-paste, must pass once step 1 is done)

`src/test/java/com/isaqb/practice/pipesandfilters/RedactSecretsFilterTest.java`:

```java
package com.isaqb.practice.pipesandfilters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class RedactSecretsFilterTest {

  private final RedactSecretsFilter filter = new RedactSecretsFilter();
  private final Instant t = Instant.parse("2024-05-01T10:15:30Z");

  @Test
  void redactsTokenValueButKeepsRestOfMessage() {
    var input =
        List.of(
            new LogEntry(
                t, "ERROR", "Failed to authenticate with token=sk-live-abcdef1234567890"));

    List<LogEntry> result = filter.apply(input);

    String message = result.get(0).message();
    assertFalse(message.contains("sk-live-abcdef1234567890"));
    assertTrue(message.contains("token=***REDACTED***"));
    assertTrue(message.startsWith("Failed to authenticate with"));
  }

  @Test
  void leavesMessagesWithoutTokensUnchanged() {
    var input = List.of(new LogEntry(t, "INFO", "Build finished successfully"));

    List<LogEntry> result = filter.apply(input);

    assertEquals("Build finished successfully", result.get(0).message());
  }

  @Test
  void preservesTimestampAndLevel() {
    var input = List.of(new LogEntry(t, "ERROR", "token=abc123"));

    LogEntry result = filter.apply(input).get(0);

    assertEquals(t, result.timestamp());
    assertEquals("ERROR", result.level());
  }
}
```

## Checkpoint

```bash
mvn -f patterns/02-pipes-and-filters/pom.xml clean verify
```

All three `RedactSecretsFilterTest` cases pass. You now have three filters
(`LogLineParser`, `DropDebugFilter`, `RedactSecretsFilter`) that each do one thing and
have never once referenced each other by name.

Next: [`04-aggregator-and-pipeline.md`](04-aggregator-and-pipeline.md).
