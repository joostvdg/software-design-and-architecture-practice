package com.isaqb.practice.plugin;

/** Thrown when looking up a plugin id that was never registered. */
public class UnknownPluginException extends RuntimeException  {

    public UnknownPluginException(String id) {
        super("no plugin registered for id: " + id);
    }
}
