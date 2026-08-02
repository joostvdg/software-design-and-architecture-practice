# Milestone 3 — The RPC client stub

## Goal

Build `FleetManagerClient`: the Node Agent's side of the call. Its method,
`reportHeartbeat(NodeHealth)`, is the RPC **stub** — it reads exactly like a plain,
local method call, but underneath performs a real HTTP POST to the Fleet Manager.
This milestone is where section 1's "hides distribution" claim stops being an
abstract sentence and becomes a method you're staring at.

The agent package models `NodeHealth` and the wire encode independently from the
manager package — same convention as `03-microservices`' two independently-owned
services.

## Step 1 — package and agent-side data (copy-paste)

Create `src/main/java/com/isaqb/practice/rpc/agent/`.

`src/main/java/com/isaqb/practice/rpc/agent/NodeHealth.java`:

```java
package com.isaqb.practice.rpc.agent;

/**
 * The agent's own view of a node's health - a separate type from
 * manager.NodeHealth, deliberately. The two packages agree on wire field names
 * only, never on a shared Java type, the same way two independently-owned real
 * services would.
 */
public record NodeHealth(String nodeId, String status, long timestampMillis) {}
```

`src/main/java/com/isaqb/practice/rpc/agent/HeartbeatResult.java`:

```java
package com.isaqb.practice.rpc.agent;

/** The outcome of one reportHeartbeat call, as seen by the caller. */
public record HeartbeatResult(boolean acknowledged, int httpStatus) {}
```

`src/main/java/com/isaqb/practice/rpc/agent/ManagerUnavailableException.java`:

```java
package com.isaqb.practice.rpc.agent;

/** Thrown when the Fleet Manager can't be reached at all (connection failure). */
public class ManagerUnavailableException extends Exception {

  public ManagerUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
```

## Step 2 — the wire encode (write this yourself)

`src/main/java/com/isaqb/practice/rpc/agent/HeartbeatWireFormat.java`:

```java
package com.isaqb.practice.rpc.agent;

/** The agent side's encode of the heartbeat wire format (see manager.HeartbeatWireFormat). */
public final class HeartbeatWireFormat {

  private HeartbeatWireFormat() {}

  /**
   * Encodes a NodeHealth as "nodeId=<id>&status=<status>&timestampMillis=<millis>",
   * in that field order.
   */
  public static String encode(NodeHealth health) {
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

Test, copy-paste,
`src/test/java/com/isaqb/practice/rpc/agent/HeartbeatWireFormatTest.java`:

```java
package com.isaqb.practice.rpc.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HeartbeatWireFormatTest {

  @Test
  void encodesFieldsInFixedOrder() {
    var health = new NodeHealth("node-7", "HEALTHY", 1732550400000L);

    assertEquals(
        "nodeId=node-7&status=HEALTHY&timestampMillis=1732550400000",
        HeartbeatWireFormat.encode(health));
  }
}
```

## Step 3 — the RPC stub itself (write the call yourself)

`src/main/java/com/isaqb/practice/rpc/agent/FleetManagerClient.java`:

```java
package com.isaqb.practice.rpc.agent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * The RPC stub. reportHeartbeat reads like a plain local call - callers of this class
 * (NodeAgentMain, milestone 5) never see an HttpRequest or HttpClient. That's the
 * pattern: the network hop is real, but invisible at the call site.
 */
public class FleetManagerClient {

  private final HttpClient httpClient;
  private final URI managerBaseUri;

  public FleetManagerClient(URI managerBaseUri) {
    this(managerBaseUri, HttpClient.newHttpClient());
  }

  // Package-visible so milestone 4's tests can inject a client with a short timeout.
  FleetManagerClient(URI managerBaseUri, HttpClient httpClient) {
    this.managerBaseUri = managerBaseUri;
    this.httpClient = httpClient;
  }

  /**
   * Reports this node's health to the Fleet Manager. Looks like a local call; is
   * actually a blocking HTTP POST to {@code managerBaseUri.resolve("/heartbeat")}.
   *
   * @throws ManagerUnavailableException if the request can't be sent/completed at all
   *     (e.g. connection refused) - wrap the underlying IOException/InterruptedException.
   */
  public HeartbeatResult reportHeartbeat(NodeHealth health) throws ManagerUnavailableException {
    // TODO:
    // 1. Build an HttpRequest: POST to managerBaseUri.resolve("/heartbeat"), body
    //    HeartbeatWireFormat.encode(health), as a String body publisher.
    // 2. Send it via httpClient.send(request, HttpResponse.BodyHandlers.ofString()).
    // 3. On success, return new HeartbeatResult(response.statusCode() == 200, response.statusCode()).
    // 4. Catch java.io.IOException and InterruptedException; wrap either in
    //    ManagerUnavailableException with a short message and the original as cause.
    //    (If you catch InterruptedException, call Thread.currentThread().interrupt()
    //    before throwing, so the interrupt status isn't silently swallowed.)
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

## Step 4 — an end-to-end test against a real running manager (write this yourself)

Write `src/test/java/com/isaqb/practice/rpc/agent/FleetManagerClientTest.java` that:

- in a `@BeforeEach` (or per-test setup), starts a real
  `com.isaqb.practice.rpc.manager.FleetManagerMain`-style `HttpServer` on an
  ephemeral port (`new InetSocketAddress(0)`, then read back `server.getAddress().getPort()`)
  wired to a fresh `NodeHealthStore` and `FleetManagerRequestHandler` — you can copy
  the dispatch/readBody/writeResponse logic from `FleetManagerMain`, or better, factor
  a small shared test helper if you prefer (either is fine for this exercise).
- calls `new FleetManagerClient(URI.create("http://localhost:" + port))
  .reportHeartbeat(new NodeHealth("node-7", "HEALTHY", 1000L))` and asserts
  `acknowledged()` is `true` and `httpStatus()` is `200`.
- stops the test server after the test (`server.stop(0)`).

This is the milestone's real proof: a call that reads exactly like invoking a method
on a local object, verified end-to-end over an actual socket.

## Checkpoint

```bash
mvn -f patterns/12-rpc/pom.xml clean verify
```

`HeartbeatWireFormatTest` and your end-to-end `FleetManagerClientTest` both pass. You
can explain, in one sentence, what `reportHeartbeat`'s method signature does *not*
tell a caller about what happens when it's invoked.

Next: [`04-timeouts-and-retries.md`](04-timeouts-and-retries.md).
