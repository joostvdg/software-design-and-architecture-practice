# Milestone 2 — The View

## Goal

Build `PipelineDashboardView`: a class that turns the Model's current state into a
text table. It only *reads* `PipelineRunModel` (via `allRuns()`), never calls a
mutating method, and has no idea a Controller or CLI exists.

## Step 1 — `PipelineDashboardView` (write the formatting logic yourself)

Create `src/main/java/com/isaqb/practice/mvcfamily/PipelineDashboardView.java`:

```java
package com.isaqb.practice.mvcfamily;

/** Renders a PipelineRunModel's current state as a text table. Read-only — never mutates. */
public class PipelineDashboardView {

  /**
   * Renders every run as one row: {@code "<id>  <status>  <completed>/<total>"},
   * padded so columns line up, preceded by a header row {@code "ID  STATUS  PROGRESS"}.
   * Exact column widths are up to you — the test below only checks content, not
   * whitespace — but every row must contain the run's id, status name, and
   * "completed/total" progress, in that order.
   */
  public String render(PipelineRunModel model) {
    // TODO: build the header line, then one line per model.allRuns() entry.
    // Join with "\n". Use PipelineRun#id/status/stagesCompleted/stagesTotal.
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

## Step 2 — tests (copy-paste, must pass once step 1 is done)

`src/test/java/com/isaqb/practice/mvcfamily/PipelineDashboardViewTest.java`:

```java
package com.isaqb.practice.mvcfamily;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PipelineDashboardViewTest {

  private final PipelineDashboardView view = new PipelineDashboardView();

  @Test
  void rendersOneRowPerRunWithIdStatusAndProgress() {
    var model = new PipelineRunModel();
    model.addRun("run-1", 4);
    model.advanceStage("run-1");
    model.advanceStage("run-1");

    var rendered = view.render(model);

    assertTrue(rendered.contains("run-1"));
    assertTrue(rendered.contains("RUNNING"));
    assertTrue(rendered.contains("2/4"));
  }

  @Test
  void rendersEmptyModelWithoutThrowing() {
    var rendered = view.render(new PipelineRunModel());

    assertTrue(rendered.contains("ID"));
  }
}
```

## Checkpoint

```bash
mvn -f patterns/10-mvc-family/pom.xml clean verify
```

Both `PipelineDashboardViewTest` cases pass. Confirm `PipelineDashboardView.java` has
no method whose name suggests mutation (`advance`, `fail`, `add`) — if you found
yourself wanting one, that logic belongs in the Model, not the View.

Next: [`03-controller.md`](03-controller.md).
