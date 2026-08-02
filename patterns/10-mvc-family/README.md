# MVC (+ MVVM / MVU / PAC)

## 1. What it is

Model-View-Controller separates a UI-driven system into three roles: the **Model**
(application state and the rules for changing it, ignorant of any UI), the **View**
(a read-only rendering of the Model, ignorant of how state changes happen), and the
**Controller** (accepts input, decides which Model mutation it implies, applies it,
then tells the View to re-render). The point is the same as Layers' dependency
direction, applied to UI: the Model must not import the View or the Controller, so
the same Model can be driven by more than one Controller/View pair (a CLI today, an
HTTP API tomorrow) without change.

The **MVC family** covers several variations on where the "who tells the View to
update" responsibility lives:

- **MVC (classic)** — the Controller explicitly calls the View's render method after
  mutating the Model. This is what this module implements.
- **MVVM (Model-View-ViewModel)** — a ViewModel exposes observable state; the View
  binds to it declaratively (typically via a UI framework's data-binding), so nothing
  explicitly calls "render" — the binding layer does it automatically when observed
  state changes.
- **MVU (Model-View-Update, "Elm architecture")** — there is no mutation at all: a
  pure `update(model, message) -> newModel` function returns a new Model, and the View
  is a pure `view(model) -> UI description` function. No object anywhere holds mutable
  state; the runtime loop is what "mutates" by holding the latest Model.
- **PAC (Presentation-Abstraction-Control)** — a hierarchical variant where the system
  is built of PAC *agents*, each with its own small Model/View/Controller triad,
  composed into a tree — used when a UI has semi-independent sub-parts that each need
  their own state and coordination logic, not just one flat MVC triad.

This module implements **classic MVC only** — enough to make the Model/View/
Controller separation and its testability payoff concrete — and discusses the other
three by contrast rather than implementing all four; the CPSA-F exam expects you to
recognize and contrast them, not ship all four from scratch.

## 2. Common use cases

- Any interactive UI (CLI, web, desktop, mobile) where "what can change" (Model)
  should be testable without driving the UI at all.
- Dashboards and admin tools where the same state needs more than one presentation
  (a text table here, potentially a JSON API elsewhere) without duplicating state
  rules.

## 3. Trade-offs

| Helps | Hurts |
|---|---|
| Model is testable without any UI — plain unit tests, no framework, no simulated input | Classic MVC's explicit "controller calls view.render()" is easy to forget, causing stale-looking UIs that silently don't reflect the latest Model |
| View can be swapped (or added to) without touching Model or Controller | For small/one-off UIs, three types and two boundaries is more ceremony than a single script needs |
| Controller centralizes "what input means" — one place to find all valid commands | The "V" in MVC means different things across MVC/MVVM/MVU (push vs. pull vs. pure) — mixing terminology across a team causes real confusion, which is exactly why the exam probes it |
| MVU's pure-function variant makes state changes trivially testable and replayable (no hidden mutation) | MVU/MVVM require either a runtime loop or a data-binding framework — not "just three classes" the way classic MVC is |

## 4. When *not* to use it

- Trivial scripts / one-shot CLI tools with no ongoing state to observe or re-render —
  the separation buys nothing if there's no "later" moment where the same state needs
  re-rendering.
- When you're already inside a framework that owns MVVM/MVU for you (most modern
  frontend frameworks) — re-inventing a manual Controller on top fights the framework.
- When the "View" would need to reach back into Model internals to render correctly —
  a sign the Model isn't exposing state at the right shape/level of abstraction yet.

## 5. Case study: Pipeline Run Status Dashboard

- **Purpose:** PipelineForge's platform engineers want a live-ish text dashboard of
  in-flight pipeline runs: id, status, and stage progress (`3/5 stages`). Runs
  advance (a stage completes) or fail via commands; the dashboard must reflect the
  latest state every time it's shown, without the thing tracking run state needing to
  know it's being displayed as a text table.
- **Actors:**
  - **Platform engineer** *(you)* — types commands at a CLI prompt: list runs, advance
    a run's stage, fail a run, refresh the view.
  - **PipelineRunModel** — owns the list of runs and the rules for changing them (e.g.
    you can't advance a finished run); knows nothing about text formatting or stdin.
  - **PipelineDashboardView** — renders the Model's current state as a text table;
    knows nothing about commands or how state got the way it is.
  - **DashboardController** — the only class that talks to both; maps a typed command
    to a Model mutation, then explicitly tells the View to re-render.
- **Scope of this exercise:** a single JVM process, in-memory state, a synchronous CLI
  command loop (no concurrency, no networking). Deliberately classic/explicit MVC —
  milestone 4 discusses, in prose, what would change under MVVM and MVU, without
  implementing either.

## 6. Milestones

1. [`00-setup.md`](milestones/00-setup.md) — confirm the module builds, tour the case
   study and target package layout.
2. [`01-model.md`](milestones/01-model.md) — `PipelineRun` and `PipelineRunModel`:
   state plus the rules for changing it.
3. [`02-view.md`](milestones/02-view.md) — `PipelineDashboardView`: read-only
   rendering of the Model as a text table.
4. [`03-controller.md`](milestones/03-controller.md) — `DashboardController`: maps
   commands to Model mutations and triggers View re-renders.
5. [`04-cli-input-loop.md`](milestones/04-cli-input-loop.md) — `Main`: the CLI driving
   adapter, plus a written contrast with MVVM/MVU/PAC.
6. [`05-build-and-release.md`](milestones/05-build-and-release.md) — build, scan, and
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
