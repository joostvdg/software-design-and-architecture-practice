package com.isaqb.practice.plugin.plugins;

import com.isaqb.practice.plugin.PipelineStepPlugin;
import com.isaqb.practice.plugin.StepContext;
import com.isaqb.practice.plugin.StepResult;

/**
 * Simulates running a shell command. This is a practice exercise, not a real process
 * launcher — it never calls {@code ProcessBuilder}; it only validates the {@code
 * command} param and reports success.
 */
public class ShellStepPlugin implements PipelineStepPlugin {
    @Override
    public String id() {
        return "shell";
    }

    @Override
    public StepResult execute(StepContext context) {
        // TODO: read the "command" param via context.require("command").
        // If it's blank (after trim), return StepResult.failed("command must not be blank").
        // Otherwise return StepResult.ok("ran: " + command).
        String command = context.params().get("command");
        if (command.trim().isEmpty()) {
            return StepResult.failed("command most not be blank");
        }
        return StepResult.ok("ran: " + command);
    }
}
