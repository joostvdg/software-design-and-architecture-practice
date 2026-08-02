# Milestone 0 — Setup

## Goal

Confirm the module builds, understand the target package layout, and get oriented in
the case study before writing any pattern code.

## Confirm the build is green

From the repo root:

```bash
mvn -f patterns/01-layers/pom.xml clean verify
```

This should pass — right now the module only contains a placeholder test
(`SmokeTest`) and an empty `package-info.java`. That placeholder test is there so you
always have a green build to come back to; delete it once the domain layer (next
milestone) has its own tests.

## Target layout

By the end of milestone 4 you'll have:

```
src/main/java/com/isaqb/practice/layers/
  Main.java                          # Presentation: composition root + CLI entry point
  application/
    ValidateConfigUseCase.java       # Application: orchestrates the use case
    ConfigSource.java                # Application-defined port, implemented by Infrastructure
    ConfigLoadException.java
  domain/
    PipelineConfig.java              # Domain model
    ValidationRule.java              # Domain: rule contract
    ValidationError.java
    ValidationResult.java
    rules/
      NameMustNotBeBlank.java
      MustHaveAtLeastOneStage.java
      StageNamesMustBeUnique.java
  infrastructure/
    FileConfigSource.java            # Infrastructure: implements ConfigSource
```

Notice the dependency direction as you build this: `domain` imports nothing from this
project. `application` imports `domain`, and defines `ConfigSource` as an interface it
needs but does not implement. `infrastructure` imports `application` (to implement
`ConfigSource`) and `domain` (to produce/consume domain types) — never the other way
around. `Main` (Presentation) is the only class allowed to import `infrastructure`
directly, because it's the composition root: the one place that's allowed to know
about concrete technical detail and wire it into the abstractions the other layers
depend on.

## The case study, one more time

You're building the **Build Config Validator**: given a pipeline config file, decide
whether it's valid before PipelineForge's orchestrator would accept it. A config file
looks like this (you'll parse this exact format in milestone 3):

```
name: nightly-build
stage: compile
stage: test
stage: package
```

Three rules must hold: the config needs a non-blank `name`, at least one `stage`, and
no duplicate stage names. Each rule becomes one small class in `domain/rules/`.

## Checkpoint

- [ ] `mvn -f patterns/01-layers/pom.xml clean verify` passes.
- [ ] You can explain, in one sentence, why `Main` is the only class allowed to import
      `infrastructure` directly.

Next: [`01-domain-layer.md`](01-domain-layer.md).
