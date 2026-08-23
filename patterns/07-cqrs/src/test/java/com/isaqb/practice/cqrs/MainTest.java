package com.isaqb.practice.cqrs;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.isaqb.practice.cqrs.query.PipelineRunSummary;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class MainTest {

    @Test
    void formatsARunningSummary() {
        var summary = new PipelineRunSummary("run-1", "RUNNING", 2, 3, "package", Duration.ofSeconds(12));

        String formatted = Main.formatSummary(summary);

        assertTrue(formatted.contains("run-1"));
        assertTrue(formatted.contains("RUNNING"));
        assertTrue(formatted.contains("2"));
        assertTrue(formatted.contains("3"));
        assertTrue(formatted.contains("package"));
    }

    @Test
    void formatsAFinishedSummaryWithNoCurrentStage() {
        var summary = new PipelineRunSummary("run-1", "FINISHED", 3, 3, null, Duration.ofSeconds(18));

        String formatted = Main.formatSummary(summary);

        assertTrue(formatted.contains("FINISHED"));
        assertTrue(formatted.toLowerCase().contains("none"));
    }
}