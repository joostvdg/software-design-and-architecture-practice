package com.isaqb.practice.cqrs.command;

/**
 * The seam between the write and read sides. PipelineRunCommandService calls this
 * after every successful command - it has no idea whether the implementation updates
 * a projection synchronously in the same thread (this exercise) or hands the run off
 * to an async queue for a separate consumer to project later (the real-world,
 * eventually consistent version - see the thought experiment at the end of this
 * milestone, and README section 3).
 */
public interface PipelineRunChangeListener {

    void onRunChanged(PipelineRun run);
}