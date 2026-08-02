# Milestone 3 — Deployment Approval Service

## Goal

Implement `DeploymentApprovalServiceImpl`: the business logic behind the Deployment
Approval Service's contract. This is ordinary domain logic — nothing about the catalog
or the callers matters here, only "given a request, what does PipelineForge's approval
policy decide?" That separation is exactly the point of a contract: this class could be
rewritten from scratch, or replaced by a call to an entirely different system, and
neither `ServiceCatalog` nor either caller would need to change, as long as the
contract's behavior is preserved.

## Step 1 — the class shell (write the body yourself)

`src/main/java/com/isaqb/practice/soa/service/DeploymentApprovalServiceImpl.java`:

```java
package com.isaqb.practice.soa.service;

import com.isaqb.practice.soa.contract.ApprovalRequest;
import com.isaqb.practice.soa.contract.ApprovalResponse;
import com.isaqb.practice.soa.contract.DeploymentApprovalService;
import com.isaqb.practice.soa.contract.RiskLevel;

/** PipelineForge's approval policy: the one implementation of DeploymentApprovalService. */
public class DeploymentApprovalServiceImpl implements DeploymentApprovalService {

  private static final String REQUIRED_APPROVER_FOR_HIGH_RISK = "release-manager";

  @Override
  public ApprovalResponse decide(ApprovalRequest request) {
    // TODO: implement PipelineForge's approval policy:
    //  - LOW or MEDIUM risk: always approved.
    //  - HIGH risk requested by exactly REQUIRED_APPROVER_FOR_HIGH_RISK: approved.
    //  - HIGH risk requested by anyone else: rejected, with a reason that says
    //    high-risk deployments require release-manager approval.
    // Use ApprovalResponse.approved(reason) / ApprovalResponse.rejected(reason) - both
    // already exist on the record from milestone 1.
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

## Step 2 — tests (copy-paste, must pass once step 1 is done)

`src/test/java/com/isaqb/practice/soa/service/DeploymentApprovalServiceImplTest.java`:

```java
package com.isaqb.practice.soa.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.isaqb.practice.soa.contract.ApprovalRequest;
import com.isaqb.practice.soa.contract.RiskLevel;
import org.junit.jupiter.api.Test;

class DeploymentApprovalServiceImplTest {

  private final DeploymentApprovalServiceImpl service = new DeploymentApprovalServiceImpl();

  @Test
  void lowRiskIsAlwaysApproved() {
    var response =
        service.decide(new ApprovalRequest("dep-1", "staging", "anyone", RiskLevel.LOW));

    assertTrue(response.approved());
  }

  @Test
  void mediumRiskIsAlwaysApproved() {
    var response =
        service.decide(new ApprovalRequest("dep-1", "staging", "anyone", RiskLevel.MEDIUM));

    assertTrue(response.approved());
  }

  @Test
  void highRiskByReleaseManagerIsApproved() {
    var response =
        service.decide(
            new ApprovalRequest("dep-1", "production", "release-manager", RiskLevel.HIGH));

    assertTrue(response.approved());
  }

  @Test
  void highRiskByAnyoneElseIsRejected() {
    var response =
        service.decide(new ApprovalRequest("dep-1", "production", "ci-bot", RiskLevel.HIGH));

    assertFalse(response.approved());
    assertTrue(response.reason().toLowerCase().contains("release-manager"));
  }
}
```

## Checkpoint

```bash
mvn -f patterns/05-soa/pom.xml clean verify
```

All four `DeploymentApprovalServiceImplTest` cases pass. Notice this class isn't
registered anywhere yet, and nothing calls it through the catalog yet - that wiring is
milestone 5. Right now you're only proving the policy itself is correct in isolation,
the same way milestone 1 of `01-layers` proved its validation rules in isolation before
any use case wired them together.

Next: [`04-environment-provisioning-service.md`](04-environment-provisioning-service.md).
