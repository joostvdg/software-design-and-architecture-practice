# Milestone 3 — Approval service

## Goal

Write `ApprovalService`: the core's implementation of the driving port
(`RequestApprovalUseCase`). It orchestrates `ApprovalPolicy` (a `core` type - no
boundary to cross) and `ApprovalRepository` (the driven *port* - an interface, not a
concrete adapter, because no concrete adapter exists in `core`'s world). No driven
adapter exists yet at all; you'll write `InMemoryApprovalRepository` next milestone.
That's the point of this milestone: the core's own use-case logic is finished and
fully tested *before* any adapter is written, because the core never needed one to be
correct - it only needed an interface.

## Step 1 — the class shell (copy-paste except the method body)

`src/main/java/com/isaqb/practice/portsandadapters/core/ApprovalService.java`:

```java
package com.isaqb.practice.portsandadapters.core;

import com.isaqb.practice.portsandadapters.core.port.ApprovalRepository;
import com.isaqb.practice.portsandadapters.core.port.RequestApprovalUseCase;
import java.time.Clock;
import java.time.Instant;

/**
 * The core's implementation of the driving port. Reachable from outside only through
 * RequestApprovalUseCase, and reaches outside only through ApprovalRepository - never
 * naming a concrete adapter class. This is where "ports and adapters" earns its name:
 * everything this class touches is either another core type or an interface core
 * itself declared.
 */
public class ApprovalService implements RequestApprovalUseCase {

  private final ApprovalPolicy policy;
  private final ApprovalRepository repository;
  private final Clock clock;

  public ApprovalService(ApprovalPolicy policy, ApprovalRepository repository) {
    this(policy, repository, Clock.systemUTC());
  }

  // Package-private overload so tests can inject a fixed Clock and assert on
  // decidedAt deterministically, without needing a driven adapter or real time.
  ApprovalService(ApprovalPolicy policy, ApprovalRepository repository, Clock clock) {
    this.policy = policy;
    this.repository = repository;
    this.clock = clock;
  }

  @Override
  public ApprovalDecision decide(ApprovalRequest request) {
    // TODO:
    //  1. Ask `policy.violations(request)`.
    //  2. If the list is non-empty, build an ApprovalDecision with approved=false and
    //     a `reason` that joins every violation into one string, e.g. with
    //     String.join("; ", violations) - the request is denied regardless of what
    //     the approver intended.
    //  3. If the list is empty, build an ApprovalDecision that reflects the request's
    //     own approve() flag as `approved`, and justification() as `reason`.
    //  4. Either way, stamp decidedAt with Instant.now(clock).
    //  5. Save the decision via repository.save(...) before returning it - the use
    //     case's job includes persisting its own outcome, not just computing it.
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

## Step 2 — tests (copy-paste, must pass once step 1's TODO is done)

`src/test/java/com/isaqb/practice/portsandadapters/core/ApprovalServiceTest.java`:

```java
package com.isaqb.practice.portsandadapters.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.isaqb.practice.portsandadapters.core.port.ApprovalRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApprovalServiceTest {

  private static final Instant FIXED_NOW = Instant.parse("2026-08-01T09:00:00Z");
  private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

  // A fake driven adapter, written right here in the test - this is the payoff of
  // depending on a port instead of a concrete class: no InMemoryApprovalRepository
  // needs to exist yet for this test to run.
  private static final class RecordingRepository implements ApprovalRepository {
    final List<ApprovalDecision> saved = new ArrayList<>();

    @Override
    public void save(ApprovalDecision decision) {
      saved.add(decision);
    }

    @Override
    public List<ApprovalDecision> findByRequester(String requester) {
      throw new UnsupportedOperationException("not needed for this test");
    }
  }

  @Test
  void approvesAWellFormedRequestAndPersistsIt() {
    var repository = new RecordingRepository();
    var service = new ApprovalService(new DefaultApprovalPolicy(), repository, FIXED_CLOCK);
    var request =
        new ApprovalRequest("alice", "bob", "payments-prod", "on-call approved", true);

    ApprovalDecision decision = service.decide(request);

    assertTrue(decision.approved());
    assertEquals("on-call approved", decision.reason());
    assertEquals(FIXED_NOW, decision.decidedAt());
    assertEquals(1, repository.saved.size());
    assertEquals(decision, repository.saved.get(0));
  }

  @Test
  void denyingApproverStillProducesADenial() {
    var repository = new RecordingRepository();
    var service = new ApprovalService(new DefaultApprovalPolicy(), repository, FIXED_CLOCK);
    var request =
        new ApprovalRequest("alice", "bob", "payments-prod", "not ready yet", false);

    ApprovalDecision decision = service.decide(request);

    assertFalse(decision.approved());
    assertEquals("not ready yet", decision.reason());
  }

  @Test
  void policyViolationOverridesApproverIntent() {
    var repository = new RecordingRepository();
    var service = new ApprovalService(new DefaultApprovalPolicy(), repository, FIXED_CLOCK);
    // approve=true, but this is self-approval - policy must win.
    var request = new ApprovalRequest("alice", "alice", "payments-prod", "seems fine", true);

    ApprovalDecision decision = service.decide(request);

    assertFalse(decision.approved());
    assertEquals(1, repository.saved.size());
  }
}
```

## Checkpoint

```bash
mvn -f patterns/06-ports-and-adapters/pom.xml clean verify
```

All three `ApprovalServiceTest` cases pass. Take a moment to check: does
`ApprovalServiceTest` import anything from a package named `adapter`? It shouldn't -
`RecordingRepository` is a fake written inline, which is exactly what a driven port
buys you: full core test coverage before any real adapter exists.

Next: [`04-in-memory-adapter.md`](04-in-memory-adapter.md).
