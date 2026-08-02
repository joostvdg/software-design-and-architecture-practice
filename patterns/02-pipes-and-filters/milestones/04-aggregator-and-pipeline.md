# Milestone 4 — Aggregator, pipeline composition, and wiring

## Goal

Build the fourth and final filter, `SeverityCountAggregator`, then compose all four
filters into one pipeline and run it end to end. This milestone is where the pattern's
payoff becomes visible: four filters, written and tested in complete isolation from
each other, snap together into a working pipeline with no changes to any of them.

## Step 1 — `SeverityCount` domain type (copy-paste)

`src/main/java/com/isaqb/practice/pipesandfilters/SeverityCount.java`:

```java
package com.isaqb.practice.pipesandfilters;

/** One severity level and how many entries at that level passed through the pipeline. */
public record SeverityCount(String level, long count) {}
```

## Step 2 — `SeverityCountAggregator` (write the body yourself)

`src/main/java/com/isaqb/practice/pipesandfilters/SeverityCountAggregator.java`:

```java
package com.isaqb.practice.pipesandfilters;

import java.util.List;

/**
 * Filter 4: the pipeline's final step, for the Observability team - collapses a list
 * of LogEntry into one SeverityCount per distinct level. Unlike the earlier filters,
 * this one changes the "shape" of the data (many entries in, a handful of summary rows
 * out), but it's still just a Filter<LogEntry, SeverityCount> - same contract, same
 * composability as every filter before it.
 */
public class SeverityCountAggregator implements Filter<LogEntry, SeverityCount> {

  @Override
  public List<SeverityCount> apply(List<LogEntry> input) {
    // TODO: group `input` by level and count entries per level. Return one
    // SeverityCount per distinct level present in `input`; order doesn't matter.
    // Hint: java.util.stream.Collectors.groupingBy(LogEntry::level, Collectors.counting())
    // gets you a Map<String, Long> in one line; turn each entry of that map into a
    // SeverityCount.
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

## Step 3 — tests for the aggregator (copy-paste, must pass once step 2 is done)

`src/test/java/com/isaqb/practice/pipesandfilters/SeverityCountAggregatorTest.java`:

```java
package com.isaqb.practice.pipesandfilters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class SeverityCountAggregatorTest {

  private final SeverityCountAggregator aggregator = new SeverityCountAggregator();
  private final Instant t = Instant.parse("2024-05-01T10:15:30Z");

  @Test
  void countsEntriesPerDistinctLevel() {
    List<LogEntry> input =
        List.of(
            new LogEntry(t, "INFO", "a"),
            new LogEntry(t, "INFO", "b"),
            new LogEntry(t, "ERROR", "c"));

    List<SeverityCount> result = aggregator.apply(input);

    Map<String, Long> byLevel =
        result.stream().collect(Collectors.toMap(SeverityCount::level, SeverityCount::count));
    assertEquals(2L, byLevel.get("INFO"));
    assertEquals(1L, byLevel.get("ERROR"));
    assertEquals(2, byLevel.size());
  }

  @Test
  void returnsEmptyListForEmptyInput() {
    assertEquals(0, aggregator.apply(List.of()).size());
  }
}
```

## Step 4 — `Pipeline`, the composition helper (copy-paste)

This class is pure wiring scaffolding, not pattern logic to practice — it's given in
full so the composability payoff is visible without an extra exercise in the way.

`src/main/java/com/isaqb/practice/pipesandfilters/Pipeline.java`:

```java
package com.isaqb.practice.pipesandfilters;

import java.util.List;

/**
 * Composes a chain of filters into a single filter. Each `.then(...)` call appends one
 * more stage; because Filter is generic in its input/output types, the compiler
 * enforces that neighboring filters' types actually line up - a `Pipeline<String,
 * LogEntry>` can only ever have `.then(Filter<LogEntry, ?>)` appended to it, so wiring
 * a filter into the wrong slot is a compile error, not a runtime surprise.
 */
public final class Pipeline<I, O> implements Filter<I, O> {

  private final Filter<I, O> filter;

  private Pipeline(Filter<I, O> filter) {
    this.filter = filter;
  }

  public static <I, O> Pipeline<I, O> start(Filter<I, O> first) {
    return new Pipeline<>(first);
  }

  public <R> Pipeline<I, R> then(Filter<O, R> next) {
    Filter<I, O> previous = this.filter;
    return new Pipeline<>(input -> next.apply(previous.apply(input)));
  }

