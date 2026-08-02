# Milestone 4 — CLI input loop, and contrasting MVVM/MVU/PAC

## Goal

Build `Main`: the driving adapter that reads commands from stdin and dispatches them
to `DashboardController`, printing whatever it returns. This is the last piece of
classic MVC — and the milestone where you write down, in your own words, what would
be different if this were MVVM, MVU, or PAC instead, since the exam expects you to
contrast these, not just define MVC.

## Step 1 — `Main` (copy-paste, then run it)

`src/main/java/com/isaqb/practice/mvcfamily/Main.java`:

```java
package com.isaqb.practice.mvcfamily;

import java.util.Scanner;

/** CLI driving adapter. Reads "command arg" lines from stdin until "quit". */
public final class Main {

  public static void main(String[] args) {
    var controller = new DashboardController(new PipelineRunModel(), new PipelineDashboardView());
    controller.addRun("run-1", 4);
    controller.addRun("run-2", 3);

    System.out.println(controller.refresh());
    System.out.println("Commands: advance <id> | fail <id> | refresh | quit");

    try (var scanner = new Scanner(System.in)) {
      while (scanner.hasNextLine()) {
        var line = scanner.nextLine().trim();
        if (line.equals("quit")) {
          break;
        }
        System.out.println(dispatch(controller, line));
      }
    }
  }

  private static String dispatch(DashboardController controller, String line) {
    var parts = line.split("\\s+", 2);
    var command = parts[0];
    try {
      return switch (command) {
        case "advance" -> controller.advance(parts[1]);
        case "fail" -> controller.fail(parts[1]);
        case "refresh" -> controller.refresh();
        default -> "unknown command: " + command;
      };
    } catch (UnknownRunException | InvalidTransitionException e) {
      return "error: " + e.getMessage();
    }
  }

  private Main() {}
}
```

Run it and try a few commands (`advance run-1`, `fail run-2`, `refresh`, `quit`):

```bash
mvn -f patterns/10-mvc-family/pom.xml -q compile exec:java -Dexec.mainClass=com.isaqb.practice.mvcfamily.Main
```

(If the `exec` goal isn't available locally, `mvn package` then
`java -cp target/classes com.isaqb.practice.mvcfamily.Main` works the same way.)

Notice: `Main` is the only class that catches `UnknownRunException` /
`InvalidTransitionException` — the Controller lets them propagate (milestone 3), and
this driving adapter is where "how do we show an error to this particular kind of
user" belongs, since a future HTTP adapter would want to turn the same exceptions
into a 400 response instead of a printed line.

## Step 2 — write the contrast (a short doc, not code)

Add a `## MVVM / MVU / PAC, contrasted` section to the **bottom** of this repo's
`README.md` for this module (append to `patterns/10-mvc-family/README.md`, don't
create a new file) covering, in your own words, for *this exact case study*:

- **MVVM:** what would replace the explicit `view.render(model)` calls in
  `DashboardController`? (Hint: a `PipelineRunViewModel` exposing observable
  properties like `stagesCompleted`, with the View bound to it declaratively — no
  method here is called "render".)
- **MVU:** what would `PipelineRunModel.advanceStage` become if there were no
  mutation at all? (Hint: a pure `update(model, AdvanceStage(runId)) -> newModel`
  function; `Main`'s loop would hold "the current model" itself instead of a
  mutable `PipelineRunModel` instance.)
- **PAC:** if the dashboard grew a second, semi-independent widget (say, a filter
  panel), how would PAC structure that differently than one flat
  `DashboardController`? (Hint: two small MVC-ish agents, each with its own
  Model/View/Controller, composed under a parent agent that coordinates them —
  contrast with this module's single flat triad.)

This isn't graded by a test — it's the artifact that proves you can *explain* the
variants, which is what CPSA-F actually asks for (section 1 of the README already
gives you the vocabulary; this is where you apply it to a concrete case).

## Checkpoint

- [ ] `mvn -f patterns/10-mvc-family/pom.xml clean verify` is green.
- [ ] Running `Main` and typing `advance run-1` then `refresh` shows `1/4` for
      `run-1`.
- [ ] `README.md` has a new `## MVVM / MVU / PAC, contrasted` section answering the
      three prompts above for this case study specifically (not the generic
      definitions already in section 1).

Next: [`05-build-and-release.md`](05-build-and-release.md).
