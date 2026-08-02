# Pattern folder authoring template

Internal spec for how every `NN-slug/` pattern folder under `patterns/` is built. This
file is for whoever (human or agent) is authoring a new pattern folder — it is not part
of the learner-facing content. If you are building a new pattern folder, follow this
exactly; `01-layers/` is the worked reference implementation of this spec.

## Universe

All case studies live in **PipelineForge**, a fictional internal developer platform
team's CI/CD + Kubernetes deployment platform. Every pattern gets its own bounded
context inside PipelineForge — pick something that would plausibly be a real slice of
that platform (a validator, a queue, a dashboard, an approval workflow, ...), not a
generic "Order/Customer" textbook example. This keeps every pattern's case study
familiar without the learner needing new domain knowledge each time, and mirrors the
"one example from your CI/CD / K8s / past Java work" framing in `../isaqb-study-prep.md`.

## Folder layout

```
NN-slug/
  README.md
  pom.xml
  sonar-project.properties
  milestones/
    00-setup.md
    01-<first-concept>.md
    ...
    0N-build-and-release.md
  src/main/java/com/isaqb/practice/<slug>/package-info.java
  src/test/java/com/isaqb/practice/<slug>/SmokeTest.java
```

- `NN` = two-digit index matching the table in the root plan / this file's pattern list.
- `slug` = kebab-case, matches the module's Maven `artifactId` and the tail of the Java
  package `com.isaqb.practice.<slug with dashes removed or camelCased if needed>`
  (Java packages can't contain dashes — e.g. slug `pipes-and-filters` → package
  `com.isaqb.practice.pipesandfilters`).

## `pom.xml` (module)

Minimal — inherits everything from the root parent:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>com.isaqb.practice</groupId>
    <artifactId>patterns</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
  </parent>

  <artifactId>SLUG</artifactId>
  <name>ISAQB Practice - PATTERN NAME</name>

  <build>
    <finalName>${project.artifactId}-${project.version}</finalName>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-jar-plugin</artifactId>
        <configuration>
          <archive>
            <manifest>
              <mainClass>com.isaqb.practice.PACKAGE.Main</mainClass>
            </manifest>
          </archive>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

Replace `SLUG`, `PATTERN NAME`, `PACKAGE` accordingly. If the pattern's milestones don't
end up with a single runnable `Main` (e.g. multi-service patterns), drop the
`mainClass` entry rather than pointing it at something that doesn't exist yet — the
build milestone will fix it up when the entry point exists.

## `sonar-project.properties`

```properties
sonar.projectKey=isaqb-practice-SLUG
sonar.projectName=ISAQB Practice - PATTERN NAME
sonar.sources=src/main/java
sonar.tests=src/test/java
sonar.java.binaries=target/classes
sonar.java.test.binaries=target/test-classes
sonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
```

## Starter skeleton (`src/main`, `src/test`)

Just enough for `mvn -f patterns/NN-slug/pom.xml verify` to pass from a clean checkout,
**before** the learner starts milestone 1:

- `package-info.java` with a one-line package Javadoc naming the bounded context.
- One trivial passing JUnit 5 test (`SmokeTest`) asserting `true` or similar, so the
  learner sees a green build immediately and the CI/release pipeline has something to
  run on day one. Do not pre-implement any of the pattern itself here — that's what the
  milestones are for.

## `README.md` structure

```markdown
# <Pattern Name>

## 1. What it is
(2-4 paragraphs: the pattern's structure, building blocks, relationships.)

## 2. Common use cases
(bulleted)

## 3. Trade-offs
(table or bullets: qualities it helps / qualities it hurts)

## 4. When *not* to use it
(bulleted, concrete)

## 5. Case study: <Bounded Context Name>
- **Purpose:** why this bounded context exists inside PipelineForge
- **Actors:** who/what interacts with it (people, services, schedulers, ...)
- **Scope of this exercise:** what's in/out for the practice implementation

## 6. Milestones
(numbered list linking to milestones/*.md, one line each on what it adds)

## For AI agents working in this folder
Reuses the standard paragraph below verbatim (see "AI agent guardrail" section).
```

Content must satisfy the study-doc's five practice questions for the pattern (problem/
context, structure, qualities helped/hurt, when not to use, a CI/CD-or-K8s example) —
sections 1-5 above map directly onto those.

## Milestone files (`milestones/*.md`)

- `00-setup.md`: create the module (if not already scaffolded), confirm `mvn verify`
  is green, orient the learner in the case study and the package layout they'll build.
- Middle milestones: one coherent increment each (e.g. "define the domain layer",
  "add the first filter", "wire the second bounded service"). Each milestone:
  - States the **goal** in a sentence.
  - Gives **copy-pastable Java** for the parts that are mechanical/boilerplate or where
    typing it out teaches nothing (package declarations, imports, interface shells,
    test scaffolding).
  - Leaves the **core pattern logic** as a described task with a signature/contract and
    a test that must pass, rather than a filled-in method body — the learner writes
    that part. Say explicitly what the method/class must do, not just its name.
  - Ends with a **checkpoint**: the exact `mvn` command to run and what "done" looks
    like.
- Final milestone, `0N-build-and-release.md`: does **not** repeat the release process.
  It names this module's `application_slug` (`isaqb-practice-<slug>`), links
  `../../RELEASE.md`, `../../AGENTS-good-release.md`, `../../AGENTS-record-release.md`,
  and notes anything module-specific (e.g. which class is the entry point for the jar).

## AI agent guardrail (verbatim block for every README.md)

```markdown
## For AI agents working in this folder

- Work one milestone at a time, in order. Don't jump ahead or generate a later
  milestone's content unprompted.
- Never generate a whole milestone's (or the whole pattern's) implementation in one
  shot. Prefer producing signatures, interfaces, and TODO-marked stubs, plus the test
  that defines correct behavior - then let the human write the method bodies.
- When asked "how would this work" or for a hint, give a short snippet or explanation,
  not the full solution.
- Explain *why* a step exists (which quality attribute it demonstrates), not just what
  to type.
- For the release/build milestone, don't invent a release process: follow
  `../../RELEASE.md`, `../../AGENTS-good-release.md`, and `../../AGENTS-record-release.md`.
```

## Pattern list (Tier 1 + Tier 2, this pass)

| NN | slug | Pattern | package tail |
|----|------|---------|---------------|
| 01 | layers | Layers | `layers` |
| 02 | pipes-and-filters | Pipes and Filters | `pipesandfilters` |
| 03 | microservices | Microservices | `microservices` |
| 04 | broker | Broker | `broker` |
| 05 | soa | SOA | `soa` |
| 06 | ports-and-adapters | Ports and Adapters | `portsandadapters` |
| 07 | cqrs | CQRS | `cqrs` |
| 08 | event-sourcing | Event Sourcing | `eventsourcing` |
| 09 | plugin | Plugin | `plugin` |
| 10 | mvc-family | MVC (+ MVVM/MVU/PAC discussed) | `mvcfamily` |
| 11 | dependency-injection | Dependency Injection | `di` |
| 12 | rpc | RPC | `rpc` |

Constraints that apply to every module's `src/main/java`: plain Java 25, JDK standard
library only (e.g. `com.sun.net.httpserver.HttpServer` / `java.net.http.HttpClient` are
fine for the networked patterns - they ship with the JDK). No third-party
application frameworks. JUnit 5 (Jupiter) is allowed in `src/test/java` only.
