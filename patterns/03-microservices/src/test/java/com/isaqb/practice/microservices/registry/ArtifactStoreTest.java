package com.isaqb.practice.microservices.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ArtifactStoreTest {

    private final ArtifactStore store = new ArtifactStore();

    @Test
    void findByNameIsEmptyWhenNothingRegistered() {
        assertTrue(store.findByName("web-app").isEmpty());
    }

    @Test
    void registerThenFindByNameReturnsIt() {
        var artifact = new Artifact("web-app", "1.4.2", "sha256:abc123");

        store.register(artifact);

        assertEquals(artifact, store.findByName("web-app").orElseThrow());
    }

    @Test
    void registeringAgainUnderSameNameOverwrites() {
        store.register(new Artifact("web-app", "1.4.2", "sha256:abc123"));
        store.register(new Artifact("web-app", "1.5.0", "sha256:def456"));

        var found = store.findByName("web-app").orElseThrow();

        assertEquals("1.5.0", found.version());
        assertEquals("sha256:def456", found.digest());
    }
}