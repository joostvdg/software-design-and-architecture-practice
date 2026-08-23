package com.isaqb.practice.cqrs;

import com.isaqb.practice.cqrs.command.PipelineRunChangeListener;
import com.isaqb.practice.cqrs.command.PipelineRunCommandService;
import com.isaqb.practice.cqrs.query.PipelineRunQueryService;
import com.isaqb.practice.cqrs.query.PipelineRunSummary;
import com.isaqb.practice.cqrs.query.SynchronousProjectionUpdater;

import java.time.Clock;
import java.util.List;

/**
 * Composition root: wires the query side's SynchronousProjectionUpdater into the
 * command side as its PipelineRunChangeListener, then drives one pipeline run through
 * both the command side (Pipeline Runner's view) and the query side (Dashboard's view)
 * to demonstrate the split end to end.
 */
public final class Main {

    private Main() {}

    public static void main(String[] args) {
        Clock clock = Clock.systemUTC();
        PipelineRunQueryService queryService = new PipelineRunQueryService(clock);
        PipelineRunChangeListener listener = new SynchronousProjectionUpdater(queryService);
        PipelineRunCommandService commandService = new PipelineRunCommandService(clock, listener);


        // Pipeline Runner's view: issue commands.
        commandService.startRun("nightly-build-42", List.of("compile", "test", "package"));
        commandService.completeStage("nightly-build-42", "compile");
        commandService.completeStage("nightly-build-42", "test");

        // Dashboard's view: read the projection, already up to date - no explicit
        // updateProjection call needed here, unlike milestone 3's tests.
        System.out.println(formatSummary(queryService.getSummary("nightly-build-42").orElseThrow()));

        commandService.completeStage("nightly-build-42", "package");
        commandService.finishRun("nightly-build-42");

        System.out.println(formatSummary(queryService.getSummary("nightly-build-42").orElseThrow()));

    }

    /**
     * Returns a formatted summary.
     *
     * For example, when in progress: "nightly-build-42: RUNNING, 2/3 stages done, current=package, duration=PT12.3S".
     *
     * And a completed example: "nightly-build-42: FINISHED, 3/3 stages done, current=none, duration=PT18S".
     *
     * @param summary the @{PipelineRunSummary} to format.
     * @return Single line String representation
     */
    static String formatSummary(PipelineRunSummary summary) {
        int totalNumberOfStages = summary.stagesTotal();
        int completedNumberOfStages = summary.stagesCompleted();
        String currentStage = "none";
        if (summary.currentStage() != null) {
            currentStage = summary.currentStage();
        }

        String summaryLine = String.format("%s: %s, %d/%d done, current=%s, duration=PT%ds",
                summary.runId(),
                summary.status(),
                completedNumberOfStages,
                totalNumberOfStages,
                currentStage,
                summary.duration().toSeconds());

        System.out.println(summaryLine);
        return summaryLine;
    }
}
