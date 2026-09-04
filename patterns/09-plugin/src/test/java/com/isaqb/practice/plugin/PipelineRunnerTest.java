package com.isaqb.practice.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PipelineRunnerTest {

    @Test
    void runsAllStepsInOrderWhenAllSucceed() {
        var registry = new PluginRegistry();
        registry.register(new EchoPlugin());

        var runner = new PipelineRunner(registry);
        var steps = List.of(
                new StepDefinition("echo", Map.of("message", "first")),
                new StepDefinition("echo", Map.of("message", "second")));

        var result = runner.run(steps);

        assertTrue(result.allSucceeded());
        assertEquals(2, result.stepResults().size());
        assertEquals("first", result.stepResults().get(0).message());
        assertEquals("second", result.stepResults().get(1).message());
    }

    @Test
    void unknownPluginIdPropagatesAsUnknownPluginException() {
        var runner = new PipelineRunner(new PluginRegistry());

        org.junit.jupiter.api.Assertions.assertThrows(
                UnknownPluginException.class,
                () -> runner.run(List.of(new StepDefinition("nope", Map.of()))));
    }
}