package com.isaqb.practice.plugin;

/**
 * The stable contract every pipeline step type implements. The {@link PipelineRunner}
 * (milestone 4) depends on this interface and on {@link PluginRegistry} only — never
 * on a concrete implementation.
 */
public interface PipelineStepPlugin {

    /** The type id this plugin registers under, e.g. {@code "shell"}. Must be stable. */
    String id();

    /** Executes one step. Must not throw for expected failures — return a failed result. */
    StepResult execute(StepContext context);
}
