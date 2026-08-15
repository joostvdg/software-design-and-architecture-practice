package com.isaqb.practice.microservices.registry;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Composition root and entry point for the Artifact Registry service. Starts its own
 * HttpServer on its own port with its own ArtifactStore - nothing else in this module
 * holds a reference to that store.
 */
public final class RegistryMain {
    private static final String ARTIFACTS_PATH = "/artifacts";
    private static final String ARTIFACTS_PREFIX = ARTIFACTS_PATH + "/";

    private RegistryMain() {}

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8081;

        var handler = new RegistryRequestHandler((new ArtifactStore()));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext(ARTIFACTS_PATH, exchange -> dispatch(exchange, handler));
        server.start();

        System.out.println("HTTP server started on port " + port);

    }

    private static void dispatch(HttpExchange exchange, RegistryRequestHandler handler) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            HttpResult result;
            if("POST".equals(method) && ARTIFACTS_PATH.equals(path)) {
                result = handler.handleRegister(readBody(exchange));
            } else if("GET".equals(method) && path.startsWith(ARTIFACTS_PREFIX)) {
                String name = path.substring(ARTIFACTS_PREFIX.length());
                result = handler.handleLookup(name);
            } else {
                result = new HttpResult(404, "status=error&message=unknown route");
            }
            writeResponse(exchange, result);
        } finally {
            exchange.close();
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void writeResponse(HttpExchange exchange, HttpResult result) throws IOException {
        byte[] bytes = result.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(result.status(), bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

}
