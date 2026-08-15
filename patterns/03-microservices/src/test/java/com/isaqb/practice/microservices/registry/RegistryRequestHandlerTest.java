package com.isaqb.practice.microservices.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RegistryRequestHandlerTest {

    private final ArtifactStore store = new ArtifactStore();
    private final RegistryRequestHandler handler = new RegistryRequestHandler(store);

    @Test
    void registersAValidArtifact() {
        var result = handler.handleRegister("name=web-app&version=1.4.2&digest=sha256:abc123");

        assertEquals(201, result.status());
        assertTrue(result.body().contains("name=web-app"));
        assertTrue(result.body().contains("version=1.4.2"));
        assertEquals("sha256:abc123", store.findByName("web-app").orElseThrow().digest());
    }

    @Test
    void rejectsRegistrationMissingAField() {
        var result = handler.handleRegister("name=web-app&version=1.4.2");

        assertEquals(400, result.status());
        assertTrue(store.findByName("web-app").isEmpty());
    }

    @Test
    void looksUpARegisteredArtifact() {
        store.register(new Artifact("web-app", "1.4.2", "sha256:abc123"));

        var result = handler.handleLookup("web-app");

        assertEquals(200, result.status());
        assertTrue(result.body().contains("sha256:abc123"));
    }

    @Test
    void lookupOfUnknownNameIs404() {
        var result = handler.handleLookup("does-not-exist");

        assertEquals(404, result.status());
    }
}