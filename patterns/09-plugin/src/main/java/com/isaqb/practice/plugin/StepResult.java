package com.isaqb.practice.plugin;

/** What a plugin reports after executing one step. */
public record StepResult(boolean success, String message) {

    public static StepResult ok(String message) {
        return new StepResult(true, message);
    }

    public static StepResult failed(String message) {
        return new StepResult(false, message);
    }
}
