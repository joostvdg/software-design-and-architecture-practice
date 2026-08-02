# Milestone 0 — Setup

## Goal

Confirm the module builds, understand the target package layout, and get oriented in
the case study before writing any pattern code.

## Confirm the build is green

From the repo root:

```bash
mvn -f patterns/05-soa/pom.xml clean verify
```

This should pass — right now the module only contains a placeholder test
(`SmokeTest`) and an empty `package-info.java`. That placeholder test is there so you
always have a green build to come back to; delete it once the Service Catalog
(milestone 2) has its own tests.

## Target layout

By the end of milestone 5 you'll have:

```
src/main/java/com/isaqb/practice/soa/
  Main.java                                # Presentation: CLI entry point (platform engineer)
  ScheduledJobRunner.java                  # Presentation: second, unattended caller
  CatalogFactory.java                      # Composition root: builds + populates the shared catalog
  ReportFormatter.java                     # shared by both callers
  contract/
    ApprovalRequest.java
    ApprovalResponse.java
    RiskLevel.java
    DeploymentApprovalService.java
    ProvisioningRequest.java
    ProvisioningResponse.java
    EnvironmentProvisioningService.java
  catalog/
    ServiceCatalog.java                    # register/lookup by (name, version)
    ServiceKey.java
    ServiceNotFoundException.java
  service/
    DeploymentApprovalServiceImpl.java
    EnvironmentProvisioningServiceImpl.java
```

Notice the dependency direction: `contract` imports nothing from this project — it's
the stable seam everything else depends on. `catalog` imports nothing but the JDK — it
doesn't know or care what a `DeploymentApprovalService` is, it stores and returns
`Object`s keyed by `(name, version)`, type-checked generically at the call site.
`service` implements the interfaces in `contract`. `Main`, `ScheduledJobRunner`, and
`CatalogFactory` are the only classes that know about every package at once — they're
the composition roots, exactly like `Main` was in `01-layers`.

## The case study, one more time

You're building the **Deployment Services Catalog**: two small, business-aligned
services —

- **Deployment Approval Service** — given a deployment (id, target environment,
  requester, risk level), decides whether it may proceed.
- **Environment Provisioning Service** — given a target environment name, reserves it
  for use.

— registered in a **Service Catalog** under a stable `(name, version)` key, e.g.
`("deployment-approval", "v1")`. Two unrelated callers — a CLI a **platform engineer**
runs by hand, and a `ScheduledJobRunner` simulating an unattended nightly job — look
both services up *by contract*, never by constructing the implementation class
themselves. That indirection through the catalog is the entire point of this exercise:
it's what lets two callers that don't know about each other reuse the same governed
service.

Everything runs in one JVM, no networking — a deliberate simplification explained in
`../README.md` section 5. Real SOA deployments usually do involve a network call (and
sometimes an ESB) between consumer and service; what you're practicing here is the part
that's still true either way — contract-first design and catalog-mediated reuse.

## Checkpoint

- [ ] `mvn -f patterns/05-soa/pom.xml clean verify` passes.
- [ ] You can explain, in one sentence, why `Main` and `ScheduledJobRunner` must not
      construct `DeploymentApprovalServiceImpl` directly.

Next: [`01-service-contracts.md`](01-service-contracts.md).
