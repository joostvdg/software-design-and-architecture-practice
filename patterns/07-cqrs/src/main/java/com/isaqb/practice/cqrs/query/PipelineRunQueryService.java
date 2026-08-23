package com.isaqb.practice.cqrs.query;

import com.isaqb.practice.cqrs.command.PipelineRun;
import com.isaqb.practice.cqrs.command.PipelineStage;
import com.isaqb.practice.cqrs.command.RunStatus;
import com.isaqb.practice.cqrs.command.StageStatus;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The read side of CQRS: serves PipelineRunSummary from its own projection store,
 * never from a PipelineRun directly. Rebuilding the projection store is a deliberate,
 * explicit step (updateProjection) - see milestone 4 for who calls it and when.
 */
public class PipelineRunQueryService {

    private final Map<String, PipelineRunSummary> projections = new ConcurrentHashMap<>();
    private final Clock clock;

    public PipelineRunQueryService(final Clock clock) {
        this.clock = clock;
    }

    public Optional<PipelineRunSummary> getSummary(String runId) {
        return Optional.ofNullable(projections.get(runId));
    }

    public List<PipelineRunSummary> listSummaries() {
        return List.copyOf(projections.values());
    }

    /**
     * Rebuilds the projection for one run from its current write-side state, and stores
     * it, keyed by run.id() - overwriting whatever was there before.
     *
     * TODO: build a PipelineRunSummary from `run`:
     *  - runId: run.id()
     *  - status: run.status().name()
     *  - stagesCompleted: how many of run.stages() have status() == StageStatus.COMPLETE
     *  - stagesTotal: run.stages().size()
     *  - currentStage: the name() of the first stage (in list order) whose status() is
     *    StageStatus.PENDING, or null if none are pending
     *  - duration: Duration.between(run.startedAt(), run.finishedAt().orElse(clock.instant()))
     *    - i.e. "how long it's run so far" while still running, or the final duration
     *    once finished
     * Then put it into `projections` under run.id(), replacing any previous entry.
     */
    public void updateProjection(PipelineRun run) {
        int stagesCompleted = 0;
        String currentStage = null;
        boolean currentStageSet = false;
        if (run.status() == RunStatus.FINISHED) {
            stagesCompleted = run.stages().size();
        } else {
            for (PipelineStage stage : run.stages()) {
                if (stage.status() == StageStatus.COMPLETE) {
                    stagesCompleted++;
                } else if (!currentStageSet) {
                    currentStage = stage.name();
                    currentStageSet = true;
                }
            }
        }

        Duration duration = Duration.between(run.startedAt(), run.finishedAt().orElse(clock.instant()));

        PipelineRunSummary summary = new PipelineRunSummary(
                run.id(),
                run.status().toString(),
                stagesCompleted,
                run.stages().size(),
                currentStage,
                duration
        );

        projections.put(run.id(), summary);
    }
}
