package com.isaqb.practice.cqrs.command;

/**
 * One stage within a pipeline run. Mutable only from within the `command` package -
 * {@link #markComplete()} is package-private on purpose, so only
 * {@code PipelineRunCommandService} (via {@code PipelineRun}) can ever complete a
 * stage.
 */
public final class PipelineStage {

    private final String name;
    private StageStatus status;

    public PipelineStage(String name) {
        this.name = name;
        this.status = StageStatus.PENDING;
    }

    public String name() {
        return name;
    }

    public StageStatus status() {
        return status;
    }

    void markComplete() {
        this.status = StageStatus.COMPLETE;
    }
}
