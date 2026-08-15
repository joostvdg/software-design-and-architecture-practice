package com.isaqb.practice.microservices.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RegistryClientTest {

    private HttpServer fakeRegistry;

    @AfterEach
    void stopFakeRegistry() {
        if (fakeRegistry != null) {
            fakeRegistry.stop(0);
        }
    }

    @Test
    void registersSuccessfullyAgainstA201Response()
            throws IOException, RegistryUnavailableException {
        AtomicReference<String> receivedBody = new AtomicReference<>();
        fakeRegistry = startFakeRegistry(201, "status=registered", receivedBody);

        var client = new RegistryClient(uriOf(fakeRegistry));
        client.register("web-app", "1.4.2", "sha256:abc123");

        assertEquals("name=web-app&version=1.4.2&digest=sha256:abc123", receivedBody.get());
    }

    @Test
    void throwsWhenRegistryRejectsTheRequest() throws IOException {
        fakeRegistry =
                startFakeRegistry(400, "status=error&message=bad request", new AtomicReference<>());
        var client = new RegistryClient(uriOf(fakeRegistry));

        assertThrows(
                RegistryUnavailableException.class,
                () -> client.register("web-app", "1.4.2", "sha256:abc123"));
    }

    @Test
    void throwsWhenNothingIsListening() throws IOException {
        int unusedPort;
        try (ServerSocket probe = new ServerSocket(0)) {
            unusedPort = probe.getLocalPort();
        } // closed immediately: guaranteed nothing is listening on it below

        var client = new RegistryClient(URI.create("http://localhost:" + unusedPort));

        assertThrows(
                RegistryUnavailableException.class,
                () -> client.register("web-app", "1.4.2", "sha256:abc123"));
    }

    private static HttpServer startFakeRegistry(
            int status, String responseBody, AtomicReference<String> receivedBody) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(
                "/artifacts",
                exchange -> {
                    try (InputStream in = exchange.getRequestBody()) {
                        receivedBody.set(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                    }
                    byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(status, bytes.length);
                    try (OutputStream out = exchange.getResponseBody()) {
                        out.write(bytes);
                    }
                    exchange.close();
                });
        server.start();
        return server;
    }

    private static URI uriOf(HttpServer server) {
        return URI.create("http://localhost:" + server.getAddress().getPort());
    }
}