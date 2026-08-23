package com.isaqb.practice.cqrs.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class PipelineRunCommandServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    private final PipelineRunCommandService service = new PipelineRunCommandService(FIXED_CLOCK);

    @Test
    void startRunCreatesARunningRunWithPendingStages() {
        PipelineRun run = service.startRun("run-1", List.of("compile", "test"));

        assertEquals(RunStatus.RUNNING, run.status());
        assertEquals(2, run.stages().size());
        assertTrue(run.stages().stream().allMatch(s -> s.status() == StageStatus.PENDING));
    }

    @Test
    void startRunRejectsDuplicateRunId() {
        service.startRun("run-1", List.of("compile"));

        assertThrows(
                InvalidCommandException.class, () -> service.startRun("run-1", List.of("compile")));
    }

    @Test
    void startRunRejectsEmptyStageList() {
        assertThrows(InvalidCommandException.class, () -> service.startRun("run-1", List.of()));
    }

    @Test
    void completeStageMarksOnlyTheNamedStageComplete() {
        service.startRun("run-1", List.of("compile", "test"));

        PipelineRun run = service.completeStage("run-1", "compile");

        var compile = run.stages().stream().filter(s -> s.name().equals("compile")).findFirst().orElseThrow();
        var test = run.stages().stream().filter(s -> s.name().equals("test")).findFirst().orElseThrow();
        assertEquals(StageStatus.COMPLETE, compile.status());
        assertEquals(StageStatus.PENDING, test.status());
    }

    @Test
    void completeStageRejectsUnknownRun() {
        assertThrows(
                PipelineRunNotFoundException.class, () -> service.completeStage("no-such-run", "compile"));
    }

    @Test
    void completeStageRejectsUnknownStage() {
        service.startRun("run-1", List.of("compile"));

        assertThrows(
                InvalidCommandException.class, () -> service.completeStage("run-1", "no-such-stage"));
    }

    @Test
    void completeStageRejectsAlreadyCompleteStage() {
        service.startRun("run-1", List.of("compile"));
        service.completeStage("run-1", "compile");

        assertThrows(InvalidCommandException.class, () -> service.completeStage("run-1", "compile"));
    }

    @Test
    void finishRunRejectsIncompleteStages() {
        service.startRun("run-1", List.of("compile", "test"));
        service.completeStage("run-1", "compile");

        assertThrows(InvalidCommandException.class, () -> service.finishRun("run-1"));
    }

    @Test
    void finishRunSucceedsOnceAllStagesComplete() {
        service.startRun("run-1", List.of("compile", "test"));
        service.completeStage("run-1", "compile");
        service.completeStage("run-1", "test");

        PipelineRun run = service.finishRun("run-1");

        assertEquals(RunStatus.FINISHED, run.status());
        assertEquals(FIXED_CLOCK.instant(), run.finishedAt().orElseThrow());
    }

    @Test
    void finishRunRejectsAlreadyFinishedRun() {
        service.startRun("run-1", List.of("compile"));
        service.completeStage("run-1", "compile");
        service.finishRun("run-1");

        assertThrows(InvalidCommandException.class, () -> service.finishRun("run-1"));
    }
}