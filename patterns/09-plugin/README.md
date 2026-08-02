# Plugin

## 1. What it is

The Plugin pattern splits a system into a stable **core** and independently
addable/removable **extensions** that the core discovers and invokes only through a
fixed, narrow interface (the extension point). The core never imports a concrete
plugin class — it only knows the extension-point interface and a registry it can look
plugins up in by some identifier (a string id, a service-loader entry, a file in a
plugins directory). Adding a new plugin means writing a new class that implements the
interface and registering it; it must never require editing the core.

This is the structural pattern behind "the thing doesn't need to know every kind of
X in advance" — new step types, new file formats, new payment providers, new IDE
language support — anywhere a system's authors can't (or don't want to) enumerate
every extension up front. The core defines the *shape* extensions must have; each
plugin defines its own *behavior* within that shape.

The critical distinction from a plain `if/else` or `switch` over a type code is that
the core has **zero knowledge** of concrete plugin classes: it depends on the
interface and the registry only, in the same inward direction Ports and Adapters uses
for its ports — the difference is that here the "adapters" (plugins) are meant to be
added *without a release of the core*, potentially by a different team entirely.

## 2. Common use cases

- CI/CD systems where "what kinds of pipeline steps exist" grows over time (shell
  commands, container builds, notifications, approvals, ...).
- IDEs and build tools (language plugins, linters, formatters).
- Anything with a "drop a jar/module in a directory and it's picked up" extension
  model.

## 3. Trade-offs

| Helps | Hurts |
|---|---|
| Open/closed: add behavior without modifying or redeploying the core | An extra layer of indirection (registry + interface) for cases that will only ever have one or two variants |
| Independent development/release of plugins vs. the core | Versioning and compatibility become an explicit concern: a plugin built against an old contract can silently misbehave against a newer core |
| Encourages a narrow, well-thought-out extension contract | Isolation/sandboxing of plugin failures is a real problem this pattern doesn't solve by itself (see milestone 2's duplicate-id and lookup-failure handling) |
| Third parties can extend the system without seeing its source | Discovery/loading mechanisms (classpath scanning, `ServiceLoader`, plugin directories) add operational complexity of their own |

## 4. When *not* to use it

- When the set of variants is small, known, and unlikely to grow — a `switch` over an
  enum is simpler to read and to debug than an interface plus a registry plus a
  lookup.
- When extensions need deep, ad-hoc access to the core's internals to do their job —
  if the "narrow interface" keeps growing to expose more of the core, the abstraction
  is leaking and a plugin model is fighting the problem rather than solving it.
- When you need strong isolation (a misbehaving plugin must not be able to crash or
  starve the core) and you're not prepared to build (or adopt) real sandboxing —
  in-process plugins share fate with the core by default.

## 5. Case study: Pipeline Step Plugin Loader

- **Purpose:** PipelineForge's pipeline runner executes a pipeline as an ordered list
  of steps. Today's step types (run a shell-like command, send a notification) will
  not be the last ones — platform teams keep asking for new step types (a container
  build, a policy check, ...). Instead of the runner growing a new `if` branch per
  step type forever, each step type is a **plugin** implementing a stable
  `PipelineStepPlugin` interface, looked up by a string id in a `PluginRegistry`. The
  runner is written once and never touched again to support a new step type.
- **Actors:**
  - **Platform engineer** *(you)* — implements new `PipelineStepPlugin`s and registers
    them; also wires the demo pipeline in `Main`.
  - **Pipeline Runner** — the core. Executes a list of step definitions in order,
    looking each one up in the registry by its type id; never references a concrete
    plugin class.
  - **Step author** — whoever owns a given step type's logic (in this exercise,
    still you, but the point is that this role *could* be a different team, working
    from the `PipelineStepPlugin` contract alone).
- **Scope of this exercise:** a single JVM process, one package, plugins registered by
  hand in `Main` (no classpath scanning or `ServiceLoader` — that's a real discovery
  mechanism this exercise deliberately skips so the extension-*point* stays the focus,
  not the extension-*discovery* mechanism). No persistence, no concurrency.

## 6. Milestones

1. [`00-setup.md`](milestones/00-setup.md) — confirm the module builds, tour the case
   study and target package layout.
2. [`01-plugin-contract.md`](milestones/01-plugin-contract.md) — the
   `PipelineStepPlugin` extension-point interface and its supporting types
   (`StepContext`, `StepResult`).
3. [`02-registry.md`](milestones/02-registry.md) — `PluginRegistry`: register, look up
   by id, reject duplicate ids.
4. [`03-builtin-plugins.md`](milestones/03-builtin-plugins.md) — two concrete plugins,
   `ShellStepPlugin` and `NotifyStepPlugin`.
5. [`04-pipeline-runner.md`](milestones/04-pipeline-runner.md) — `PipelineRunner`,
   which executes a list of step definitions purely through the registry and the
   interface.
6. [`05-add-a-plugin-without-touching-runner.md`](milestones/05-add-a-plugin-without-touching-runner.md)
   — add a third plugin type, `DockerBuildStepPlugin`, and prove the runner didn't
   change.
7. [`06-build-and-release.md`](milestones/06-build-and-release.md) — build, scan, and
   (practice-)release this module.

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
