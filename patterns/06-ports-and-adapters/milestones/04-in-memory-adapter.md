# Milestone 4 — In-memory driven adapter

## Goal

Implement `InMemoryApprovalRepository`, the first real implementation of the
`ApprovalRepository` driven port. You'll also set up a **repository contract test**: a
single, reusable test suite that any `ApprovalRepository` implementation must pass.
You'll run it unmodified against a second, genuinely different adapter in milestone 6
— that's how this exercise *proves* adapters are interchangeable instead of just
asserting it in prose.

## Step 1 — the contract test (copy-paste)

This is test infrastructure, not the pattern logic itself, so it's given in full.

`src/test/java/com/isaqb/practice/portsandadapters/adapter/ApprovalRepositoryContractTest.java`:

```java
package com.isaqb.practice.portsandadapters.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.isaqb.practice.portsandadapters.core.ApprovalDecision;
import com.isaqb.practice.portsandadapters.core.port.ApprovalRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Every ApprovalRepository implementation - in-memory, file-backed, or anything else
 * a platform engineer plugs in later - must satisfy this same behavior. Subclasses
 * only provide a fresh instance via newRepository(); the test bodies never change.
 * That's the proof, not just the claim, that adapters are interchangeable: the exact
 * same assertions run against every adapter. Public (and its abstract method
 * `protected`) because concrete subclasses live in sibling packages -
 * `adapter.driven.memory` here, `adapter.driven.file` in milestone 6.
 */
public abstract class ApprovalRepositoryContractTest {

  protected abstract ApprovalRepository newRepository();

  @Test
  void returnsEmptyListWhenNothingSaved() {
    var repository = newRepository();

    assertTrue(repository.findByRequester("alice").isEmpty());
  }

  @Test
  void findsASavedDecisionByRequester() {
    var repository = newRepository();
    var decision =
        new ApprovalDecision(
            "alice", "bob", "payments-prod", true, "ok", Instant.parse("2026-08-01T09:00:00Z"));

    repository.save(decision);

    assertEquals(List.of(decision), repository.findByRequester("alice"));
  }

  @Test
  void doesNotReturnDecisionsForOtherRequesters() {
    var repository = newRepository();
    repository.save(
        new ApprovalDecision(
            "alice", "bob", "payments-prod", true, "ok", Instant.parse("2026-08-01T09:00:00Z")));

    assertTrue(repository.findByRequester("carol").isEmpty());
  }

  @Test
  void keepsMultipleDecisionsForTheSameRequesterInInsertionOrder() {
    var repository = newRepository();
    var first =
        new ApprovalDecision(
            "alice", "bob", "payments-prod", true, "first",
            Instant.parse("2026-08-01T09:00:00Z"));
    var second =
        new ApprovalDecision(
            "alice", "carol", "payments-staging", false, "second",
            Instant.parse("2026-08-01T10:00:00Z"));

    repository.save(first);
    repository.save(second);

    assertEquals(List.of(first, second), repository.findByRequester("alice"));
  }
}
```

Notice this file lives in `adapter/`, not `core/` — the contract test is *about*
adapters (it exercises the port from the outside), even though it only talks to the
port type, `ApprovalRepository`. It never mentions `InMemoryApprovalRepository` by
name.

## Step 2 — the adapter (write the two method bodies yourself)

`src/main/java/com/isaqb/practice/portsandadapters/adapter/driven/memory/InMemoryApprovalRepository.java`:

```java
package com.isaqb.practice.portsandadapters.adapter.driven.memory;

import com.isaqb.practice.portsandadapters.core.ApprovalDecision;
import com.isaqb.practice.portsandadapters.core.port.ApprovalRepository;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The simplest possible ApprovalRepository: an in-process list. Good enough for the
 * CLI and for tests. A platform engineer can later swap this for FileApprovalRepository
 * (milestone 6) or a real database without ApprovalService, or either port, changing
 * by a single line.
 */
public class InMemoryApprovalRepository implements ApprovalRepository {

  private final List<ApprovalDecision> decisions = new CopyOnWriteArrayList<>();

  @Override
  public void save(ApprovalDecision decision) {
    // TODO: store `decision`.
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public List<ApprovalDecision> findByRequester(String requester) {
    // TODO: return every stored decision whose requester() equals `requester`, in the
    // order they were saved. Return an empty list (never null) if there are none.
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

## Step 3 — wire the contract test to this adapter (copy-paste)

`src/test/java/com/isaqb/practice/portsandadapters/adapter/driven/memory/InMemoryApprovalRepositoryTest.java`:

```java
package com.isaqb.practice.portsandadapters.adapter.driven.memory;

import com.isaqb.practice.portsandadapters.adapter.ApprovalRepositoryContractTest;
import com.isaqb.practice.portsandadapters.core.port.ApprovalRepository;

class InMemoryApprovalRepositoryTest extends ApprovalRepositoryContractTest {

  @Override
  protected ApprovalRepository newRepository() {
    return new InMemoryApprovalRepository();
  }
}
```

This class has no `@Test` methods of its own — it inherits all four from
`ApprovalRepositoryContractTest` and just says which concrete adapter to run them
against.

## Checkpoint

```bash
mvn -f patterns/06-ports-and-adapters/pom.xml clean verify
```

All four inherited contract-test cases pass for `InMemoryApprovalRepositoryTest`.

Next: [`05-cli-adapter.md`](05-cli-adapter.md).
