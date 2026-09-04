package com.isaqb.practice.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PluginRegistryTest {

    private final PluginRegistry registry = new PluginRegistry();

    @Test
    void registersAndLooksUpById() {
        var echo = new EchoPlugin();
        registry.register(echo);

        assertSame(echo, registry.lookup("echo"));
    }

    @Test
    void unknownIdThrows() {
        assertThrows(UnknownPluginException.class, () -> registry.lookup("nope"));
    }

    @Test
    void duplicateIdThrows() {
        registry.register(new EchoPlugin());

        assertThrows(DuplicatePluginException.class, () -> registry.register(new EchoPlugin()));
    }
}