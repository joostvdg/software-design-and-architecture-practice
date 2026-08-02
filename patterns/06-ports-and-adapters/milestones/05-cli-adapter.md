# Milestone 5 — CLI driving adapter

## Goal

Write `Main`: the CLI driving adapter *and* the composition root. This is the only
class in the module allowed to name every concrete class at once — `ApprovalService`,
`DefaultApprovalPolicy`, and `InMemoryApprovalRepository` — because wiring concrete
implementations into ports is exactly what a composition root is for. Notice `Main`
lives in `adapter.driving.cli`, not in `core`: it is *a* driving adapter among possible
others (an HTTP adapter, a Slack-bot adapter), not a special layer above everything
else.

## Step 1 — wiring and CLI plumbing (copy-paste)

`src/main/java/com/isaqb/practice/portsandadapters/adapter/driving/cli/Main.java`:

```java
package com.isaqb.practice.portsandadapters.adapter.driving.cli;

import com.isaqb.practice.portsandadapters.adapter.driven.memory.InMemoryApprovalRepository;
import com.isaqb.practice.portsandadapters.core.ApprovalDecision;
import com.isaqb.practice.portsandadapters.core.ApprovalRequest;
import com.isaqb.practice.portsandadapters.core.ApprovalService;
import com.isaqb.practice.portsandadapters.core.DefaultApprovalPolicy;
import com.isaqb.practice.portsandadapters.core.port.RequestApprovalUseCase;

/**
 * Composition root and driving adapter in one: the only class allowed to know about
 * every concrete class in the module at once. Wires a driven adapter
 * (InMemoryApprovalRepository) and the core's own policy into the core's use case,
 * then drives that use case from the command line.
 */
public final class Main {

  private Main() {}

  public static void main(String[] args) {
    if (args.length != 5) {
      System.err.println(
          "usage: ports-and-adapters <requester> <approver> <namespace> <true|false> <justification>");
      System.exit(2);
      return;
    }

    RequestApprovalUseCase useCase =
        new ApprovalService(new DefaultApprovalPolicy(), new InMemoryApprovalRepository());

    var request =
        new ApprovalRequest(args[0], args[1], args[2], args[4], Boolean.parseBoolean(args[3]));

    ApprovalDecision decision = useCase.decide(request);
    System.out.println(formatDecision(decision));
    System.exit(decision.approved() ? 0 : 1);
  }

  // See step 2 below.
  static String formatDecision(ApprovalDecision decision) {
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

Notice the local variable is declared as `RequestApprovalUseCase` — the driving port
type — even though the object behind it is an `ApprovalService`. `Main` only ever
*calls* the core through the port's method, `decide(...)`; the fact that it also
happens to be the one place that *constructs* an `ApprovalService` is a composition-root
concern, not a driving-adapter concern. If you wanted a second driving adapter (an
HTTP handler, say), it would construct its own `ApprovalService` (or receive one) and
call the exact same `decide(...)` method — no change to `core` at all.

## Step 2 — output formatting (write this yourself)

Implement `formatDecision` above:

- If `decision.approved()`, return a line such as `"APPROVED: <namespace> for
  <requester>, approved by <approver> - <reason>"`.
- Otherwise, return a line such as `"DENIED: <namespace> for <requester> - <reason>"`.

Exact wording is up to you; the test below only checks that the right pieces of
information appear somewhere in the string.

## Step 3 — test (copy-paste, must pass once step 2 is done)

`src/test/java/com/isaqb/practice/portsandadapters/adapter/driving/cli/MainTest.java`:

```java
package com.isaqb.practice.portsandadapters.adapter.driving.cli;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.isaqb.practice.portsandadapters.core.ApprovalDecision;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MainTest {

  private static final Instant WHEN = Instant.parse("2026-08-01T09:00:00Z");

  @Test
  void formatsAnApprovedDecision() {
    var decision =
        new ApprovalDecision("alice", "bob", "payments-prod", true, "on-call approved", WHEN);

    String formatted = Main.formatDecision(decision);

    assertTrue(formatted.toUpperCase().contains("APPROVED"));
    assertTrue(formatted.contains("payments-prod"));
    assertTrue(formatted.contains("on-call approved"));
  }

  @Test
  void formatsADeniedDecision() {
    var decision =
        new ApprovalDecision("alice", "alice", "payments-prod", false, "self-approval", WHEN);

    String formatted = Main.formatDecision(decision);

    assertTrue(formatted.toUpperCase().contains("DENIED"));
    assertTrue(formatted.contains("self-approval"));
  }
}
```

## Step 4 — try it for real

```bash
mvn -f patterns/06-ports-and-adapters/pom.xml clean package
java -jar patterns/06-ports-and-adapters/target/ports-and-adapters-1.0.0-SNAPSHOT.jar \
  alice bob payments-prod true "on-call approved this ahead of the change window"
echo "exit code: $?"

java -jar patterns/06-ports-and-adapters/target/ports-and-adapters-1.0.0-SNAPSHOT.jar \
  alice alice payments-prod true "seems fine"
echo "exit code: $?"
```

The first run should print an `APPROVED` line and exit `0`. The second is
self-approval — it should print a `DENIED` line (policy overriding the requested
`true`) and exit `1`.

## Checkpoint

- [ ] `mvn -f patterns/06-ports-and-adapters/pom.xml clean verify` passes, every test
      green.
- [ ] Both CLI runs in step 4 behave as described.
- [ ] You can point to the one line in `Main` that would change if
      `InMemoryApprovalRepository` were swapped for a different driven adapter — and
      confirm nothing in `core` would need to change. Milestone 6 makes you prove this
      for real.

Next: [`06-second-driven-adapter.md`](06-second-driven-adapter.md).
