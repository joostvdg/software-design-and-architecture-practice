# Milestone 4 — Calling the Registry over real HTTP

## Goal

Write `RegistryClient`: the Orchestrator's side of a real HTTP call to the Artifact
Registry, using `java.net.http.HttpClient`. This is the milestone where "independently
deployable services communicate over the network" stops being a diagram and becomes
code that can fail in ways a local method call never can — a connection refused, a
non-2xx status, a timeout. Handling that is the actual exercise here, not the HTTP
plumbing itself.

## Step 1 — the failure type (copy-paste)

`src/main/java/com/isaqb/practice/microservices/orchestrator/RegistryUnavailableException.java`:

```java
package com.isaqb.practice.microservices.orchestrator;

/** Thrown when the Artifact Registry can't be reached, or rejects a request. */
public class RegistryUnavailableException extends Exception {

  public RegistryUnavailableException(String message) {
    super(message);
  }

  public RegistryUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
```

## Step 2 — the client (write the call yourself)

`src/main/java/com/isaqb/practice/microservices/orchestrator/RegistryClient.java`:

```java
package com.isaqb.practice.microservices.orchestrator;

import java.net.URI;
import java.net.http.HttpClient;

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

  public RegistryClient(URI registryBaseUri) {
    this(registryBaseUri, HttpClient.newHttpClient());
  }

  // Package-visible so tests can inject a client with, e.g., a short timeout.
  RegistryClient(URI registryBaseUri, HttpClient httpClient) {
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
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

If you want a starting shape for the request itself:
`HttpRequest.newBuilder(registryBaseUri.resolve("/artifacts")).POST(HttpRequest.BodyPublishers.ofString(body)).build()`,
sent with `httpClient.send(request, HttpResponse.BodyHandlers.ofString())`.

## Step 3 — tests (copy-paste, must pass once step 2 is done)

This test starts a small **fake** registry — a real `HttpServer`, just not the one
you'll build in milestone 5 — so `RegistryClient` is verified against a real socket, a
real request, and a real response, the same as it will be against the real Registry
later. Setting up that fake server is mechanical ceremony (given in full); the point of
the test is what it proves about the code you just wrote.

`src/test/java/com/isaqb/practice/microservices/orchestrator/RegistryClientTest.java`:

```java
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
```

## Checkpoint

```bash
mvn -f patterns/03-microservices/pom.xml clean verify
```

All three `RegistryClientTest` cases pass. The third test in particular
(`throwsWhenNothingIsListening`) is worth sitting with: in a single-process monolith,
there's no equivalent scenario — a method either exists and runs, or fails to compile.
Here, "the thing I'm calling isn't there right now" is a **runtime** condition your code
has to actively handle. That's the operational-overhead row from the README's
trade-offs table, made concrete.

Next: [`05-orchestrator-http-api.md`](05-orchestrator-http-api.md).
