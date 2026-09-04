package com.isaqb.practice.plugin;

/** Thrown when registering a plugin id that's already registered. */
public class DuplicatePluginException extends RuntimeException {

    public DuplicatePluginException(String id) {
        super("a plugin is already registered for id: " + id);
    }
}
