# Milestone 5 — Callers and composition root

## Goal

Wire everything together, and prove the reuse claim from the README for real: two
independent callers, `Main` (a CLI a platform engineer runs by hand) and
`ScheduledJobRunner` (a simulated unattended job), both looking up and calling the
*same* service contracts through the *same* catalog, neither one aware of the other.

`CatalogFactory` is the composition root - the one place in this module that names
every concrete `service` class. It stands in for what, in a real deployment, would be a
long-running registry process both callers reach over the network: since this exercise
is deliberately in-process (see `../README.md` section 5), both callers instead call
this one factory method to obtain their catalog. That's a practice-repo simplification,
not a claim that real SOA catalogs are compiled into every consumer's `Main`.

## Step 1 — the composition root (copy-paste)

`src/main/java/com/isaqb/practice/soa/CatalogFactory.java`:

```java
package com.isaqb.practice.soa;

import com.isaqb.practice.soa.catalog.ServiceCatalog;
import com.isaqb.practice.soa.contract.DeploymentApprovalService;
import com.isaqb.practice.soa.contract.EnvironmentProvisioningService;
import com.isaqb.practice.soa.service.DeploymentApprovalServiceImpl;
import com.isaqb.practice.soa.service.EnvironmentProvisioningServiceImpl;

/**
 * Builds and populates the Service Catalog both callers use. In a real deployment this
 * catalog would be a long-running registry process reached over the network; here, every
 * caller obtains an identically-populated catalog from this one factory method instead of
 * constructing a service implementation directly.
 */
public final class CatalogFactory {

  public static final String DEPLOYMENT_APPROVAL = "deployment-approval";
  public static final String ENVIRONMENT_PROVISIONING = "environment-provisioning";
  public static final String V1 = "v1";

  private CatalogFactory() {}

  public static ServiceCatalog createDefault() {
    ServiceCatalog catalog = new ServiceCatalog();
    catalog.register(
        DEPLOYMENT_APPROVAL, V1, DeploymentApprovalService.class,
        new DeploymentApprovalServiceImpl());
    catalog.register(
        ENVIRONMENT_PROVISIONING, V1, EnvironmentProvisioningService.class,
        new EnvironmentProvisioningServiceImpl());
    return catalog;
  }
}
```

## Step 2 — the shared report formatter (write the body yourself)

Both callers need to turn an `ApprovalResponse` (and, if approval succeeded, a
`ProvisioningResponse`) into a human-readable line of output. Rather than each caller
formatting its own output, they share one small formatter - itself a tiny piece of
reuse, the same instinct as the catalog, just for presentation instead of business
logic.

`src/main/java/com/isaqb/practice/soa/ReportFormatter.java`:

```java
package com.isaqb.practice.soa;

import com.isaqb.practice.soa.contract.ApprovalResponse;
import com.isaqb.practice.soa.contract.ProvisioningResponse;

/** Turns a decision (and, if any, a provisioning result) into one human-readable report. */
public final class ReportFormatter {

  private ReportFormatter() {}

  /**
   * @param provisioning may be null - callers only attempt provisioning when the
   *     deployment was approved, so a rejected deployment never has one.
   */
  public static String format(
      String deploymentId, ApprovalResponse approval, ProvisioningResponse provisioning) {
    // TODO:
    //  - First line: "deployment <deploymentId>: APPROVED (<reason>)" or
    //    "deployment <deploymentId>: REJECTED (<reason>)", matching approval.approved().
    //  - If provisioning is null, add a second line saying the environment was not
    //    provisioned (because the deployment was rejected).
    //  - If provisioning is non-null and reserved(), add a second line naming the
    //    reserved environmentId.
    //  - If provisioning is non-null and NOT reserved(), add a second line with its
    //    message explaining why.
    //  Join the lines with "\n". Exact wording is up to you; the tests below only check
    //  for specific substrings, the same way MainTest did in 01-layers.
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

## Step 3 — Main, the platform engineer's CLI (copy-paste)

`src/main/java/com/isaqb/practice/soa/Main.java`:

```java
package com.isaqb.practice.soa;

