package com.isaqb.practice.plugin;

import com.isaqb.practice.plugin.plugins.DockerBuildStepPlugin;
import com.isaqb.practice.plugin.plugins.NotifyStepPlugin;
import com.isaqb.practice.plugin.plugins.ShellStepPlugin;

import java.util.List;
import java.util.Map;

/** Composition root: this is the only class allowed to import a concrete plugin. */
public final class Main {

    public static void main(String[] args) {
        var registry = new PluginRegistry();
        registry.register(new ShellStepPlugin());
        registry.register(new NotifyStepPlugin());
        registry.register(new DockerBuildStepPlugin());

        var runner = new PipelineRunner(registry);
        var steps = List.of(
            new StepDefinition("shell", Map.of("command", "./gradlew build")),
            new StepDefinition("docker-build", Map.of("image", "pipelineforge/build-validator", "tag", "1.0.0")),
            new StepDefinition("notify", Map.of("channel", "slack", "message", "build finished"))
        );

        var result = runner.run(steps);
        result.stepResults().forEach(r -> System.out.println((r.success() ? "OK   " : "FAIL ") + r.message()));
        System.out.println("all succeeded: " + result.allSucceeded());
    }

    private Main() {}
}
