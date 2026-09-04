package com.isaqb.practice.plugin.plugins;

import com.isaqb.practice.plugin.PipelineStepPlugin;
import com.isaqb.practice.plugin.StepContext;
import com.isaqb.practice.plugin.StepResult;

public class NotifyStepPlugin implements PipelineStepPlugin {
    @Override
    public String id() {
        return "notify";
    }

    @Override
    public StepResult execute(StepContext context) {
        String channel = context.params().get("channel");
        String message = context.params().get("message");

        if (channel.trim().isEmpty() ) {
            return StepResult.failed("channel is empty");
        }
        if (message.trim().isEmpty() ) {
            return StepResult.failed("message is empty");
        }
        if (channel.trim().equalsIgnoreCase("email")  ||  channel.trim().equalsIgnoreCase("email")) {
            return StepResult.ok("notified " + channel + ": " + message);
        }
        return StepResult.failed("unknown channel");

    }
}
