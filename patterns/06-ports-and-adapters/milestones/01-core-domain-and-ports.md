# Milestone 1 — Core domain model and ports

## Goal

Create the domain model (`ApprovalRequest`, `ApprovalDecision`) and the two ports that
define the core's entire outward-facing contract: `RequestApprovalUseCase` (driving)
and `ApprovalRepository` (driven). Everything in this milestone is given — these are
the shapes everything else in the module plugs into, so getting them exactly right
matters more than writing them yourself. Milestone 2 onward is where you start writing
real logic against these shapes.

Delete `src/test/java/com/isaqb/practice/portsandadapters/SmokeTest.java` now — from
this point on, `core` has its own code to build the "is the build green" signal on top
of, even before it has its own tests.

## Step 1 — the domain model (copy-paste)

`src/main/java/com/isaqb/practice/portsandadapters/core/ApprovalRequest.java`:

```java
package com.isaqb.practice.portsandadapters.core;

import java.util.Objects;

/**
 * What's being asked: an approver's response to a requester's ask, before policy has
 * been applied. `approve` is the approver's stated intent - the core may still turn
 * this into a denial if the request violates policy (see ApprovalPolicy).
 */
public record ApprovalRequest(
    String requester, String approver, String namespace, String justification, boolean approve) {

  public ApprovalRequest {
    Objects.requireNonNull(requester, "requester");
    Objects.requireNonNull(approver, "approver");
    Objects.requireNonNull(namespace, "namespace");
    Objects.requireNonNull(justification, "justification");
  }
}
```

`src/main/java/com/isaqb/practice/portsandadapters/core/ApprovalDecision.java`:

```java
package com.isaqb.practice.portsandadapters.core;

import java.time.Instant;
import java.util.Objects;

/**
 * The outcome the core produced for one ApprovalRequest: whether the deployment is
 * actually approved, and why. `reason` is either the approver's own justification (if
 * the request was policy-compliant) or an explanation of which policy rule failed (if
 * it wasn't) - see ApprovalService in milestone 3.
 */
public record ApprovalDecision(
    String requester,
    String approver,
    String namespace,
    boolean approved,
    String reason,
    Instant decidedAt) {

  public ApprovalDecision {
    Objects.requireNonNull(requester, "requester");
    Objects.requireNonNull(approver, "approver");
    Objects.requireNonNull(namespace, "namespace");
    Objects.requireNonNull(reason, "reason");
    Objects.requireNonNull(decidedAt, "decidedAt");
  }
}
```

## Step 2 — the driving port (copy-paste)

`src/main/java/com/isaqb/practice/portsandadapters/core/port/RequestApprovalUseCase.java`:

```java
package com.isaqb.practice.portsandadapters.core.port;

import com.isaqb.practice.portsandadapters.core.ApprovalDecision;
import com.isaqb.practice.portsandadapters.core.ApprovalRequest;

/**
 * The driving (primary) port: the one entry point the core exposes to whatever calls
 * it - a CLI today, conceivably an HTTP handler or a chat-bot tomorrow. The core
 * defines this interface; driving adapters depend on it, never the other way around.
 * Any number of driving adapters can call the same port without the core knowing how
 * many there are or what they look like.
 */
public interface RequestApprovalUseCase {

  ApprovalDecision decide(ApprovalRequest request);
}
```

## Step 3 — the driven port (copy-paste)

`src/main/java/com/isaqb/practice/portsandadapters/core/port/ApprovalRepository.java`:

```java
package com.isaqb.practice.portsandadapters.core.port;

import com.isaqb.practice.portsandadapters.core.ApprovalDecision;
import java.util.List;

/**
 * The driven (secondary) port: something the core needs done but does not know how to
 * do itself. The core defines this interface too - driven adapters (in-memory, a
 * file, a real database) implement it. Notice the dependency arrow points the same
 * way as the driving port: adapters depend on core's interface, core never depends on
 * an adapter, regardless of which side of the hexagon we're talking about.
 */
public interface ApprovalRepository {

  void save(ApprovalDecision decision);

  List<ApprovalDecision> findByRequester(String requester);
}
```

## Checkpoint

```bash
mvn -f patterns/06-ports-and-adapters/pom.xml clean verify
```

Build is green (there's nothing to test yet - that starts next milestone). Confirm by
inspection: nothing under `core/` imports anything from a package named `adapter` (it
doesn't exist yet, and after milestone 4 it must still not be imported from `core/`).

Next: [`02-approval-policy.md`](02-approval-policy.md).
