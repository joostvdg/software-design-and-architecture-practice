package com.isaqb.practice.plugin;

import java.util.Map;

/** One entry in a pipeline: which plugin type to invoke, with what params. */
public record StepDefinition(String pluginId, Map<String, String> params) {

    public StepDefinition {
        params = Map.copyOf(params);
    }
}
