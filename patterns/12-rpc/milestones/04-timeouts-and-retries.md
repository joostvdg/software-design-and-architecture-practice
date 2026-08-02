# Milestone 4 — Timeouts and retries (the pattern-defining milestone)

## Goal

`FleetManagerClient.reportHeartbeat` currently has no explicit timeout. Without one,
a slow or hung Fleet Manager doesn't make the call fail — it makes the call **block**,
for however long the underlying OS-level TCP behavior allows (which can be minutes,
not seconds). That is RPC's hidden-distribution danger from section 1 of the README,
made concrete: nothing in `reportHeartbeat`'s signature warns a caller this can
happen. This milestone adds an explicit timeout, then a small retry-with-backoff
wrapper on top, and proves both with a real slow server — not a mock.

## Step 1 — add an explicit timeout (write this yourself)

Modify `FleetManagerClient`'s constructor to accept a request timeout, and apply it
when building the request:

```java
public FleetManagerClient(URI managerBaseUri, Duration requestTimeout) {
  this(managerBaseUri, requestTimeout, HttpClient.newHttpClient());
}

FleetManagerClient(URI managerBaseUri, Duration requestTimeout, HttpClient httpClient) {
  this.managerBaseUri = managerBaseUri;
  this.requestTimeout = requestTimeout;
  this.httpClient = httpClient;
}
```

Keep the single-argument constructor from milestone 3 as an overload that delegates
with a sane default, e.g. `Duration.ofSeconds(3)`.

In `reportHeartbeat`'s request-building step, call `.timeout(requestTimeout)` on the
`HttpRequest.Builder`. `HttpClient.send` throws `java.net.http.HttpTimeoutException`
(a subtype of `IOException`) when the timeout is exceeded — since your `catch
(IOException e)` from milestone 3 already wraps it into `ManagerUnavailableException`,
no new catch clause should be needed; only the request-building line changes.

## Step 2 — prove it with a real slow server (write this test yourself)

Write `src/test/java/com/isaqb/practice/rpc/agent/FleetManagerClientTimeoutTest.java`
that:

- opens a raw `java.net.ServerSocket` on an ephemeral port (`new ServerSocket(0)`) —
  **not** a `FleetManagerMain`/`HttpServer` — that accepts one connection and then
  simply never writes a response (e.g. `serverSocket.accept()` in a background
  thread, then just `Thread.sleep(...)` past your client's timeout before closing).
  This simulates a Fleet Manager that's alive at the TCP level but hung — a real,
  observed failure mode, not a contrived one.
- constructs `new FleetManagerClient(URI.create("http://localhost:" + port),
  Duration.ofMillis(300))` — a short timeout, so the test doesn't run for minutes.
- calls `reportHeartbeat(...)` and asserts it throws `ManagerUnavailableException`
  **and** that the call returned in well under a second (assert on elapsed wall-clock
  time, e.g. `System.nanoTime()` before/after, asserting the elapsed duration is less
  than, say, 2 seconds). That second assertion is the actual point: it's the
  difference between "the call failed fast" and "the call would have hung."
- closes the `ServerSocket` when the test ends.

## Step 3 — retry with backoff (write this yourself)

Create `src/main/java/com/isaqb/practice/rpc/agent/RetryingHeartbeatReporter.java`:

```java
package com.isaqb.practice.rpc.agent;

import java.time.Duration;

/**
 * Wraps a FleetManagerClient with a bounded retry policy. Exists as a separate class,
 * not folded into FleetManagerClient itself, so "how many times to retry and how long
 * to wait between attempts" is a decision made by the caller (NodeAgentMain,
 * milestone 5) - not hidden inside the RPC stub.
 */
public class RetryingHeartbeatReporter {

  private final FleetManagerClient client;
  private final int maxAttempts;
  private final Duration delayBetweenAttempts;

  public RetryingHeartbeatReporter(FleetManagerClient client, int maxAttempts, Duration delayBetweenAttempts) {
    this.client = client;
    this.maxAttempts = maxAttempts;
    this.delayBetweenAttempts = delayBetweenAttempts;
  }

  /**
   * Calls client.reportHeartbeat, retrying up to maxAttempts total attempts (so
   * maxAttempts - 1 retries) whenever it throws ManagerUnavailableException, sleeping
   * delayBetweenAttempts between attempts. If every attempt fails, rethrows the last
   * ManagerUnavailableException. Does not retry on a successful call, even if
   * acknowledged() is false (a 400 response is a real rejection, not a transient
   * failure - retrying it would just get the same rejection).
   */
  public HeartbeatResult reportWithRetry(NodeHealth health) throws ManagerUnavailableException {
    // TODO: loop up to maxAttempts times, calling client.reportHeartbeat(health).
    // On success, return immediately. On ManagerUnavailableException, if this wasn't
    // the last attempt, Thread.sleep(delayBetweenAttempts.toMillis()) and continue;
    // if it was the last attempt, rethrow it.
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

Test, write this yourself,
`src/test/java/com/isaqb/practice/rpc/agent/RetryingHeartbeatReporterTest.java`:
using the same slow-`ServerSocket` technique as step 2 (or a `FleetManagerClient`
pointed at a port nothing is listening on, for an immediate connection-refused
failure instead of a timeout — either failure mode should trigger a retry), assert
that with `maxAttempts = 3`, `reportWithRetry` still throws
`ManagerUnavailableException` after exhausting all attempts, and that it does *not*
throw before at least `maxAttempts` worth of time has elapsed (proving it actually
retried, not failed immediately).

## Checkpoint

- [ ] `mvn -f patterns/12-rpc/pom.xml clean verify` is green, including both new test
      classes.
- [ ] `FleetManagerClientTimeoutTest` proves the call fails fast against a hung
      server instead of blocking indefinitely.
- [ ] You can explain, in one or two sentences, why `RetryingHeartbeatReporter` does
      *not* retry a request that got a `400` response from the Fleet Manager.

Next: [`05-node-agent-loop.md`](05-node-agent-loop.md).
