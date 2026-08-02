# Milestone 5 — Orchestrator HTTP API and the full end-to-end run

## Goal

Give the Orchestrator its own HTTP API and wire everything together: when a run
finishes, the Orchestrator saves its own run record *and* calls the Registry over real
HTTP to register the artifact that run produced. This is the milestone where the two
services you've built actually talk to each other as separate processes — the payoff
of everything since milestone 0.

## Step 1 — the request handler (write the core logic yourself)

`src/main/java/com/isaqb/practice/microservices/orchestrator/HttpResult.java`
(copy-paste — same shape as the Registry's, deliberately not shared, see milestone 0):

```java
package com.isaqb.practice.microservices.orchestrator;

/** The outcome of handling one request: an HTTP status code and a response body. */
public record HttpResult(int status, String body) {}
```

`src/main/java/com/isaqb/practice/microservices/orchestrator/OrchestratorRequestHandler.java`:

```java
package com.isaqb.practice.microservices.orchestrator;

/**
 * The Orchestrator's core request logic, kept free of any HttpExchange/HttpServer
 * detail, same as RegistryRequestHandler on the Registry side.
 */
public final class OrchestratorRequestHandler {

  private final PipelineRunStore store;
  private final RegistryClient registryClient;

  public OrchestratorRequestHandler(PipelineRunStore store, RegistryClient registryClient) {
    this.store = store;
    this.registryClient = registryClient;
  }

  /**
   * Handles "run `runId` finished". `body` is "name=...&version=...&digest=..."
   * describing the artifact that run produced (same wire format as the Registry's,
   * parsed independently here rather than importing ArtifactWireFormat - see
   * milestone 4's note on not sharing code between the two services).
   *
   * Must:
   *  - parse `body` into name/version/digest (split on '&', then each segment on the
   *    first '=' - the same idea as ArtifactWireFormat.decode, written again here)
   *  - if any of the three fields is missing or blank, return
   *    HttpResult(400, "status=error&message=<short reason>") - and do NOT call the
   *    registry in this case
   *  - otherwise: save a COMPLETED PipelineRun for `runId` (with those three fields)
   *    via `store`, then call registryClient.register(name, version, digest)
   *  - if the registry call succeeds: return
   *    HttpResult(200, "status=completed&runId=" + runId)
   *  - if registryClient.register throws RegistryUnavailableException: return
   *    HttpResult(502, "status=error&message=registry unavailable: " + e.getMessage())
   *    - note the run stays saved as COMPLETED in the Orchestrator's own store even
   *    though telling the Registry failed. That gap between "my own state says done"
   *    and "the other service actually knows" is the eventual-consistency trade-off
   *    from the README, not a bug to paper over.
   */
  public HttpResult handleComplete(String runId, String body) {
    // TODO: implement per the contract above.
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

## Step 2 — tests (copy-paste, must pass once step 1 is done)

Like milestone 4's test, this spins up a small fake registry `HttpServer` so the
handler is verified against a real HTTP round trip without depending on the real
Registry's code.

`src/test/java/com/isaqb/practice/microservices/orchestrator/OrchestratorRequestHandlerTest.java`:

```java
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
```

## Step 3 — wire it to a real `HttpServer` (copy-paste)

`src/main/java/com/isaqb/practice/microservices/orchestrator/OrchestratorMain.java`:

```java
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

  private static final String RUNS_PREFIX = "/runs/";
  private static final String COMPLETE_SUFFIX = "/complete";

  private OrchestratorMain() {}

  public static void main(String[] args) throws IOException {
    if (args.length < 2) {
      System.err.println(
          "usage: OrchestratorMain <port> <registry-base-uri>, e.g. 8080 http://localhost:8081");
      System.exit(2);
      return;
    }

    int port = Integer.parseInt(args[0]);
    URI registryBaseUri = URI.create(args[1]);

    var handler =
        new OrchestratorRequestHandler(
            new PipelineRunStore(), new RegistryClient(registryBaseUri));

    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
    server.createContext(RUNS_PREFIX, exchange -> dispatch(exchange, handler));
    server.start();

    System.out.println(
        "Pipeline Orchestrator listening on port " + port + ", registry at " + registryBaseUri);
  }

  private static void dispatch(HttpExchange exchange, OrchestratorRequestHandler handler)
      throws IOException {
    try {
      String method = exchange.getRequestMethod();
      String path = exchange.getRequestURI().getPath();

      HttpResult result;
      if ("POST".equalsIgnoreCase(method)
          && path.startsWith(RUNS_PREFIX)
          && path.endsWith(COMPLETE_SUFFIX)) {
        String runId = path.substring(RUNS_PREFIX.length(), path.length() - COMPLETE_SUFFIX.length());
        result = handler.handleComplete(runId, readBody(exchange));
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

## Step 4 — try the full two-service flow for real

Build once, then start both services in separate terminals:

```bash
mvn -f patterns/03-microservices/pom.xml clean compile

# terminal 1 - the Artifact Registry, port 8081
java -cp patterns/03-microservices/target/classes \
  com.isaqb.practice.microservices.registry.RegistryMain 8081

# terminal 2 - the Pipeline Orchestrator, port 8080, pointed at the Registry
java -cp patterns/03-microservices/target/classes \
  com.isaqb.practice.microservices.orchestrator.OrchestratorMain 8080 http://localhost:8081
```

In a third terminal, play the platform engineer: tell the Orchestrator a run finished.

```bash
curl -i -X POST http://localhost:8080/runs/run-1/complete \
  -d 'name=web-app&version=1.4.2&digest=sha256:5f3d4c1e'
```

You should get `200` with `status=completed&runId=run-1`. Now confirm the Registry —
a completely separate process that the Orchestrator never touched directly — actually
received it:

```bash
curl -i http://localhost:8081/artifacts/web-app
```

`200`, with the same name/version/digest you sent. That round trip — one process
telling another process about something over a real socket, no shared memory, no
shared database — *is* the Microservices pattern, not a diagram of it.

Now see the failure mode from the README's trade-offs table. Stop the Registry
(`Ctrl-C` in terminal 1) and repeat the `curl -X POST .../complete` call. You should
get `502`. Check the Orchestrator's own record of the run anyway — there's no HTTP
endpoint to query it in this exercise, but you proved in `OrchestratorRequestHandlerTest`
that it's still saved as `COMPLETED` locally. That mismatch (Orchestrator thinks it's
done; Registry never heard about it) is exactly the "eventual, not transactional
consistency" row from the README — something a single in-process monolith call could
never produce.

## Checkpoint

- [ ] `mvn -f patterns/03-microservices/pom.xml clean verify` passes, every package's
      tests green.
- [ ] The three-terminal flow above works: `POST .../complete` returns `200`, and the
      Registry has the artifact.
- [ ] Stopping the Registry and repeating the call returns `502`, and you can explain
      why that's a *new* failure mode this pattern introduces, not one Layers or a
      monolith would ever have to handle at this call site.

Next: [`06-build-and-release.md`](06-build-and-release.md).