import com.isaqb.practice.soa.catalog.ServiceCatalog;
import com.isaqb.practice.soa.contract.ApprovalRequest;
import com.isaqb.practice.soa.contract.ApprovalResponse;
import com.isaqb.practice.soa.contract.DeploymentApprovalService;
import com.isaqb.practice.soa.contract.EnvironmentProvisioningService;
import com.isaqb.practice.soa.contract.ProvisioningRequest;
import com.isaqb.practice.soa.contract.ProvisioningResponse;
import com.isaqb.practice.soa.contract.RiskLevel;
import java.util.Locale;

/**
 * The platform engineer's entry point: a CLI that asks the Deployment Approval Service
 * whether one deployment may proceed, and, if so, asks the Environment Provisioning
 * Service to reserve its target environment. Both services are obtained from the Service
 * Catalog - this class never names DeploymentApprovalServiceImpl or
 * EnvironmentProvisioningServiceImpl.
 */
public final class Main {

  private Main() {}

  public static void main(String[] args) {
    if (args.length != 4) {
      System.err.println(
          "usage: soa <deploymentId> <environment> <requestedBy> <riskLevel: LOW|MEDIUM|HIGH>");
      System.exit(2);
      return;
    }

    String deploymentId = args[0];
    String environment = args[1];
    String requestedBy = args[2];
    RiskLevel riskLevel;
    try {
      riskLevel = RiskLevel.valueOf(args[3].toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      System.err.println("invalid risk level: " + args[3]);
      System.exit(2);
      return;
    }

    ServiceCatalog catalog = CatalogFactory.createDefault();
    DeploymentApprovalService approvalService =
        catalog.lookup(
            CatalogFactory.DEPLOYMENT_APPROVAL, CatalogFactory.V1, DeploymentApprovalService.class);
    EnvironmentProvisioningService provisioningService =
        catalog.lookup(
            CatalogFactory.ENVIRONMENT_PROVISIONING,
            CatalogFactory.V1,
            EnvironmentProvisioningService.class);

    ApprovalResponse approval =
        approvalService.decide(new ApprovalRequest(deploymentId, environment, requestedBy, riskLevel));

    ProvisioningResponse provisioning = null;
    if (approval.approved()) {
      provisioning = provisioningService.provision(new ProvisioningRequest(environment, requestedBy));
    }

    System.out.println(ReportFormatter.format(deploymentId, approval, provisioning));
    boolean success = approval.approved() && (provisioning == null || provisioning.reserved());
    System.exit(success ? 0 : 1);
  }
}
```

## Step 4 — ScheduledJobRunner, the unattended second caller (copy-paste)

`src/main/java/com/isaqb/practice/soa/ScheduledJobRunner.java`:

```java
package com.isaqb.practice.soa;

import com.isaqb.practice.soa.catalog.ServiceCatalog;
import com.isaqb.practice.soa.contract.ApprovalRequest;
import com.isaqb.practice.soa.contract.ApprovalResponse;
import com.isaqb.practice.soa.contract.DeploymentApprovalService;
import com.isaqb.practice.soa.contract.EnvironmentProvisioningService;
import com.isaqb.practice.soa.contract.ProvisioningRequest;
import com.isaqb.practice.soa.contract.ProvisioningResponse;
import com.isaqb.practice.soa.contract.RiskLevel;
import java.util.List;

/**
 * Simulates an unattended, scheduled caller of the exact same service contracts Main
 * uses - e.g. a nightly job sweeping a fixed batch of pending deployments. No human runs
 * this by hand; it exists to prove the Service Catalog, not the CLI, is what both callers
 * actually depend on. Note this class never names DeploymentApprovalServiceImpl or
 * EnvironmentProvisioningServiceImpl either - same rule as Main.
 */
public final class ScheduledJobRunner {

  private ScheduledJobRunner() {}

  private record ScheduledDeployment(
      String deploymentId, String environment, String requestedBy, RiskLevel riskLevel) {}

