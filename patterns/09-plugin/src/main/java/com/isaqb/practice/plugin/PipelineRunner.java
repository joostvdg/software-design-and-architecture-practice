package com.isaqb.practice.plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Executes a pipeline: a list of {@link StepDefinition}s, resolved to behavior purely
 * through {@link PluginRegistry#lookup(String)}. Never references a concrete plugin
 * class — that's the whole point of this exercise.
 */
public class PipelineRunner {

    private final PluginRegistry registry;

    public PipelineRunner(PluginRegistry registry) {
        this.registry = registry;
    }

    /**
     * Runs each step in order. Stops at (and includes) the first failed step's result —
     * does not run steps after a failure. Returns the accumulated results either way.
     */
    public PipelineRunResult run(List<StepDefinition> steps) {
        // TODO: for each StepDefinition, look up its plugin via registry.lookup(pluginId),
        // build a StepContext from its params, call execute(), and collect the StepResult.
        // Stop as soon as one StepResult has success() == false; don't run later steps.

        List<StepResult> stepResults = new ArrayList<>();

        for (StepDefinition step : steps) {
            StepContext context = new StepContext(step.params());
            var plugin = registry.lookup(step.pluginId());
            if (plugin == null) {
                throw new RuntimeException("No plugin found for step " + step.params());
            }
            StepResult result = plugin.execute(context);
            if (result != null) {
                stepResults.add(result);
            }
        }

        return new PipelineRunResult(stepResults);
    }
}
