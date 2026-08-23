package com.isaqb.practice.cqrs.query;

import com.isaqb.practice.cqrs.command.PipelineRun;
import com.isaqb.practice.cqrs.command.PipelineRunChangeListener;

/**
 * Updates the read-side projection synchronously, in the same thread, immediately
 * after the command that changed it. This is what keeps this exercise's read side
 * always consistent with the write side - and exactly the piece you'd swap out for
 * genuine eventual consistency. See the thought experiment below.
 */
public class SynchronousProjectionUpdater implements PipelineRunChangeListener {

    private final PipelineRunQueryService queryService;

    public SynchronousProjectionUpdater(PipelineRunQueryService queryService) {
        this.queryService = queryService;
    }

    @Override
    public void onRunChanged(PipelineRun run) {
        queryService.updateProjection(run);
    }
}