  public static void main(String[] args) {
    List<ScheduledDeployment> nightlyBatch =
        List.of(
            new ScheduledDeployment("dep-101", "staging", "ci-bot", RiskLevel.LOW),
            new ScheduledDeployment("dep-102", "production", "ci-bot", RiskLevel.HIGH));

    ServiceCatalog catalog = CatalogFactory.createDefault();
    DeploymentApprovalService approvalService =
        catalog.lookup(
            CatalogFactory.DEPLOYMENT_APPROVAL, CatalogFactory.V1, DeploymentApprovalService.class);
    EnvironmentProvisioningService provisioningService =
        catalog.lookup(
            CatalogFactory.ENVIRONMENT_PROVISIONING,
            CatalogFactory.V1,
            EnvironmentProvisioningService.class);

    for (ScheduledDeployment deployment : nightlyBatch) {
      ApprovalResponse approval =
          approvalService.decide(
              new ApprovalRequest(
                  deployment.deploymentId(),
                  deployment.environment(),
                  deployment.requestedBy(),
                  deployment.riskLevel()));

      ProvisioningResponse provisioning =
          approval.approved()
              ? provisioningService.provision(
                  new ProvisioningRequest(deployment.environment(), deployment.requestedBy()))
              : null;

      System.out.println(ReportFormatter.format(deployment.deploymentId(), approval, provisioning));
    }
  }
}
```

## Step 5 — test (copy-paste, must pass once step 2 is done)

`src/test/java/com/isaqb/practice/soa/ReportFormatterTest.java`:

```java
package com.isaqb.practice.soa;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.isaqb.practice.soa.contract.ApprovalResponse;
import com.isaqb.practice.soa.contract.ProvisioningResponse;
import org.junit.jupiter.api.Test;

class ReportFormatterTest {

  @Test
  void formatsApprovedAndReservedReport() {
    var approval = ApprovalResponse.approved("looks fine");
    var provisioning = ProvisioningResponse.reserved("staging-abc123", "environment reserved");

    String report = ReportFormatter.format("dep-1", approval, provisioning);

    assertTrue(report.contains("dep-1"));
    assertTrue(report.toUpperCase().contains("APPROVED"));
    assertTrue(report.contains("staging-abc123"));
  }

  @Test
  void formatsRejectedReportWithoutProvisioning() {
    var approval =
        ApprovalResponse.rejected("high-risk deployments require release-manager approval");

    String report = ReportFormatter.format("dep-2", approval, null);

    assertTrue(report.toUpperCase().contains("REJECT"));
    assertTrue(report.contains("dep-2"));
  }

  @Test
  void formatsApprovedButNotReservedReport() {
    var approval = ApprovalResponse.approved("looks fine");
    var provisioning = ProvisioningResponse.rejected("unknown environment: nope");

    String report = ReportFormatter.format("dep-3", approval, provisioning);

    assertTrue(report.contains("dep-3"));
    assertTrue(report.contains("unknown environment"));
  }
}
```

## Step 6 — try it for real

```bash
mvn -f patterns/05-soa/pom.xml clean package
java -jar patterns/05-soa/target/soa-1.0.0-SNAPSHOT.jar dep-1 staging alice LOW
java -jar patterns/05-soa/target/soa-1.0.0-SNAPSHOT.jar dep-2 production alice HIGH
java -jar patterns/05-soa/target/soa-1.0.0-SNAPSHOT.jar dep-3 production release-manager HIGH
```

The first and third should print an approved-and-reserved report and exit `0`; the
second should print a rejected report (no release-manager sign-off) and exit `1`. Then
run the unattended caller directly, without a `-jar`/manifest entry point, to see the
same services reused by a completely different caller:

```bash
mvn -f patterns/05-soa/pom.xml clean compile
java -cp patterns/05-soa/target/classes com.isaqb.practice.soa.ScheduledJobRunner
```

You should see two reports printed - one per hard-coded item in `nightlyBatch` - built
by looking up the exact same `DeploymentApprovalService`/`EnvironmentProvisioningService`
contracts as `Main`, from an independently-built catalog instance populated by the same
`CatalogFactory`.

## Checkpoint

- [ ] `mvn -f patterns/05-soa/pom.xml clean verify` passes, every milestone's tests
      green.
- [ ] Both `Main` invocations and the `ScheduledJobRunner` run in step 6 behave as
      described.
- [ ] You can point to the one place you'd add a line to register a third service
      version (e.g. `"deployment-approval"`, `"v2"`) — and confirm neither `Main` nor
      `ScheduledJobRunner` would need to change to keep using `"v1"`.

Next: [`06-build-and-release.md`](06-build-and-release.md).
