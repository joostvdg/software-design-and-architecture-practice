# Milestone 1 — Service contracts

## Goal

Define the stable, versioned **contracts** both services and both callers depend on:
the request/response DTOs and the two service interfaces. In SOA, the contract is the
thing that must not casually change once consumers depend on it — that's why it lives
in its own package, with nothing but plain data and interfaces in it, and why it will
turn out to be the *only* thing `catalog`, `service`, and the two callers all share.

This milestone is entirely declarative — records and interfaces have no behavior to get
wrong — so everything below is copy-paste. The interesting design decisions (what goes
in a request, what a response needs to communicate) have already been made for you;
milestone 3 and 4 are where you'll write actual logic against these shapes.

## Step 1 — risk level (copy-paste)

`src/main/java/com/isaqb/practice/soa/contract/RiskLevel.java`:

```java
package com.isaqb.practice.soa.contract;

/** How risky a deployment is judged to be, as declared by its requester. */
public enum RiskLevel {
  LOW,
  MEDIUM,
  HIGH
}
```

## Step 2 — the Deployment Approval Service contract (copy-paste)

`src/main/java/com/isaqb/practice/soa/contract/ApprovalRequest.java`:

```java
package com.isaqb.practice.soa.contract;

/** A request to decide whether a deployment may proceed. */
public record ApprovalRequest(
    String deploymentId, String environment, String requestedBy, RiskLevel riskLevel) {}
```

`src/main/java/com/isaqb/practice/soa/contract/ApprovalResponse.java`:

```java
package com.isaqb.practice.soa.contract;

/** The Deployment Approval Service's decision, and why it decided that. */
public record ApprovalResponse(boolean approved, String reason) {

  public static ApprovalResponse approved(String reason) {
    return new ApprovalResponse(true, reason);
  }

  public static ApprovalResponse rejected(String reason) {
    return new ApprovalResponse(false, reason);
  }
}
```

`src/main/java/com/isaqb/practice/soa/contract/DeploymentApprovalService.java`:

```java
package com.isaqb.practice.soa.contract;

/**
 * The Deployment Approval Service's contract. Consumers depend on this interface only -
 * never on whichever class implements it, and never construct that class themselves.
 */
public interface DeploymentApprovalService {

  ApprovalResponse decide(ApprovalRequest request);
}
```

## Step 3 — the Environment Provisioning Service contract (copy-paste)

`src/main/java/com/isaqb/practice/soa/contract/ProvisioningRequest.java`:

```java
package com.isaqb.practice.soa.contract;

/** A request to reserve a target environment for a deployment. */
public record ProvisioningRequest(String environmentName, String requestedBy) {}
```

`src/main/java/com/isaqb/practice/soa/contract/ProvisioningResponse.java`:

```java
package com.isaqb.practice.soa.contract;

/** The Environment Provisioning Service's result: what got reserved, and under what id. */
public record ProvisioningResponse(boolean reserved, String environmentId, String message) {

  public static ProvisioningResponse reserved(String environmentId, String message) {
    return new ProvisioningResponse(true, environmentId, message);
  }

  public static ProvisioningResponse rejected(String message) {
    return new ProvisioningResponse(false, "", message);
  }
}
```

`src/main/java/com/isaqb/practice/soa/contract/EnvironmentProvisioningService.java`:

```java
package com.isaqb.practice.soa.contract;

/** The Environment Provisioning Service's contract. Same rule as its sibling above. */
public interface EnvironmentProvisioningService {

  ProvisioningResponse provision(ProvisioningRequest request);
}
```

## Checkpoint

```bash
mvn -f patterns/05-soa/pom.xml clean verify
```

Still green — there's nothing to test yet, only shapes. Take a moment to check: does
anything in `contract/` import anything else from this project? It shouldn't — this
package is the stable seam everything else in the module will depend on.

Next: [`02-service-catalog.md`](02-service-catalog.md).
