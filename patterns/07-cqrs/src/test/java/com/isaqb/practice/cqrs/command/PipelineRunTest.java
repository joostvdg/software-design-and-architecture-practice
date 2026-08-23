package com.isaqb.practice.cqrs.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PipelineRunTest {

    @Test
    void startsWithAllStagesPendingAndStatusRunning() {
        var run = new PipelineRun("run-1", List.of("compile", "test"), Instant.EPOCH);

        assertEquals(RunStatus.RUNNING, run.status());
        assertEquals(2, run.stages().size());
        assertTrue(run.stages().stream().allMatch(s -> s.status() == StageStatus.PENDING));
        assertTrue(run.finishedAt().isEmpty());
    }

    @Test
    void stagesAreReturnedAsAnImmutableSnapshot() {
        var run = new PipelineRun("run-1", List.of("compile"), Instant.EPOCH);

        List<PipelineStage> stages = run.stages();

        assertThrows(UnsupportedOperationException.class, () -> stages.add(new PipelineStage("extra")));
    }
}