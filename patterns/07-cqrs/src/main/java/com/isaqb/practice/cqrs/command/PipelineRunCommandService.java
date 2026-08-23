package com.isaqb.practice.cqrs.command;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The write side of CQRS: the only class allowed to create or mutate a PipelineRun.
 * Each public method is one command a Pipeline Runner can issue.
 */
public class PipelineRunCommandService {
    private final Map<String, PipelineRun> runs = new ConcurrentHashMap<>();
    private final Clock clock;
    private final PipelineRunChangeListener listener;

    public PipelineRunCommandService(Clock clock) {
        this(clock, run -> {});
    }

    public PipelineRunCommandService(Clock clock, PipelineRunChangeListener listener) {
        this.clock = clock;
        this.listener = listener;
    }

    /**
     * Looks up a run by id, or throws if it doesn't exist.
     * @param runId the identifier of the PipelineRun to find
     * @return found PipelineRun or throws @PipelineRunNotFoundException
     */
    public PipelineRun get(String runId) {
        PipelineRun run = runs.get(runId);
        if (run == null) {
            throw new PipelineRunNotFoundException(runId);
        }

        listener.onRunChanged(run);
        return run;
    }

    /**
     * Starts a new pipeline run with the given stages, all initially PENDING.
     *
     * TODO:
     *  - Reject a blank runId or an empty stageNames list with InvalidCommandException.
     *  - Reject a runId that's already in use with InvalidCommandException (a run id
     *    must be unique - starting the same run twice is a bug in the caller, not
     *    something to silently overwrite).
     *  - Otherwise, create a new PipelineRun (use clock.instant() for startedAt), store
     *    it, and return it.
     */
    public PipelineRun startRun(String runId, List<String> stageNames) {
        if (runId.trim().isEmpty() || stageNames.isEmpty()) {
            throw new InvalidCommandException("runId and stageNames are mandatory");
        }

        if (runs.containsKey(runId)) {
            throw new InvalidCommandException("runId already exists");
        }

        PipelineRun run = new PipelineRun(runId,stageNames, Instant.now(clock));
        runs.put(runId, run);

        listener.onRunChanged(run);
        return run;
    }

    /**
     * Marks one stage of an existing run as complete.
     *
     * TODO:
     *  - Look the run up via get(runId) (throws PipelineRunNotFoundException if missing
     *    - nothing more to do there).
     *  - Reject if the run's status is already FINISHED (InvalidCommandException - you
     *    can't complete a stage on a run that's already done).
     *  - Reject if no stage with that name exists (InvalidCommandException). Hint:
     *    run.findStage(stageName) is package-private and visible here.
     *  - Reject if that stage is already COMPLETE (InvalidCommandException - completing
     *    the same stage twice is a bug in the caller, not a no-op).
     *  - Otherwise, mark it complete and return the run. Hint: PipelineStage.markComplete()
     *    is package-private but PipelineRun doesn't expose a public "complete this stage"
     *    method - you're expected to call stage.markComplete() directly here, since
     *    PipelineRunCommandService is in the same package as PipelineStage.
     */
    public PipelineRun completeStage(String runId, String stageName) {
        var run = runs.get(runId);
        if (run == null) {
            throw new PipelineRunNotFoundException(runId);
        }

        if (run.status() == RunStatus.FINISHED) {
            throw new InvalidCommandException("run already finished");
        }

        var stage = run.findStage(stageName);
        if (stage.isEmpty()) {
            throw new InvalidCommandException("stage not found");
        }

        if (stage.get().status() == StageStatus.COMPLETE) {
            throw new InvalidCommandException("stage already completed");
        }
        stage.get().markComplete();

        listener.onRunChanged(run);
        return run;
    }

    /**
     * Finishes an existing run.
     *
     * TODO:
     *  - Look the run up via get(runId).
     *  - Reject if the run's status is already FINISHED (InvalidCommandException).
     *  - Reject if any stage is not yet COMPLETE (InvalidCommandException - a run can
     *    only finish once every stage is done). Hint: run.allStagesComplete() is
     *    package-private and visible here.
     *  - Otherwise, call run.finish(clock.instant()) and return the run.
     */
    public PipelineRun finishRun(String runId) {
        var run = runs.get(runId);
        if (run == null) {
            throw new PipelineRunNotFoundException(runId);
        }

        if  (run.status() == RunStatus.FINISHED) {
            throw new InvalidCommandException("run already finished");
        }

        if (!run.allStagesComplete()) {
            throw new InvalidCommandException("no all stages completed");
        }

        run.finish(Instant.now(clock));

        listener.onRunChanged(run);
        return run;
    }
}
