# Milestone 2 — Fleet Manager HTTP API

## Goal

Give the Fleet Manager a real HTTP API: the manager's own wire-format decode, the
request-handling logic (kept free of any `HttpExchange`/`HttpServer` detail so it's
trivially unit-testable), and `FleetManagerMain` wiring it to a real `HttpServer`. By
the end of this milestone the Fleet Manager is a real, independently runnable process
a `curl` command can talk to.

## The wire format

Same style as `03-microservices`: `key=value` pairs joined by `&`, one line:

```
nodeId=node-7&status=HEALTHY&timestampMillis=1732550400000
```

One endpoint:

| Method | Path | Request body | Success | Failure |
|---|---|---|---|---|
| `POST` | `/heartbeat` | `nodeId=...&status=...&timestampMillis=...` | `200`, body `status=ack` | `400` if any field is missing/blank or `status` isn't a valid `HealthStatus` |

## Step 1 — the wire format decode (write this yourself)

`src/main/java/com/isaqb/practice/rpc/manager/HeartbeatWireFormat.java`:

```java
package com.isaqb.practice.rpc.manager;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The manager side's decode of the heartbeat wire format. The agent package
 * (milestone 3) has its own, separate encode - the two packages agree on field
 * names only, never on shared code, exactly as two independently-owned real services
 * would.
 */
public final class HeartbeatWireFormat {

  private HeartbeatWireFormat() {}

  /**
   * Parses a "key=value&key=value" body into a Map. Segments without an '=' are
   * ignored. An empty or blank body decodes to an empty map.
   */
  public static Map<String, String> decode(String body) {
    // TODO: split `body` on '&', then split each non-empty segment on the *first*
    // '=' into a key and a value, collecting them into a Map. A LinkedHashMap is a
    // fine, simple choice.
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

Tests, copy-paste,
`src/test/java/com/isaqb/practice/rpc/manager/HeartbeatWireFormatTest.java`:

```java
package com.isaqb.practice.rpc.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class HeartbeatWireFormatTest {

  @Test
  void decodesKeyValuePairs() {
    Map<String, String> fields =
        HeartbeatWireFormat.decode("nodeId=node-7&status=HEALTHY&timestampMillis=1000");

    assertEquals("node-7", fields.get("nodeId"));
    assertEquals("HEALTHY", fields.get("status"));
    assertEquals("1000", fields.get("timestampMillis"));
  }

  @Test
  void decodesEmptyBodyToEmptyMap() {
    assertTrue(HeartbeatWireFormat.decode("").isEmpty());
  }
}
```

## Step 2 — the request handler (write the core logic yourself)

`src/main/java/com/isaqb/practice/rpc/manager/HttpResult.java` (copy-paste):

```java
package com.isaqb.practice.rpc.manager;

/** The outcome of handling one request: an HTTP status code and a response body. */
public record HttpResult(int status, String body) {}
```

`src/main/java/com/isaqb/practice/rpc/manager/FleetManagerRequestHandler.java`:

```java
package com.isaqb.practice.rpc.manager;

/**
 * The Fleet Manager's core request logic, deliberately free of any HttpExchange /
 * HttpServer detail - FleetManagerMain (next step) is the only class that touches
 * the JDK HTTP types directly.
 */
public final class FleetManagerRequestHandler {

  private final NodeHealthStore store;

  public FleetManagerRequestHandler(NodeHealthStore store) {
    this.store = store;
  }

  /**
   * Handles a POST /heartbeat request. `body` is the raw request body in the wire
   * format above. Must:
   *  - decode `body` with HeartbeatWireFormat.decode
   *  - if "nodeId", "status", or "timestampMillis" is missing/blank, or "status"
   *    isn't a valid HealthStatus name, or "timestampMillis" isn't a valid long,
   *    return HttpResult(400, "status=error&message=<short reason>")
   *  - otherwise, build a NodeHealth, record it via `store`, and return
   *    HttpResult(200, "status=ack")
   */
  public HttpResult handleHeartbeat(String body) {
    // TODO: implement per the contract above.
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

Tests, copy-paste,
`src/test/java/com/isaqb/practice/rpc/manager/FleetManagerRequestHandlerTest.java`:

```java
package com.isaqb.practice.rpc.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FleetManagerRequestHandlerTest {

  private final NodeHealthStore store = new NodeHealthStore();
  private final FleetManagerRequestHandler handler = new FleetManagerRequestHandler(store);

  @Test
  void recordsAValidHeartbeat() {
    var result = handler.handleHeartbeat("nodeId=node-7&status=HEALTHY&timestampMillis=1000");

    assertEquals(200, result.status());
    assertEquals(HealthStatus.HEALTHY, store.findByNodeId("node-7").orElseThrow().status());
  }

  @Test
  void rejectsHeartbeatMissingAField() {
    var result = handler.handleHeartbeat("nodeId=node-7&status=HEALTHY");

    assertEquals(400, result.status());
    assertTrue(store.findByNodeId("node-7").isEmpty());
  }

  @Test
  void rejectsInvalidStatusValue() {
    var result = handler.handleHeartbeat("nodeId=node-7&status=ON_FIRE&timestampMillis=1000");

    assertEquals(400, result.status());
  }
}
```

## Step 3 — wire it to a real `HttpServer` (copy-paste)

This is mechanical JDK HTTP-server ceremony, not pattern logic — given in full.

`src/main/java/com/isaqb/practice/rpc/manager/FleetManagerMain.java`:

```java
package com.isaqb.practice.rpc.manager;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Composition root and entry point for the Fleet Manager. Starts its own HttpServer
 * on its own port with its own NodeHealthStore.
 */
public final class FleetManagerMain {

  private static final String HEARTBEAT_PATH = "/heartbeat";

  private FleetManagerMain() {}

  public static void main(String[] args) throws IOException {
    int port = args.length > 0 ? Integer.parseInt(args[0]) : 8082;

    var handler = new FleetManagerRequestHandler(new NodeHealthStore());

    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
    server.createContext(HEARTBEAT_PATH, exchange -> dispatch(exchange, handler));
    server.start();

    System.out.println("Fleet Manager listening on port " + port);
  }

  private static void dispatch(HttpExchange exchange, FleetManagerRequestHandler handler)
      throws IOException {
    try {
      HttpResult result;
      if ("POST".equalsIgnoreCase(exchange.getRequestMethod())
          && HEARTBEAT_PATH.equals(exchange.getRequestURI().getPath())) {
        result = handler.handleHeartbeat(readBody(exchange));
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
mvn -f patterns/12-rpc/pom.xml clean compile
java -cp patterns/12-rpc/target/classes com.isaqb.practice.rpc.manager.FleetManagerMain 8082
```

In another terminal, while it's running:

```bash
curl -i -X POST http://localhost:8082/heartbeat \
  -d 'nodeId=node-7&status=HEALTHY&timestampMillis=1732550400000'

curl -i -X POST http://localhost:8082/heartbeat -d 'nodeId=node-7&status=ON_FIRE'
```

You should see `200 status=ack`, then `400`. Stop the server with `Ctrl-C` when done.

## Checkpoint

- [ ] `mvn -f patterns/12-rpc/pom.xml clean verify` passes, all `manager` package
      tests green.
- [ ] The two `curl` calls above behave as described.
- [ ] You can explain why `FleetManagerRequestHandler` never imports anything from
      `com.sun.net.httpserver`.

Next: [`03-rpc-client-stub.md`](03-rpc-client-stub.md).
