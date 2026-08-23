package com.isaqb.practice.cqrs.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.isaqb.practice.cqrs.command.PipelineRunCommandService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class PipelineRunQueryServiceTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(START, ZoneOffset.UTC);

    private final PipelineRunCommandService commandService =
            new PipelineRunCommandService(FIXED_CLOCK);
    private final PipelineRunQueryService queryService = new PipelineRunQueryService(FIXED_CLOCK);

    @Test
    void projectsAFreshlyStartedRun() {
        var run = commandService.startRun("run-1", List.of("compile", "test"));

        queryService.updateProjection(run);

        var summary = queryService.getSummary("run-1").orElseThrow();
        assertEquals("run-1", summary.runId());
        assertEquals("RUNNING", summary.status());
        assertEquals(0, summary.stagesCompleted());
        assertEquals(2, summary.stagesTotal());
        assertEquals("compile", summary.currentStage());
        assertEquals(Duration.ZERO, summary.duration());
    }

    @Test
    void projectsPartialProgressAndAdvancesCurrentStage() {
        commandService.startRun("run-1", List.of("compile", "test"));
        var run = commandService.completeStage("run-1", "compile");

        queryService.updateProjection(run);

        var summary = queryService.getSummary("run-1").orElseThrow();
        assertEquals(1, summary.stagesCompleted());
        assertEquals("test", summary.currentStage());
    }

    @Test
    void projectsNoCurrentStageWhenAllStagesComplete() {
        commandService.startRun("run-1", List.of("compile"));
        var run = commandService.completeStage("run-1", "compile");

        queryService.updateProjection(run);

        var summary = queryService.getSummary("run-1").orElseThrow();
        assertEquals(1, summary.stagesCompleted());
        assertNull(summary.currentStage());
    }

    @Test
    void projectionIsOverwrittenNotAccumulatedOnRepeatedUpdates() {
        commandService.startRun("run-1", List.of("compile", "test"));
        queryService.updateProjection(commandService.get("run-1"));
        var run = commandService.completeStage("run-1", "compile");

        queryService.updateProjection(run);

        assertEquals(1, queryService.listSummaries().size());
        assertEquals(1, queryService.getSummary("run-1").orElseThrow().stagesCompleted());
    }

    @Test
    void missingRunHasNoSummaryUntilProjected() {
        assertTrue(queryService.getSummary("never-projected").isEmpty());
    }
}