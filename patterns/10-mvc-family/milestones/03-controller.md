# Milestone 3 — The Controller

## Goal

Build `DashboardController`: the only class that knows about both the Model and the
View. It exposes one method per user-facing command, and after every mutating
command, it explicitly triggers a re-render — the exact "controller calls
`view.render()`" step that section 1 of the README calls out as classic MVC's
easy-to-forget responsibility (and that MVVM/MVU handle differently, discussed in
milestone 4).

## Step 1 — `DashboardController` (write the wiring logic yourself)

Create `src/main/java/com/isaqb/practice/mvcfamily/DashboardController.java`:

```java
package com.isaqb.practice.mvcfamily;

/**
 * Maps user-facing commands to Model mutations, then re-renders the View. This is the
 * only class in the package allowed to depend on both PipelineRunModel and
 * PipelineDashboardView.
 */
public class DashboardController {

  private final PipelineRunModel model;
  private final PipelineDashboardView view;

  public DashboardController(PipelineRunModel model, PipelineDashboardView view) {
    this.model = model;
    this.view = view;
  }

  /** Adds a new run, then returns the freshly rendered dashboard. */
  public String addRun(String id, int stagesTotal) {
    // TODO: call model.addRun(...), then return view.render(model).
    throw new UnsupportedOperationException("not implemented yet");
  }

  /** Advances a run's stage, then returns the freshly rendered dashboard. */
  public String advance(String runId) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  /** Fails a run, then returns the freshly rendered dashboard. */
  public String fail(String runId) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  /** No mutation — just re-renders the current state. */
  public String refresh() {
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

Notice every method returns the rendered String rather than printing it — that keeps
`DashboardController` itself testable without capturing stdout; *printing* the result
is `Main`'s job (milestone 4).

## Step 2 — tests (copy-paste, must pass once step 1 is done)

`src/test/java/com/isaqb/practice/mvcfamily/DashboardControllerTest.java`:

```java
package com.isaqb.practice.mvcfamily;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DashboardControllerTest {

  private final DashboardController controller =
      new DashboardController(new PipelineRunModel(), new PipelineDashboardView());

  @Test
  void addRunThenAdvanceIsReflectedInTheNextRender() {
    controller.addRun("run-1", 2);

    var rendered = controller.advance("run-1");

    assertTrue(rendered.contains("run-1"));
    assertTrue(rendered.contains("1/2"));
  }

  @Test
  void failPropagatesModelExceptionsUnchanged() {
    // No run added yet — failing an unknown run should still surface UnknownRunException,
    // proving the Controller doesn't swallow or re-wrap Model errors.
    assertThrows(UnknownRunException.class, () -> controller.fail("nope"));
  }

  @Test
  void refreshRendersCurrentStateWithoutMutating() {
    controller.addRun("run-1", 3);

    var before = controller.refresh();
    var after = controller.refresh();

    assertTrue(before.equals(after));
  }
}
```

## Checkpoint

```bash
mvn -f patterns/10-mvc-family/pom.xml clean verify
```

All `DashboardControllerTest` cases pass. You can explain, in one sentence, why
`DashboardController` deliberately does *not* catch `UnknownRunException` /
`InvalidTransitionException` itself.

Next: [`04-cli-input-loop.md`](04-cli-input-loop.md).
