package com.isaqb.practice.microservices.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ArtifactWireFormatTest {

    @Test
    void encodesFieldsInFixedOrder() {
        var artifact = new Artifact("web-app", "1.4.2", "sha256:abc123");

        assertEquals(
                "name=web-app&version=1.4.2&digest=sha256:abc123",
                ArtifactWireFormat.encodeArtifact(artifact));
    }

    @Test
    void decodesKeyValuePairs() {
        Map<String, String> fields =
                ArtifactWireFormat.decode("name=web-app&version=1.4.2&digest=sha256:abc123");

        assertEquals("web-app", fields.get("name"));
        assertEquals("1.4.2", fields.get("version"));
        assertEquals("sha256:abc123", fields.get("digest"));
    }

    @Test
    void decodesEmptyBodyToEmptyMap() {
        assertTrue(ArtifactWireFormat.decode("").isEmpty());
    }

    @Test
    void ignoresSegmentsWithoutEquals() {
        Map<String, String> fields = ArtifactWireFormat.decode("name=web-app&garbage&version=1.4.2");

        assertEquals(2, fields.size());
        assertEquals("web-app", fields.get("name"));
    }
}