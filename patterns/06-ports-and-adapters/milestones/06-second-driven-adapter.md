# Milestone 6 — A second driven adapter, proven interchangeable

## Goal

So far you've only had to *believe* that swapping a driven adapter wouldn't touch
`core`. This milestone makes you prove it: write `FileApprovalRepository`, a second,
genuinely different implementation of `ApprovalRepository` — backed by a flat file
instead of a list in memory — and run the *exact same* `ApprovalRepositoryContractTest`
from milestone 4 against it, completely unmodified. If both adapters pass the same
suite, and `core/` has zero new imports or changes, that's the proof: the port is the
only thing either adapter or `core` ever agreed on.

This is also the moment to imagine the platform engineer from the case study: they'd
make exactly this change — pick a different driven adapter — to move the Deployment
Approval Service from a quick local demo to something that survives a process restart,
without asking anyone who owns `core` to change a line.

## Step 1 — the adapter (write the two method bodies yourself)

`src/main/java/com/isaqb/practice/portsandadapters/adapter/driven/file/FileApprovalRepository.java`:

```java
package com.isaqb.practice.portsandadapters.adapter.driven.file;

import com.isaqb.practice.portsandadapters.core.ApprovalDecision;
import com.isaqb.practice.portsandadapters.core.port.ApprovalRepository;
import java.nio.file.Path;
import java.util.List;

/**
 * A second ApprovalRepository implementation, backed by a flat file instead of an
 * in-process list. Deliberately a different storage technology from
 * InMemoryApprovalRepository, to make the interchangeability claim concrete rather
 * than a decorator around the same data structure.
 */
public class FileApprovalRepository implements ApprovalRepository {

  private static final String DELIMITER = "|";

  private final Path file;

  public FileApprovalRepository(Path file) {
    this.file = file;
  }

  @Override
  public void save(ApprovalDecision decision) {
    // TODO: append one line to `file` (creating it if it doesn't exist yet -
    // Files.writeString(path, text, StandardOpenOption.CREATE,
    // StandardOpenOption.APPEND) does both at once), encoding `decision` as six
    // fields joined by DELIMITER, in this exact order: requester, approver,
    // namespace, approved (Boolean.toString), reason, decidedAt
    // (decision.decidedAt().toString()). End the line with "\n".
    // ApprovalRepository's contract declares no checked exceptions, so translate any
    // IOException into an UncheckedIOException - the same "adapter translates checked
    // I/O trouble into whatever its port promises" move FileConfigSource made for
    // ConfigLoadException in 01-layers, just landing on an unchecked exception here
    // because that's what *this* port promises.
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public List<ApprovalDecision> findByRequester(String requester) {
    // TODO:
    //  - if `file` does not exist yet, return an empty list (nobody has saved
    //    anything yet - a valid state, not an error).
    //  - otherwise read every line (Files.readAllLines), split each on DELIMITER
    //    (careful: "|" is a regex metacharacter - use Pattern.quote(DELIMITER), or
    //    the literal "\\|", as the split() argument), rebuild an ApprovalDecision per
    //    line (Boolean.parseBoolean, Instant.parse), and keep only the ones whose
    //    requester() equals `requester`, in file order.
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

## Step 2 — wire the same contract test to this adapter (copy-paste)

`src/test/java/com/isaqb/practice/portsandadapters/adapter/driven/file/FileApprovalRepositoryTest.java`:

```java
package com.isaqb.practice.portsandadapters.adapter.driven.file;

import com.isaqb.practice.portsandadapters.adapter.ApprovalRepositoryContractTest;
import com.isaqb.practice.portsandadapters.core.port.ApprovalRepository;
import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;

class FileApprovalRepositoryTest extends ApprovalRepositoryContractTest {

  @TempDir Path tempDir;

  @Override
  protected ApprovalRepository newRepository() {
    return new FileApprovalRepository(tempDir.resolve("decisions-" + System.nanoTime() + ".log"));
  }
}
```

This is character-for-character the same shape as `InMemoryApprovalRepositoryTest`
from milestone 4 — only `newRepository()`'s body differs. `ApprovalRepositoryContractTest`
itself was not touched, copied, or forked.

## Step 3 — confirm nothing in `core` changed

```bash
git status patterns/06-ports-and-adapters/src/main/java/com/isaqb/practice/portsandadapters/core
```

(If this repo isn't tracking your in-progress work in git yet, just re-read
`core/ApprovalService.java`, `core/ApprovalPolicy.java`, `core/DefaultApprovalPolicy.java`,
and `core/port/*.java` and confirm none of them mention `FileApprovalRepository`,
`java.nio.file`, or anything else you just wrote in `adapter/driven/file`.)

## Checkpoint

```bash
mvn -f patterns/06-ports-and-adapters/pom.xml clean verify
```

- [ ] All four inherited contract-test cases pass for **both**
      `InMemoryApprovalRepositoryTest` and `FileApprovalRepositoryTest`.
- [ ] `core/` has zero new imports since milestone 3 — grep it yourself:
      `grep -r "import com.isaqb.practice.portsandadapters.adapter" patterns/06-ports-and-adapters/src/main/java/com/isaqb/practice/portsandadapters/core`
      should print nothing.
- [ ] You can name the one line in `Main` (milestone 5) you'd change to make the CLI
      use `FileApprovalRepository` instead of `InMemoryApprovalRepository` — and
      confirm it's still just that one line, now that you've actually built the second
      adapter instead of imagining it.

Next: [`07-build-and-release.md`](07-build-and-release.md).
