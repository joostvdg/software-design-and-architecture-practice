# Milestone 2 — Registry HTTP API

## Goal

Give the Registry a real HTTP API: a hand-rolled wire format (no JSON library is
allowed in this repo — see `../../PATTERN-TEMPLATE.md`), the request-handling logic
that turns a raw HTTP request into a decision (register / look up / reject), and a
`RegistryMain` that actually starts an `HttpServer` on its own port. By the end of this
milestone the Registry is a real, independently runnable process a `curl` command can
talk to — the whole point of "independently deployable."

## The wire format

No JSON, no library — just `key=value` pairs joined by `&`, one line, e.g.:

```
name=web-app&version=1.4.2&digest=sha256:5f3d...
```

Two endpoints, both under `/artifacts`:

| Method | Path | Request body | Success | Failure |
|---|---|---|---|---|
| `POST` | `/artifacts` | `name=...&version=...&digest=...` | `201`, body contains the registered fields | `400` if any field is missing/blank |
| `GET` | `/artifacts/{name}` | *(none)* | `200`, body is the artifact's fields | `404` if unknown |

## Step 1 — the wire format codec (write this yourself)

`src/main/java/com/isaqb/practice/microservices/registry/ArtifactWireFormat.java`:

```java
package com.isaqb.practice.microservices.registry;

import java.util.LinkedHashMap;
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
    throw new UnsupportedOperationException("not implemented yet");
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
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

Tests, copy-paste,
`src/test/java/com/isaqb/practice/microservices/registry/ArtifactWireFormatTest.java`:

```java
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
```

## Step 2 — the request handler (write the core logic yourself)

`src/main/java/com/isaqb/practice/microservices/registry/HttpResult.java` (copy-paste):

```java
package com.isaqb.practice.microservices.registry;

/** The outcome of handling one request: an HTTP status code and a response body. */
public record HttpResult(int status, String body) {}
```

`src/main/java/com/isaqb/practice/microservices/registry/RegistryRequestHandler.java`:

```java
package com.isaqb.practice.microservices.registry;

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
    // TODO: implement per the contract above.
    throw new UnsupportedOperationException("not implemented yet");
  }

  /**
   * Handles a GET /artifacts/{name} request.
   *  - look `name` up via store.findByName
   *  - if present: HttpResult(200, ArtifactWireFormat.encodeArtifact(artifact))
   *  - if absent: HttpResult(404, "status=error&message=artifact not found: " + name)
   */
  public HttpResult handleLookup(String name) {
    // TODO: implement per the contract above.
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

Tests, copy-paste,
`src/test/java/com/isaqb/practice/microservices/registry/RegistryRequestHandlerTest.java`:

```java
package com.isaqb.practice.microservices.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RegistryRequestHandlerTest {

  private final ArtifactStore store = new ArtifactStore();
  private final RegistryRequestHandler handler = new RegistryRequestHandler(store);

  @Test
  void registersAValidArtifact() {
    var result = handler.handleRegister("name=web-app&version=1.4.2&digest=sha256:abc123");

    assertEquals(201, result.status());
    assertTrue(result.body().contains("name=web-app"));
    assertTrue(result.body().contains("version=1.4.2"));
    assertEquals("sha256:abc123", store.findByName("web-app").orElseThrow().digest());
  }

  @Test
  void rejectsRegistrationMissingAField() {
    var result = handler.handleRegister("name=web-app&version=1.4.2");

    assertEquals(400, result.status());
    assertTrue(store.findByName("web-app").isEmpty());
  }

  @Test
  void looksUpARegisteredArtifact() {
    store.register(new Artifact("web-app", "1.4.2", "sha256:abc123"));

    var result = handler.handleLookup("web-app");

    assertEquals(200, result.status());
    assertTrue(result.body().contains("sha256:abc123"));
  }

  @Test
  void lookupOfUnknownNameIs404() {
    var result = handler.handleLookup("does-not-exist");

    assertEquals(404, result.status());
  }
}
```

## Step 3 — wire it to a real `HttpServer` (copy-paste)

This is mechanical JDK HTTP-server ceremony, not pattern logic — given in full.

`src/main/java/com/isaqb/practice/microservices/registry/RegistryMain.java`:

```java
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

    var handler = new RegistryRequestHandler(new ArtifactStore());

    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
    server.createContext(ARTIFACTS_PATH, exchange -> dispatch(exchange, handler));
    server.start();

    System.out.println("Artifact Registry listening on port " + port);
  }

  private static void dispatch(HttpExchange exchange, RegistryRequestHandler handler)
      throws IOException {
    try {
      String method = exchange.getRequestMethod();
      String path = exchange.getRequestURI().getPath();

      HttpResult result;
      if ("POST".equalsIgnoreCase(method) && ARTIFACTS_PATH.equals(path)) {
        result = handler.handleRegister(readBody(exchange));
      } else if ("GET".equalsIgnoreCase(method) && path.startsWith(ARTIFACTS_PREFIX)) {
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
    try (OutputStream out = exchange.getResponseBody()) {
      out.write(bytes);
    }
  }
}
```

## Step 4 — try it for real

```bash
mvn -f patterns/03-microservices/pom.xml clean compile
java -cp patterns/03-microservices/target/classes \
  com.isaqb.practice.microservices.registry.RegistryMain 8081
```

In another terminal, while it's running:

```bash
curl -i -X POST http://localhost:8081/artifacts \
  -d 'name=web-app&version=1.4.2&digest=sha256:5f3d4c1e'

curl -i http://localhost:8081/artifacts/web-app

curl -i http://localhost:8081/artifacts/does-not-exist
```

You should see `201`, then `200` with the fields you registered, then `404`. Stop the
server with `Ctrl-C` when done.

## Checkpoint

- [ ] `mvn -f patterns/03-microservices/pom.xml clean verify` passes, all
      `registry` package tests green.
- [ ] The three `curl` calls above behave as described.
- [ ] You can explain why `RegistryRequestHandler` never imports anything from
      `com.sun.net.httpserver` — what does keeping it plain-Java buy you?

Next: [`03-orchestrator-domain.md`](03-orchestrator-domain.md).
