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