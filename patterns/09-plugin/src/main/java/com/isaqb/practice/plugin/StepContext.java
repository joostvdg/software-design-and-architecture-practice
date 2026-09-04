package com.isaqb.practice.plugin;

import java.util.Map;

/** Everything a plugin needs to execute one step: its declared parameters. */
public record StepContext(Map<String, String> params) {

    public StepContext {
        params = Map.copyOf(params);
    }

    public String require(String key) {
        var value = params.get(key);
        if (value == null) {
            throw new IllegalArgumentException("missing required parameter: " + key);
        }
        return value;
    }
}
