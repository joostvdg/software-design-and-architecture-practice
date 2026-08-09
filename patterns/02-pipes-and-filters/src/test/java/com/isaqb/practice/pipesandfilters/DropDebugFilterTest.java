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