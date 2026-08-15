package com.isaqb.practice.microservices.registry;

import java.util.Map;

/**
 * The Registry's core request logic, deliberately kept free of any HttpExchange /
 * HttpServer detail so it's trivially unit-testable - RegistryMain (next step) is the
 * only class that touches the JDK HTTP types directly.
 */
public final class RegistryRequestHandler {
    private final ArtifactStore store;

    public RegistryRequestHandler(ArtifactStore store) {
        this.store = store;
    }

    /**
     * Handles a POST /artifacts request. `body` is the raw request body in the wire
     * format above. Must:
     *  - decode `body` with ArtifactWireFormat.decode
     *  - if "name", "version", or "digest" is missing or blank, return
     *    HttpResult(400, "status=error&message=<short reason>")
     *  - otherwise, build an Artifact, register it via `store`, and return
     *    HttpResult(201, "status=registered&" + ArtifactWireFormat.encodeArtifact(artifact))
     */
    public HttpResult handleRegister(String body) {
        var artifactMapping = ArtifactWireFormat.decode(body);
        if (artifactMapping.isEmpty() || !artifactMapping.containsKey("name") || !artifactMapping.containsKey("version") || !artifactMapping.containsKey("digest")) {
            return new HttpResult(400, "status=error&message=one or more fields not found");
        }
        var name = artifactMapping.get("name");
        var version = artifactMapping.get("version");
        var digest = artifactMapping.get("digest");

        Artifact artifact = new Artifact(name, version, digest);
        store.register(artifact);
        return new HttpResult(201, String.format("status=success&message=artifact name=%s,version=%s registered successfully", name, version));
    }

    /**
     * Handles a GET /artifacts/{name} request.
     *  - look `name` up via store.findByName
     *  - if present: HttpResult(200, ArtifactWireFormat.encodeArtifact(artifact))
     *  - if absent: HttpResult(404, "status=error&message=artifact not found: " + name)
     */
    public HttpResult handleLookup(String name) {
        var artifact = store.findByName(name);
        if (artifact.isEmpty()) {
            return new HttpResult(404, "status=error&message=artifact not found");
        }
        var encodedArtifact = ArtifactWireFormat.encodeArtifact(artifact.get());
        return new HttpResult(200, encodedArtifact);
    }

}
