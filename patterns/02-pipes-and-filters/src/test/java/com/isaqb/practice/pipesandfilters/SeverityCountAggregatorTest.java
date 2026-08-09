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