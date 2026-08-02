# Milestone 1 — The `Filter` contract, `LogEntry`, and the first filter

## Goal

Define the `Filter<I, O>` contract every filter in this pipeline implements, the
`LogEntry` domain type, and the first concrete filter: `LogLineParser`, which turns raw
log text into structured `LogEntry` values. This is the only filter in the whole
pipeline allowed to know the raw text format exists — every filter after it only ever
sees `LogEntry` objects.

Delete `src/test/java/com/isaqb/practice/pipesandfilters/SmokeTest.java` now — the
tests you add in this milestone replace it as your "is the build green" signal.

## Step 1 — the `Filter` contract (copy-paste)

`src/main/java/com/isaqb/practice/pipesandfilters/Filter.java`:

```java
package com.isaqb.practice.pipesandfilters;

import java.util.List;

/**
 * One independent processing step in the pipeline. A filter's contract is only ever
 * "a list of I in, a list of O out" - it doesn't know what ran before it or what runs
 * after it, only its own transformation. That's what makes filters independently
 * testable, reorderable, and replaceable: nothing about this interface ties a filter
 * to its neighbors.
 *
 * <p>This exercise uses a batch contract (whole lists in, whole lists out) rather than
 * a streaming one (one element at a time) - see the README's trade-offs section for
 * why that's a deliberate choice, not a simplification you should "fix".
 */
@FunctionalInterface
public interface Filter<I, O> {

  List<O> apply(List<I> input);
}
```

## Step 2 — the `LogEntry` domain type (copy-paste)

`src/main/java/com/isaqb/practice/pipesandfilters/LogEntry.java`:

```java
package com.isaqb.practice.pipesandfilters;

import java.time.Instant;

/** A single parsed log entry: when it happened, how severe it is, and what it says. */
public record LogEntry(Instant timestamp, String level, String message) {}
```

## Step 3 — `LogLineParser` (write the body yourself)

`src/main/java/com/isaqb/practice/pipesandfilters/LogLineParser.java`:

```java
package com.isaqb.practice.pipesandfilters;

import java.util.List;

/**
 * Filter 1: raw text -> structured data. Parses lines of the form
 * "<ISO-8601 timestamp> <LEVEL> <message>" into LogEntry. This is the only filter in
 * the pipeline that needs to know this raw wire format exists - everything downstream
 * only ever sees LogEntry.
 */
public class LogLineParser implements Filter<String, LogEntry> {

  @Override
  public List<LogEntry> apply(List<String> input) {
    // TODO: for each raw line in `input`, in order:
    //  - if the line is blank (empty or all whitespace), skip it - it does not produce
    //    a LogEntry.
    //  - otherwise split it into at most 3 parts on spaces (the message itself may
    //    contain spaces, so a plain 3-way split is what you want -
    //    String.split(" ", 3) keeps the third part intact). If you don't get 3 parts,
    //    the line is malformed: throw an IllegalArgumentException naming the
    //    offending line.
    //  - parse the first part as a timestamp with java.time.Instant.parse(...). It
    //    throws java.time.format.DateTimeParseException on bad input, which is NOT an
    //    IllegalArgumentException - catch it and wrap it into one (or throw a new
    //    IllegalArgumentException naming the offending line) so callers only ever have
    //    to handle one exception type from this filter.
    //  - the second part is the level, used as-is (e.g. "INFO", "DEBUG", "ERROR").
    //  - the third part is the message, used as-is.
    // Return one LogEntry per non-blank input line, preserving the original order.
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

## Step 4 — tests (copy-paste, must pass once step 3 is done)

`src/test/java/com/isaqb/practice/pipesandfilters/LogLineParserTest.java`:

```java
package com.isaqb.practice.pipesandfilters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class LogLineParserTest {

  private final LogLineParser parser = new LogLineParser();

  @Test
  void parsesTimestampLevelAndMessage() {
    List<LogEntry> entries =
        parser.apply(
            List.of("2024-05-01T10:15:30Z INFO Starting build for pipeline nightly-build"));

    assertEquals(1, entries.size());
    LogEntry entry = entries.get(0);
    assertEquals(Instant.parse("2024-05-01T10:15:30Z"), entry.timestamp());
    assertEquals("INFO", entry.level());
    assertEquals("Starting build for pipeline nightly-build", entry.message());
  }

  @Test
  void parsesMultipleLinesInOrder() {
    List<LogEntry> entries =
        parser.apply(
            List.of(
                "2024-05-01T10:15:30Z INFO first",
                "2024-05-01T10:15:31Z DEBUG second"));

    assertEquals(2, entries.size());
    assertEquals("first", entries.get(0).message());
    assertEquals("second", entries.get(1).message());
  }

  @Test
  void skipsBlankLines() {
    List<LogEntry> entries =
        parser.apply(List.of("2024-05-01T10:15:30Z INFO hello", "", "   "));

    assertEquals(1, entries.size());
  }

  @Test
  void rejectsLinesWithTooFewParts() {
    assertThrows(IllegalArgumentException.class, () -> parser.apply(List.of("justoneword")));
  }

  @Test
  void rejectsLinesWithUnparsableTimestamp() {
    assertThrows(
        IllegalArgumentException.class,
        () -> parser.apply(List.of("not-a-timestamp INFO hello")));
  }
}
```

## Checkpoint

```bash
mvn -f patterns/02-pipes-and-filters/pom.xml clean verify
```

All five `LogLineParserTest` cases pass, and nothing in `Filter.java` or `LogEntry.java`
knows the raw text format exists — only `LogLineParser` does.

Next: [`02-drop-debug-filter.md`](02-drop-debug-filter.md).
