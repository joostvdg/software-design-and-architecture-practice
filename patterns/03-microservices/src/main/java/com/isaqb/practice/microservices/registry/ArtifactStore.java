package com.isaqb.practice.microservices.registry;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Registry's own in-memory data. Keyed by artifact name; registering an artifact
 * under a name that's already known overwrites it - "latest registered version wins"
 * is this exercise's simplification of a real registry's full version history.
 *
 * This class is intentionally the only place in the module that holds artifact data.
 * HTTP handlers call it; nothing calls into it from outside the registry package.
 */
public final class ArtifactStore {
    private final Map<String, Artifact> byName = new ConcurrentHashMap<>();

    /**
     * Registers (or overwrites) an artifact, keyed by its name. After this call,
     * findByName(artifact.name()) must return this exact artifact.
     */
    public void register(Artifact artifact) {
        // TODO: store `artifact` in `byName`, keyed by artifact.name().

        byName.put(artifact.name(), artifact);
    }

    /**
     * Looks up the most recently registered artifact for the given name.
     * Returns Optional.empty() if nothing has ever been registered under that name.
     */
    public Optional<Artifact> findByName(String name) {
        // TODO: look `name` up in `byName`, wrapping the result in Optional.

        return Optional.ofNullable(byName.get(name));
    }
}
