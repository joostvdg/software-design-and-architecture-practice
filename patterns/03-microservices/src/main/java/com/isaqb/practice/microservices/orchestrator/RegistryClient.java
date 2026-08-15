package com.isaqb.practice.microservices.orchestrator;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * The Orchestrator's side of the wire contract RegistryRequestHandler implements on
 * the Registry side (milestone 2). The two packages never share code for this - only
 * the field names ("name", "version", "digest") are agreed on, the same way two
 * independently-owned real services would agree on an API contract without sharing an
 * implementation.
 */
public final class RegistryClient {

    private final HttpClient httpClient;
    private final URI registryBaseUri;
    private final String artifactPath = "artifacts";

    public RegistryClient(URI registryBaseUri) {
        this(registryBaseUri, HttpClient.newHttpClient());
    }

    public RegistryClient(URI registryBaseUri, HttpClient httpClient) {
        this.registryBaseUri = registryBaseUri;
        this.httpClient = httpClient;
    }

    /**
     * Registers an artifact with the Registry by POSTing to
     * "{registryBaseUri}/artifacts" with body "name=<name>&version=<version>&digest=<digest>"
     * - the same wire format RegistryRequestHandler.handleRegister expects. Build that
     * body yourself here (three fields, fixed order); don't import anything from the
     * registry package to do it - duplicating three lines of string-building is a much
     * smaller cost than coupling the two services' code together.
     *
     * Must:
     *  - build and send the POST request via `httpClient`, to registryBaseUri + "/artifacts"
     *  - return normally (void) if the response status is 201
     *  - otherwise throw RegistryUnavailableException - both for a non-201 response
     *    (include the status in the message) and for a failed call (IOException /
     *    InterruptedException; wrap the cause, and if you catch InterruptedException,
     *    re-set the thread's interrupt flag with Thread.currentThread().interrupt()
     *    before throwing)
     */
    public void register(String name, String version, String digest)
            throws RegistryUnavailableException {
        // TODO: implement per the contract above.


        String body = String.format("name=%s&version=%s&digest=%s", name, version, digest);

        HttpRequest request = HttpRequest.newBuilder(registryBaseUri.resolve("/" + artifactPath))
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 201) {
                throw new RegistryUnavailableException(String.format("HTTP response code %d", response.statusCode()));
            }
        } catch (InterruptedException | IOException e) {
            Thread.currentThread().interrupt();
            throw new RegistryUnavailableException("RegistryClient interrupted");
        }
    }
}
