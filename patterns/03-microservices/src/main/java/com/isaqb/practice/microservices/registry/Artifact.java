package com.isaqb.practice.microservices.registry;

/**
 * A built artifact as the Registry knows it: which pipeline output it is (name),
 * which version, and the content digest that identifies the exact bytes that were
 * built. This is the Registry's own data - nothing outside this package should ever
 * need to construct one directly except through ArtifactStore.
 */
public record Artifact(String  name, String version, String digest) {
}
