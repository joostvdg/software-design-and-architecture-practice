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