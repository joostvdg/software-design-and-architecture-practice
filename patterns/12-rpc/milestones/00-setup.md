# Milestone 0 — Setup

## Goal

Confirm the module builds, understand the target two-process package layout, and get
oriented in the case study before writing any pattern code.

## Confirm the build is green

From the repo root:

```bash
mvn -f patterns/12-rpc/pom.xml clean verify
```

This should pass — right now the module only contains a placeholder test
(`SmokeTest`) and an empty `package-info.java`. That placeholder test is there so you
always have a green build to come back to; delete it once the manager domain
(next milestone) has its own tests.

## Target layout

By the end of milestone 5 you'll have:

```
src/main/java/com/isaqb/practice/rpc/
  manager/
    FleetManagerMain.java          # composition root + HttpServer for the Fleet Manager
    NodeHealth.java                # one node's reported health
    HealthStatus.java              # enum: HEALTHY, DEGRADED, UNHEALTHY
    NodeHealthStore.java           # in-memory, latest health per nodeId
    HeartbeatWireFormat.java       # manager's own decode of the wire format
    HttpResult.java
    FleetManagerRequestHandler.java
  agent/
    NodeAgentMain.java             # periodic heartbeat loop, drives FleetManagerClient
    FleetManagerClient.java        # the RPC stub: looks local, does an HTTP POST
    HeartbeatWireFormat.java       # agent's own encode of the wire format (separate class!)
    HeartbeatResult.java
    ManagerUnavailableException.java
```

Notice there are **two** `HeartbeatWireFormat` classes, one per package, each
implementing only the direction it needs (manager decodes, agent encodes). This
mirrors `03-microservices`' Orchestrator/Registry split: the two sides agree on field
names, never on shared Java code — exactly as two independently-owned real services
would. It also means neither package imports anything from the other; the only thing
connecting them is the network and the wire contract.

## The case study, one more time

You're building the **Node Heartbeat RPC**: a Node Agent that reports its health to a
Fleet Manager via a call that reads like a plain method —
`client.reportHeartbeat(health)` — but is actually an HTTP POST. The wire format
(you'll implement both directions independently in milestones 2 and 3):

```
nodeId=node-7&status=HEALTHY&timestampMillis=1732550400000
```

## Checkpoint

- [ ] `mvn -f patterns/12-rpc/pom.xml clean verify` passes.
- [ ] You can explain, in one sentence, why this exercise gives `manager` and `agent`
      *separate* wire-format classes instead of one shared one.

Next: [`01-manager-domain.md`](01-manager-domain.md).
