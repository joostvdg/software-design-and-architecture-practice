package com.isaqb.practice.plugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds {@link PipelineStepPlugin}s keyed by their {@link PipelineStepPlugin#id()}.
 * The only way {@link PipelineRunner} learns what step types exist.
 */
public class PluginRegistry {

    // TODO: back this with a Map<String, PipelineStepPlugin>.
    private final Map<String, PipelineStepPlugin> plugins;

    public PluginRegistry() {
        plugins = new HashMap<>();
    }

    /**
     * Registers a plugin under its own {@code id()}.
     *
     * @throws DuplicatePluginException if a plugin is already registered under that id.
     */
    public void register(PipelineStepPlugin plugin) {
        if (plugin == null || plugin.id().trim().isEmpty()) {
            throw new IllegalArgumentException("plugin id is null or empty");
        }

        if (plugins.containsKey(plugin.id())) {
            throw new DuplicatePluginException(plugin.id());
        }
        plugins.put(plugin.id(), plugin);
    }

    /**
     * Looks up the plugin registered under {@code id}.
     *
     * @throws UnknownPluginException if no plugin is registered under that id.
     */
    public PipelineStepPlugin lookup(String id) {
        if (plugins.containsKey(id)) {
            return plugins.get(id);
        }
        throw new UnknownPluginException(id);
    }

}
