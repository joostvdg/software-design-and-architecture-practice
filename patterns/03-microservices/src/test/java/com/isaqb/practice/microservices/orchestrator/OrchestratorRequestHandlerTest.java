package com.isaqb.practice.microservices.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OrchestratorRequestHandlerTest {

    private final PipelineRunStore store = new PipelineRunStore();
    private HttpServer fakeRegistry;

    @AfterEach
    void stopFakeRegistry() {
        if (fakeRegistry != null) {
            fakeRegistry.stop(0);
        }
    }

    @Test
    void completesARunAndRegistersItsArtifact() throws IOException {
        fakeRegistry = startFakeRegistry(201, "status=registered");
        var handler = new OrchestratorRequestHandler(store, new RegistryClient(uriOf(fakeRegistry)));

        var result =
                handler.handleComplete("run-1", "name=web-app&version=1.4.2&digest=sha256:abc123");

        assertEquals(200, result.status());
        assertEquals(RunStatus.COMPLETED, store.findById("run-1").orElseThrow().status());
        assertEquals("web-app", store.findById("run-1").orElseThrow().artifactName());
    }

    @Test
    void rejectsAnIncompleteBodyWithoutCallingTheRegistry() throws IOException {
        int unusedPort;
        try (ServerSocket probe = new ServerSocket(0)) {
            unusedPort = probe.getLocalPort();
        }
        var handler =
                new OrchestratorRequestHandler(
                        store, new RegistryClient(URI.create("http://localhost:" + unusedPort)));

        var result = handler.handleComplete("run-1", "name=web-app&version=1.4.2");

        assertEquals(400, result.status());
        assertTrue(store.findById("run-1").isEmpty());
    }

    @Test
    void reportsA502WhenTheRegistryRejectsTheCall() throws IOException {
        fakeRegistry = startFakeRegistry(500, "status=error&message=boom");
        var handler = new OrchestratorRequestHandler(store, new RegistryClient(uriOf(fakeRegistry)));

        var result =
                handler.handleComplete("run-1", "name=web-app&version=1.4.2&digest=sha256:abc123");

        assertEquals(502, result.status());
        assertEquals(RunStatus.COMPLETED, store.findById("run-1").orElseThrow().status());
    }

    private static HttpServer startFakeRegistry(int status, String responseBody) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(
                "/artifacts",
                exchange -> {
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