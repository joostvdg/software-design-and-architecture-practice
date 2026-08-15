package com.isaqb.practice.microservices.registry;


import java.util.HashMap;
import java.util.Map;

/**
 * A tiny hand-rolled wire format for this exercise's HTTP APIs: fields joined as
 * "key=value" pairs separated by "&", e.g.
 * "name=web-app&version=1.4.2&digest=sha256:abc123". No percent-encoding is performed
 * - this exercise's values never contain '&' or '='. A real system would use a real
 * serialization format (or at least proper URL-encoding); this repo disallows JSON
 * libraries, so we hand-roll the smallest thing that works.
 */
public final class ArtifactWireFormat {
    private ArtifactWireFormat() {}

    /** Encodes an artifact's fields in a fixed order: name, version, digest. */
    public static String encodeArtifact(Artifact artifact) {
        // TODO: return "name=<name>&version=<version>&digest=<digest>".

        return String.format("name=%s&version=%s&digest=%s", artifact.name(), artifact.version(), artifact.digest());
    }

    /**
     * Parses a "key=value&key=value" body into a Map. Segments without an '=' are
     * ignored. An empty or blank body decodes to an empty map.
     */
    public static Map<String, String> decode(String body) {
        // TODO: split `body` on '&', then split each non-empty segment on the *first*
        // '=' into a key and a value (a digest like "sha256:abc" has no '=' in it, but
        // splitting on the first '=' is still the safest approach in general), collecting
        // them into a Map. A LinkedHashMap is a fine, simple choice.
        Map<String, String> mappedArtifactStructure = new HashMap<>();
        String[] parts = body.split("&");
        for (String part : parts) {
            String[] keyValue = part.split("=");
            if (keyValue.length == 2) {
                mappedArtifactStructure.put(keyValue[0], keyValue[1]);
            }
        }

        System.out.println(mappedArtifactStructure);
        return mappedArtifactStructure;
    }
}