  @Override
  public List<O> apply(List<I> input) {
    return filter.apply(input);
  }
}
```

## Step 5 — integration test (copy-paste, must pass once steps 1-2 are done)

`src/test/java/com/isaqb/practice/pipesandfilters/PipelineIntegrationTest.java`:

```java
package com.isaqb.practice.pipesandfilters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Exercises the whole chain end to end: raw lines in, severity counts out. If every
 * individual filter's own tests pass but this one doesn't, the bug is in how the
 * filters compose (wrong order, mismatched types), not in an individual filter.
 */
class PipelineIntegrationTest {

  @Test
  void parsesDropsRedactsAndCountsEndToEnd() {
    Pipeline<String, SeverityCount> pipeline =
        Pipeline.start(new LogLineParser())
            .then(new DropDebugFilter())
            .then(new RedactSecretsFilter())
            .then(new SeverityCountAggregator());

    List<String> rawLines =
        List.of(
            "2024-05-01T10:15:30Z INFO Starting build for pipeline nightly-build",
            "2024-05-01T10:15:31Z DEBUG Resolved dependency cache at /var/cache/pf",
            "2024-05-01T10:15:32Z ERROR Failed to authenticate with"
                + " token=sk-live-abcdef1234567890",
            "2024-05-01T10:15:33Z INFO Build finished successfully");

    List<SeverityCount> result = pipeline.apply(rawLines);

    Map<String, Long> byLevel =
        result.stream().collect(Collectors.toMap(SeverityCount::level, SeverityCount::count));
    assertEquals(2L, byLevel.get("INFO"));
    assertEquals(1L, byLevel.get("ERROR"));
    assertFalse(byLevel.containsKey("DEBUG"));
  }
}
```

## Step 6 — `Main`: the composition root (copy-paste)

`src/main/java/com/isaqb/practice/pipesandfilters/Main.java`:

```java
package com.isaqb.practice.pipesandfilters;

import java.util.List;

/**
 * Composition root: builds the pipeline from the four filters and runs it against a
 * batch of raw log lines a build agent might have emitted, then prints the aggregated
 * severity counts for the Observability team. This is the only class that names all
 * four filters - each filter itself only ever names Filter, LogEntry, or SeverityCount.
 */
public final class Main {

  private static final List<String> SAMPLE_LOG_LINES =
      List.of(
          "2024-05-01T10:15:30Z INFO Starting build for pipeline nightly-build",
          "2024-05-01T10:15:31Z DEBUG Resolved dependency cache at /var/cache/pf",
          "2024-05-01T10:15:32Z ERROR Failed to authenticate with"
              + " token=sk-live-abcdef1234567890",
          "2024-05-01T10:15:33Z INFO Build finished successfully");

  private Main() {}

  public static void main(String[] args) {
    Pipeline<String, SeverityCount> pipeline =
        Pipeline.start(new LogLineParser())
            .then(new DropDebugFilter())
            .then(new RedactSecretsFilter())
            .then(new SeverityCountAggregator());

    List<SeverityCount> result = pipeline.apply(SAMPLE_LOG_LINES);
    result.forEach(sc -> System.out.println(sc.level() + ": " + sc.count()));
  }
}
```

## Step 7 — try it for real

```bash
mvn -f patterns/02-pipes-and-filters/pom.xml clean package
java -jar patterns/02-pipes-and-filters/target/pipes-and-filters-1.0.0-SNAPSHOT.jar
```

You should see two lines of output — one per distinct severity level remaining after
DEBUG was dropped (`INFO` and `ERROR`, in whatever order your aggregator produced
them), each with a count. Nowhere in that output should the string `sk-live` appear.

## Checkpoint

- [ ] `mvn -f patterns/02-pipes-and-filters/pom.xml clean verify` passes, all filters'
      tests green, including `PipelineIntegrationTest`.
- [ ] The jar runs as described in step 7 and the token is redacted in effect (it never
      reaches the aggregated output at all, since aggregation only looks at levels —
      but confirm you understand *why* the redaction still matters even though this
      particular output doesn't print messages).
- [ ] You can point to the one line in `Main` you'd change to reorder
      `RedactSecretsFilter` before `DropDebugFilter` instead of after — and confirm
      none of the four filter classes themselves would need to change.

Next: [`05-build-and-release.md`](05-build-and-release.md).
