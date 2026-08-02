# Milestone 0 — Setup

## Goal

Confirm the module builds, understand the target `core` / `adapter` package layout,
and get oriented in the case study before writing any pattern code.

## Confirm the build is green

From the repo root:

```bash
mvn -f patterns/06-ports-and-adapters/pom.xml clean verify
```

This should pass — right now the module only contains a placeholder test
(`SmokeTest`) and an empty `package-info.java`. That placeholder test is there so you
always have a green build to come back to; delete it once the core (next milestone)
has its own code.

## Target layout

By the end of milestone 6 you'll have:

```
src/main/java/com/isaqb/practice/portsandadapters/
  core/
    ApprovalRequest.java                # domain model: what's being asked
    ApprovalDecision.java               # domain model: what was decided
    ApprovalPolicy.java                 # core-owned contract: what makes a request policy-compliant
    DefaultApprovalPolicy.java          # the concrete decision rules
    ApprovalService.java                # implements the driving port; depends on the driven port
    port/
      RequestApprovalUseCase.java       # driving (primary) port - core's entry point
      ApprovalRepository.java           # driven (secondary) port - what core calls out to
  adapter/
    driving/
      cli/
        Main.java                       # driving adapter + composition root
    driven/
      memory/
        InMemoryApprovalRepository.java # driven adapter #1
      file/
        FileApprovalRepository.java     # driven adapter #2 (milestone 6)
```

Notice the dependency direction as you build this: **everything under `core/`,
including `core/port/`, imports nothing from `adapter`.** Ever. Not even from `Main`.
Compare this to `01-layers`, where `Main` was *allowed* to import Infrastructure
because it was the composition root sitting at the top of the layer stack. Here, the
composition root (`Main`) isn't part of the core's world at all — it lives in
`adapter.driving.cli`, alongside the other adapters, and it depends on `core` (through
the ports), not the other way around. There is no class, anywhere in this module, for
which "import a concrete adapter" is acceptable *except* the adapters themselves and
`Main`. `core` never gets that exception.

Both `adapter.driving.*` and `adapter.driven.*` are peers: neither is "above" the
other. A driving adapter calls into `core` through `RequestApprovalUseCase`; a driven
adapter is called *by* `core` through `ApprovalRepository`. Both relationships point
the same direction — inward, toward `core` — which is the whole idea of a hexagon:
symmetric on every side, not a stack with a top and bottom.

## The case study, one more time

You're building the **Deployment Approval Service**: given a request to approve a
production deployment, decide whether it's policy-compliant, and if so, record the
approver's actual decision. Two rules must hold before a decision counts as
policy-compliant:

1. The requester and the approver must be different people — nobody approves their own
   deployment.
2. The justification must not be blank — every decision needs a stated reason.

If either rule is violated, the request is automatically denied with a reason
explaining *why*, regardless of what the approver intended. If both rules pass, the
decision reflects the approver's actual choice (approve or deny) and their stated
justification.

## Checkpoint

- [ ] `mvn -f patterns/06-ports-and-adapters/pom.xml clean verify` passes.
- [ ] You can explain, in one sentence, why `Main` — unlike `01-layers`' `Main` — does
      not live inside the boundary it wires together.

Next: [`01-core-domain-and-ports.md`](01-core-domain-and-ports.md).
