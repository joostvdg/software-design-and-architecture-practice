package com.isaqb.practice.eventsourcing.projection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.isaqb.practice.eventsourcing.event.PipelineRunEvent;
import com.isaqb.practice.eventsourcing.event.RunFinished;
import com.isaqb.practice.eventsourcing.event.RunStarted;
import com.isaqb.practice.eventsourcing.event.StageCompleted;
import com.isaqb.practice.eventsourcing.event.StageFailed;
import com.isaqb.practice.eventsourcing.event.StageStarted;
import com.isaqb.practice.eventsourcing.state.PipelineRunState;
import com.isaqb.practice.eventsourcing.state.RunStatus;
import com.isaqb.practice.eventsourcing.state.StageStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PipelineRunProjectorTest {

    private static Instant t(int seconds) {
        return Instant.parse("2026-01-01T00:00:00Z").plusSeconds(seconds);
    }

    @Test
    void replayingAnEmptyListReturnsTheUnknownState() {
        PipelineRunState state = PipelineRunProjector.replay("run-1", List.of());

        assertEquals(PipelineRunState.unknown("run-1"), state);
    }

    @Test
    void runStartedSetsRunningStatusAndPipelineName() {
        List<PipelineRunEvent> events = List.of(new RunStarted("run-1", "nightly-build", t(0)));

        PipelineRunState state = PipelineRunProjector.replay("run-1", events);

        assertEquals(RunStatus.RUNNING, state.status());
        assertEquals("nightly-build", state.pipelineName());
    }

    @Test
    void aFullSuccessfulRunProducesSucceededStatusAndAllStagesSucceeded() {
        List<PipelineRunEvent> events =
                List.of(
                        new RunStarted("run-1", "nightly-build", t(0)),
                        new StageStarted("run-1", "compile", t(1)),
                        new StageCompleted("run-1", "compile", t(2)),
                        new StageStarted("run-1", "test", t(3)),
                        new StageCompleted("run-1", "test", t(4)),
                        new RunFinished("run-1", true, t(5)));

        PipelineRunState state = PipelineRunProjector.replay("run-1", events);

        assertEquals(RunStatus.SUCCEEDED, state.status());
        assertEquals(2, state.stages().size());
        assertEquals("compile", state.stages().get(0).name());
        assertEquals(StageStatus.SUCCEEDED, state.stages().get(0).status());
        assertEquals("test", state.stages().get(1).name());
        assertEquals(StageStatus.SUCCEEDED, state.stages().get(1).status());
    }

    @Test
    void aRetriedStageHasAttemptsGreaterThanOneAndKeepsItsOriginalPosition() {
        List<PipelineRunEvent> events =
                List.of(
                        new RunStarted("run-1", "nightly-build", t(0)),
                        new StageStarted("run-1", "compile", t(1)),
                        new StageFailed("run-1", "compile", "flaky download", t(2)),
                        new StageStarted("run-1", "compile", t(3)),
                        new StageCompleted("run-1", "compile", t(4)),
                        new StageStarted("run-1", "test", t(5)),
                        new StageCompleted("run-1", "test", t(6)),
                        new RunFinished("run-1", true, t(7)));

        PipelineRunState state = PipelineRunProjector.replay("run-1", events);

        var compile = state.stages().get(0);
        assertEquals("compile", compile.name());
        assertEquals(2, compile.attempts());
        assertEquals(StageStatus.SUCCEEDED, compile.status());
        assertEquals(RunStatus.SUCCEEDED, state.status());
    }

    @Test
    void aFailedStageThatIsNeverRetriedProducesAFailedRun() {
        List<PipelineRunEvent> events =
                List.of(
                        new RunStarted("run-1", "nightly-build", t(0)),
                        new StageStarted("run-1", "compile", t(1)),
                        new StageFailed("run-1", "compile", "out of disk space", t(2)),
                        new RunFinished("run-1", false, t(3)));

        PipelineRunState state = PipelineRunProjector.replay("run-1", events);

        assertEquals(RunStatus.FAILED, state.status());
        assertEquals(StageStatus.FAILED, state.stages().get(0).status());
    }

    @Test
    void replayFromContinuesFoldingOntoANonEmptyInitialState() {
        PipelineRunState afterTwoEvents =
                PipelineRunProjector.replay(
                        "run-1",
                        List.of(
                                new RunStarted("run-1", "nightly-build", t(0)),
                                new StageStarted("run-1", "compile", t(1))));

        PipelineRunState finalState =
                PipelineRunProjector.replayFrom(
                        afterTwoEvents,
                        List.of(
                                new StageCompleted("run-1", "compile", t(2)),
                                new RunFinished("run-1", true, t(3))));

        PipelineRunState fullReplay =
                PipelineRunProjector.replay(
                        "run-1",
                        List.of(
                                new RunStarted("run-1", "nightly-build", t(0)),
                                new StageStarted("run-1", "compile", t(1)),
                                new StageCompleted("run-1", "compile", t(2)),
                                new RunFinished("run-1", true, t(3))));

        assertEquals(fullReplay, finalState);
    }

    @Test
    void aStageCompletedWithNoPriorStageStartedIsRejected() {
        List<PipelineRunEvent> events =
                List.of(
                        new RunStarted("run-1", "nightly-build", t(0)),
                        new StageCompleted("run-1", "compile", t(1)));

        assertThrows(IllegalStateException.class, () -> PipelineRunProjector.replay("run-1", events));
    }
}