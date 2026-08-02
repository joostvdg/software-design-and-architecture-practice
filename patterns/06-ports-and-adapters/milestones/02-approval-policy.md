# Milestone 2 — Approval policy

## Goal

Write the core decision-rule logic: `ApprovalPolicy`, the contract, and
`DefaultApprovalPolicy`, the concrete rules that decide whether a request is even
eligible to be honored. This is deliberately the first milestone with real logic —
it's pure domain reasoning, no I/O, no framework, nothing but `ApprovalRequest` in and
a list of violations out. That purity is exactly what "the core has zero outward
dependencies" buys you: this class is trivially testable, and you're about to prove it.

## Step 1 — the contract (copy-paste)

`src/main/java/com/isaqb/practice/portsandadapters/core/ApprovalPolicy.java`:

```java
package com.isaqb.practice.portsandadapters.core;

import java.util.List;

/**
 * The core decision-rule logic: what makes an approval request acceptable at all,
 * independent of who is asking (CLI, HTTP, chat-bot) or where decisions end up stored
 * (memory, file, database). Returns every violated rule as a human-readable message;
 * an empty list means the request is policy-compliant.
 */
public interface ApprovalPolicy {

  List<String> violations(ApprovalRequest request);
}
```

## Step 2 — the rules (write this yourself)

`src/main/java/com/isaqb/practice/portsandadapters/core/DefaultApprovalPolicy.java`:

```java
package com.isaqb.practice.portsandadapters.core;

import java.util.List;

public class DefaultApprovalPolicy implements ApprovalPolicy {

  @Override
  public List<String> violations(ApprovalRequest request) {
    // TODO: return a list of human-readable violation messages, one per failing rule
    // below; return an empty list if every rule passes. Rules:
    //  1. requester must not be blank (null-safe: use request.requester(), which is
    //     never null thanks to the record's compact constructor, but it can be "" or
    //     all-whitespace).
    //  2. approver must not be blank.
    //  3. namespace must not be blank.
    //  4. justification must not be blank.
    //  5. requester and approver must not name the same person - compare
    //     case-insensitively, after trimming whitespace, so "alice" and " Alice "
    //     count as the same person. Nobody approves their own deployment.
    // Check every rule and collect every violation - don't stop at the first one, so
    // a caller sees the full picture in one pass. Hint: java.util.ArrayList<String>.
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

## Step 3 — tests (copy-paste, must pass once step 2 is done)

`src/test/java/com/isaqb/practice/portsandadapters/core/DefaultApprovalPolicyTest.java`:

```java
package com.isaqb.practice.portsandadapters.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DefaultApprovalPolicyTest {

  private final DefaultApprovalPolicy policy = new DefaultApprovalPolicy();

  @Test
  void passesForAWellFormedRequest() {
    var request =
        new ApprovalRequest("alice", "bob", "payments-prod", "on-call approved", true);

    assertTrue(policy.violations(request).isEmpty());
  }

  @Test
  void rejectsSelfApproval() {
    var request = new ApprovalRequest("alice", "Alice ", "payments-prod", "looks fine", true);

    assertTrue(!policy.violations(request).isEmpty());
  }

  @Test
  void rejectsBlankJustification() {
    var request = new ApprovalRequest("alice", "bob", "payments-prod", "   ", true);

    assertTrue(!policy.violations(request).isEmpty());
  }

  @Test
  void rejectsBlankNamespace() {
    var request = new ApprovalRequest("alice", "bob", " ", "on-call approved", true);

    assertTrue(!policy.violations(request).isEmpty());
  }

  @Test
  void collectsMultipleViolationsAtOnce() {
    // All four blank-field rules fire; self-approval does not (both sides are blank,
    // so there's no named requester/approver to be "the same person" as - the blank
    // checks above already cover this case).
    var request = new ApprovalRequest(" ", " ", " ", " ", true);

    assertEquals(4, policy.violations(request).size());
  }
}
```

Notice this test file imports nothing but `core` types and JUnit - no adapter, no
port even. `ApprovalPolicy` doesn't depend on `RequestApprovalUseCase` or
`ApprovalRepository` at all; it's a self-contained piece of business logic that would
work identically if you deleted every adapter in the module.

## Checkpoint

```bash
mvn -f patterns/06-ports-and-adapters/pom.xml clean verify
```

All five `DefaultApprovalPolicyTest` cases pass.

Next: [`03-approval-service.md`](03-approval-service.md).
