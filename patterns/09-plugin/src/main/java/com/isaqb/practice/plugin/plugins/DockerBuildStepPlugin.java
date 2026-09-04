package com.isaqb.practice.plugin.plugins;

import com.isaqb.practice.plugin.PipelineStepPlugin;
import com.isaqb.practice.plugin.StepContext;
import com.isaqb.practice.plugin.StepResult;

public class DockerBuildStepPlugin implements PipelineStepPlugin {
    @Override
    public String id() {
        return "docker-build";
    }

    @Override
    public StepResult execute(StepContext context) {
        String image = context.require("image");
        String tag = context.require("tag");
        return StepResult.ok("built image " + image + ":" + tag);
    }
}
