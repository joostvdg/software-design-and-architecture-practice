package com.isaqb.practice.microservices.orchestrator;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Composition root and entry point for the Pipeline Orchestrator service. Starts its
 * own HttpServer on its own port with its own PipelineRunStore, and a RegistryClient
 * pointed at wherever the Artifact Registry happens to be running - a separate
 * process, possibly a separate machine, reached only over HTTP.
 */
public final class OrchestratorMain {

    private static final String RUNS_PREFIX = "/runs";
    private static final String COMPLETE_SUFFIX = "/complete";

    private OrchestratorMain() {    }

    public static void main(final String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("usage: OrchestratorMain <port> <registry-base-uri>, e.g. 8080 http://localhost:8081");
            System.exit(2);
            return;
        }

        int port = Integer.parseInt(args[0]);
        URI registryBaseUri = URI.create(args[1]);

        var handler = new OrchestratorRequestHandler(new PipelineRunStore(), new RegistryClient(registryBaseUri));

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext(RUNS_PREFIX, exchange -> dispatch(exchange, handler));
        server.start();

        System.out.println("Pipeline Orchestrator listening on port " + port + ", registry at " + registryBaseUri);

    }

    private static void dispatch(HttpExchange httpExchange, OrchestratorRequestHandler handler) throws IOException {
        try {
            String method = httpExchange.getRequestMethod();
            String path = httpExchange.getRequestURI().getPath();

            HttpResult result;
            if ("POST".equalsIgnoreCase(method)
                && path.startsWith(RUNS_PREFIX)
                && path.endsWith(COMPLETE_SUFFIX)
            ) {
                String runId = path.substring(RUNS_PREFIX.length(), path.length() - COMPLETE_SUFFIX.length());
                result = handler.handleComplete(runId, readBody(httpExchange));
            } else {
                result = new HttpResult(404, "status=error&message=unknown route");
            }
            writeResponse(httpExchange, result);
        } finally {
            httpExchange.close();
        }
    }

    private static String readBody(final HttpExchange httpExchange) throws IOException {
        try (InputStream inputStream = httpExchange.getRequestBody()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void writeResponse(HttpExchange exchange, HttpResult result) throws IOException {
        byte[] bytes = result.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(result.status(), bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
