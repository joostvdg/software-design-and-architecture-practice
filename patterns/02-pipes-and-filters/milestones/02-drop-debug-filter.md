# Milestone 2 — Drop-debug filter

## Goal

Build `DropDebugFilter`: the second stage of the pipeline, which removes DEBUG-level
entries so downstream filters (and eventually the Observability team) don't have to
wade through build agents' verbose diagnostic chatter. This filter is deliberately the
simplest one in the pipeline — it exists mainly to make the *shape* of a
`Filter<LogEntry, LogEntry>` (same type in and out) obvious before the next milestone's
redaction filter does something less trivial with that same shape.

## Step 1 — the class shell (write the body yourself)

`src/main/java/com/isaqb/practice/pipesandfilters/DropDebugFilter.java`:

```java
package com.isaqb.practice.pipesandfilters;

import java.util.List;

/**
 * Filter 2: drops noise. Removes DEBUG-level entries from the stream. Notice this
 * filter has no idea LogLineParser exists, or that a RedactSecretsFilter runs after
 * it - it only knows "given a list of LogEntry, produce a list of LogEntry". That's
 * the independence Pipes and Filters is built on.
 */
public class DropDebugFilter implements Filter<LogEntry, LogEntry> {

  @Override
  public List<LogEntry> apply(List<LogEntry> input) {
    // TODO: return a new list containing every entry from `input` whose level is not
    // "DEBUG" (case-sensitive is fine for this exercise), preserving relative order.
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

## Step 2 — tests (copy-paste, must pass once step 1 is done)

`src/test/java/com/isaqb/practice/pipesandfilters/DropDebugFilterTest.java`:

```java
package com.isaqb.practice.pipesandfilters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class DropDebugFilterTest {

  private final DropDebugFilter filter = new DropDebugFilter();
  private final Instant t = Instant.parse("2024-05-01T10:15:30Z");

  @Test
  void dropsDebugEntriesButKeepsOthersInOrder() {
    List<LogEntry> input =
        List.of(
            new LogEntry(t, "INFO", "first"),
            new LogEntry(t, "DEBUG", "noisy"),
            new LogEntry(t, "ERROR", "second"));

    List<LogEntry> result = filter.apply(input);

    assertEquals(2, result.size());
    assertEquals("first", result.get(0).message());
    assertEquals("second", result.get(1).message());
  }

  @Test
  void keepsAllEntriesWhenNoneAreDebug() {
    List<LogEntry> input = List.of(new LogEntry(t, "INFO", "a"), new LogEntry(t, "WARN", "b"));

    assertEquals(2, filter.apply(input).size());
  }

  @Test
  void returnsEmptyListWhenEverythingIsDebug() {
    List<LogEntry> input = List.of(new LogEntry(t, "DEBUG", "a"), new LogEntry(t, "DEBUG", "b"));

    assertEquals(0, filter.apply(input).size());
  }
}
```

## Checkpoint

```bash
mvn -f patterns/02-pipes-and-filters/pom.xml clean verify
```

All three `DropDebugFilterTest` cases pass. Try wiring `LogLineParser` and
`DropDebugFilter` together manually in a scratch `main` method (don't commit it) —
`dropDebugFilter.apply(logLineParser.apply(rawLines))` — to see two filters compose
before `Pipeline` (milestone 4) gives you a nicer way to write that.

Next: [`03-redact-secrets-filter.md`](03-redact-secrets-filter.md).
