# Milestone 5 — Node agent loop, end to end

## Goal

Build `NodeAgentMain`: a small loop that periodically reports this node's health via
`RetryingHeartbeatReporter`, and run the whole case study end to end — two real
processes, one real network hop between them, exactly as `03-microservices` proves
its two services talking over real HTTP.

## Step 1 — `NodeAgentMain` (copy-paste, then run it)

`src/main/java/com/isaqb/practice/rpc/agent/NodeAgentMain.java`:

```java
package com.isaqb.practice.rpc.agent;

import java.net.URI;
import java.time.Duration;

/** Entry point for a Node Agent: reports health to the Fleet Manager every few seconds. */
public final class NodeAgentMain {

  public static void main(String[] args) throws InterruptedException {
    String nodeId = args.length > 0 ? args[0] : "node-7";
    URI managerUri = args.length > 1 ? URI.create(args[1]) : URI.create("http://localhost:8082");

    var client = new FleetManagerClient(managerUri, Duration.ofSeconds(3));
    var reporter = new RetryingHeartbeatReporter(client, 3, Duration.ofMillis(500));

    System.out.println("Node Agent " + nodeId + " reporting to " + managerUri);

    while (true) {
      var health = new NodeHealth(nodeId, "HEALTHY", System.currentTimeMillis());
      try {
        var result = reporter.reportWithRetry(health);
        System.out.println("heartbeat sent, acknowledged=" + result.acknowledged());
      } catch (ManagerUnavailableException e) {
        System.out.println("heartbeat failed after retries: " + e.getMessage());
      }
      Thread.sleep(Duration.ofSeconds(5).toMillis());
    }
  }

  private NodeAgentMain() {}
}
```

## Step 2 — run both processes for real

Terminal 1 — the Fleet Manager:

```bash
mvn -f patterns/12-rpc/pom.xml clean compile
java -cp patterns/12-rpc/target/classes com.isaqb.practice.rpc.manager.FleetManagerMain 8082
```

Terminal 2 — the Node Agent:

```bash
java -cp patterns/12-rpc/target/classes com.isaqb.practice.rpc.agent.NodeAgentMain node-7 http://localhost:8082
```

You should see the agent print `heartbeat sent, acknowledged=true` every 5 seconds.
Confirm it with a direct check from a third terminal:

```bash
curl -i -X POST http://localhost:8082/heartbeat -d 'nodeId=node-check&status=HEALTHY&timestampMillis=1'
```

Now stop the Fleet Manager (`Ctrl-C` in terminal 1) while the Node Agent keeps
running. Watch terminal 2: after up to `3 × 500ms` of retrying, it should print
`heartbeat failed after retries: ...` instead of hanging — the direct, observable
payoff of milestone 4's work. Restart the Fleet Manager and confirm the agent's next
heartbeat succeeds again, with no code change or restart needed on the agent side.

## Checkpoint

- [ ] `mvn -f patterns/12-rpc/pom.xml clean verify` is green.
- [ ] Both processes run simultaneously and the agent's heartbeats are acknowledged.
- [ ] Stopping the Fleet Manager mid-run produces a bounded "failed after retries"
      message on the agent side within a few seconds — not a hang.
- [ ] Restarting the Fleet Manager causes the very next heartbeat to succeed again.

Next: [`06-build-and-release.md`](06-build-and-release.md).
