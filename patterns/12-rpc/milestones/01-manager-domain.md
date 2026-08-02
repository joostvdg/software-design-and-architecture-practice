# Milestone 1 — Fleet Manager domain

## Goal

Build the Fleet Manager's own data model and store: `NodeHealth`, `HealthStatus`, and
the in-memory `NodeHealthStore` that keeps the latest reported health per node. None
of this touches HTTP yet — that's milestone 2.

Delete `src/test/java/com/isaqb/practice/rpc/SmokeTest.java` now — the tests you add
in this milestone replace it as your "is the build green" signal.

## Step 1 — package (copy-paste)

Create `src/main/java/com/isaqb/practice/rpc/manager/`.

## Step 2 — health status and node health (copy-paste)

`src/main/java/com/isaqb/practice/rpc/manager/HealthStatus.java`:

```java
package com.isaqb.practice.rpc.manager;

public enum HealthStatus {
  HEALTHY,
  DEGRADED,
  UNHEALTHY
}
```

`src/main/java/com/isaqb/practice/rpc/manager/NodeHealth.java`:

```java
package com.isaqb.practice.rpc.manager;

/** One node's most recently reported health, as recorded by the Fleet Manager. */
public record NodeHealth(String nodeId, HealthStatus status, long timestampMillis) {}
```

## Step 3 — `NodeHealthStore` (write the core logic yourself)

Create `src/main/java/com/isaqb/practice/rpc/manager/NodeHealthStore.java`:

```java
package com.isaqb.practice.rpc.manager;

import java.util.Optional;

/**
 * Keeps the most recently reported NodeHealth per nodeId. A later report for the
 * same nodeId replaces the earlier one - the Fleet Manager only cares about current
 * state, not history.
 */
public class NodeHealthStore {

  // TODO: back this with a Map<String, NodeHealth>.

  /** Records (or replaces) the health for {@code health.nodeId()}. */
  public void record(NodeHealth health) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  /** Looks up the latest known health for a node, if any has ever been reported. */
  public Optional<NodeHealth> findByNodeId(String nodeId) {
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

## Step 4 — tests (copy-paste, must pass once step 3 is done)

`src/test/java/com/isaqb/practice/rpc/manager/NodeHealthStoreTest.java`:

```java
package com.isaqb.practice.rpc.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NodeHealthStoreTest {

  private final NodeHealthStore store = new NodeHealthStore();

  @Test
  void recordsAndFindsByNodeId() {
    store.record(new NodeHealth("node-7", HealthStatus.HEALTHY, 1000L));

    var found = store.findByNodeId("node-7");

    assertTrue(found.isPresent());
    assertEquals(HealthStatus.HEALTHY, found.get().status());
  }

  @Test
  void laterReportReplacesEarlierOne() {
    store.record(new NodeHealth("node-7", HealthStatus.HEALTHY, 1000L));
    store.record(new NodeHealth("node-7", HealthStatus.DEGRADED, 2000L));

    assertEquals(HealthStatus.DEGRADED, store.findByNodeId("node-7").orElseThrow().status());
  }

  @Test
  void unknownNodeIsEmpty() {
    assertTrue(store.findByNodeId("nope").isEmpty());
  }
}
```

## Checkpoint

```bash
mvn -f patterns/12-rpc/pom.xml clean verify
```

All `NodeHealthStoreTest` cases pass.

Next: [`02-manager-http-api.md`](02-manager-http-api.md).
