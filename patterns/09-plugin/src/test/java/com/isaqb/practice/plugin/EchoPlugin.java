package com.isaqb.practice.plugin;


public class EchoPlugin implements PipelineStepPlugin {

    private static final String ID = "echo";


    @Override
    public String id() {
        return ID;
    }

    @Override
    public StepResult execute(StepContext context) {
        return StepResult.ok(context.require("message"));
    }
}
