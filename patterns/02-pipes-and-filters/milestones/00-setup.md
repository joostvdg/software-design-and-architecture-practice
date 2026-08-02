# Milestone 0 — Setup

## Goal

Confirm the module builds, understand the target package layout, and get oriented in
the case study before writing any pattern code.

## Confirm the build is green

From the repo root:

```bash
mvn -f patterns/02-pipes-and-filters/pom.xml clean verify
```

This should pass — right now the module only contains a placeholder test
(`SmokeTest`) and an empty `package-info.java`. That placeholder test is there so you
always have a green build to come back to; delete it once the first filter (next
milestone) has its own tests.

## Target layout

By the end of milestone 4 you'll have:

```
src/main/java/com/isaqb/practice/pipesandfilters/
  Filter.java                    # the pipe-and-filter contract every filter implements
  LogEntry.java                  # domain type: one parsed log line
  LogLineParser.java             # filter 1: raw String -> LogEntry
  DropDebugFilter.java           # filter 2: drops DEBUG-level entries
  RedactSecretsFilter.java       # filter 3: scrubs anything that looks like a token
  SeverityCount.java             # domain type: one severity level + how many entries
  SeverityCountAggregator.java   # filter 4: LogEntry -> SeverityCount summary
  Pipeline.java                  # composes a chain of filters into one filter
  Main.java                      # composition root: wires the four filters and runs them
```

Notice every filter implements the exact same `Filter<I, O>` shape regardless of what
it does internally — that uniformity is what lets `Pipeline` chain them together
without knowing anything about parsing, redaction, or aggregation specifically. You'll
write `Filter<I, O>` yourself in the next milestone; by milestone 4 you'll see all four
filters composed through it.

## The case study, one more time

You're building PipelineForge's **Log Ingestion Pipeline**: build agents emit raw log
lines while a pipeline runs, and before those lines are searchable by the Observability
team, they need to be parsed, cleaned, redacted, and summarized. A raw log line looks
like this (you'll parse this exact format in milestone 1):

```
2024-05-01T10:15:30Z INFO Starting build for pipeline nightly-build
2024-05-01T10:15:31Z DEBUG Resolved dependency cache at /var/cache/pf
2024-05-01T10:15:32Z ERROR Failed to authenticate with token=sk-live-abcdef1234567890
2024-05-01T10:15:33Z INFO Build finished successfully
```

Each line is `<ISO-8601 timestamp> <LEVEL> <message>`, space-separated, where the
message itself may contain spaces. Four filters will process a batch of these lines in
sequence:

1. **Parse** raw text into structured `LogEntry` values.
2. **Drop** DEBUG-level entries (noise the Observability team doesn't need).
3. **Redact** anything that looks like a leaked token (`token=...`) before it's stored.
4. **Aggregate** the remaining entries into a count per severity level.

Each of those becomes one small, independently testable class. None of them will know
the others exist — that's the point of expressing this as filters connected by pipes
rather than one method that does all four things.

## Checkpoint

- [ ] `mvn -f patterns/02-pipes-and-filters/pom.xml clean verify` passes.
- [ ] You can explain, in one sentence, why a filter that drops DEBUG entries doesn't
      need to know anything about how entries were parsed or what happens after it.

Next: [`01-filter-and-parser.md`](01-filter-and-parser.md).
