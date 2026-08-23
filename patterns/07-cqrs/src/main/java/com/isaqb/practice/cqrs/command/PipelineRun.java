package com.isaqb.practice.cqrs.command;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The write-side aggregate: the one source of truth for a pipeline run's state.
 * Every mutating method is package-private - only {@code PipelineRunCommandService},
 * in this same package, is allowed to call them. The `query` package only ever sees
 * this class's public (read-only) surface.
 */
public final class PipelineRun {

    private final String id;
    private final List<PipelineStage> stages;
    private final Instant startedAt;
    private RunStatus status;
    private Instant finishedAt;

    public PipelineRun(String id, List<String> stageNames, Instant startedAt) {
        this.id = id;
        this.stages = new ArrayList<>();
        this.startedAt = startedAt;
        for (String stageName : stageNames) {
            this.stages.add(new PipelineStage(stageName));
        }
        this.status = RunStatus.RUNNING;
    }

    public String id() {
        return id;
    }

    public List<PipelineStage> stages() {
        return List.copyOf(stages);
    }

    public RunStatus status() {
        return status;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Optional<Instant> finishedAt() {
        return Optional.ofNullable(finishedAt);
    }

    Optional<PipelineStage> findStage(String stageName) {
        return stages.stream().filter(s -> s.name().equals(stageName)).findFirst();
    }

    boolean allStagesComplete() {
        return stages.stream().allMatch(stage -> stage.status() == StageStatus.COMPLETE);
    }

    void finish(Instant finishedAt) {
        this.status = RunStatus.FINISHED;
        this.finishedAt = finishedAt;
    }
}
