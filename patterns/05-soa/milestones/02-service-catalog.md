# Milestone 2 — Service Catalog

## Goal

Build the `ServiceCatalog`: the governance/discovery mechanism a consumer uses to
obtain a service **by its stable `(name, version)` contract key**, instead of
constructing (or even naming) the concrete implementation class. This is the core
mechanic of the whole pattern — everything else in this module exists to be registered
in, or looked up from, this one class.

Delete `src/test/java/com/isaqb/practice/soa/SmokeTest.java` now — the tests you add in
this milestone replace it as your "is the build green" signal.

## Step 1 — the key and the not-found exception (copy-paste)

`src/main/java/com/isaqb/practice/soa/catalog/ServiceKey.java`:

```java
package com.isaqb.practice.soa.catalog;

/** The catalog's lookup key: a service is identified by name AND version, not name alone. */
record ServiceKey(String name, String version) {}
```

`src/main/java/com/isaqb/practice/soa/catalog/ServiceNotFoundException.java`:

```java
package com.isaqb.practice.soa.catalog;

/** Thrown when a consumer looks up a (name, version) the catalog has nothing registered under. */
public class ServiceNotFoundException extends RuntimeException {

  public ServiceNotFoundException(String message) {
    super(message);
  }
}
```

`ServiceKey` is a record, so it gets structurally-correct `equals`/`hashCode` for free —
that's what makes it safe to use as a `HashMap` key below.

## Step 2 — the catalog itself (write this yourself)

`src/main/java/com/isaqb/practice/soa/catalog/ServiceCatalog.java`:

```java
package com.isaqb.practice.soa.catalog;

import java.util.HashMap;
import java.util.Map;

/**
 * PipelineForge's central place to register and discover service implementations by a
 * stable (name, version) contract key. Callers never construct a service directly - they
 * ask the catalog for e.g. "deployment-approval v1" and get back whatever's registered
 * under that key. That indirection is what lets multiple, unrelated callers reuse the
 * same service instance without depending on its concrete class - the whole point of SOA's
 * contract-first reuse, exercised here without any networking involved.
 */
public final class ServiceCatalog {

  private final Map<ServiceKey, Object> services = new HashMap<>();

  /**
   * Registers {@code implementation} under the given name and version, checked against
   * {@code contract}.
   *
   * @throws IllegalStateException if something is already registered under this exact
   *     (name, version) - registering the same key twice is almost always a bug (e.g. two
   *     different implementations of "v1" accidentally both wired up), so this must fail
   *     loudly rather than silently overwrite.
   */
  public <T> void register(String name, String version, Class<T> contract, T implementation) {
    // TODO:
    //  1. Build a ServiceKey from name and version.
    //  2. If `services` already has an entry for that key, throw IllegalStateException
    //     with a message that names the offending (name, version).
    //  3. Otherwise store `implementation` under that key. `contract.cast(implementation)`
    //     is a good habit here even though the compiler already guarantees the type -
    //     it documents that `contract` is meaningful, not just a formality.
    throw new UnsupportedOperationException("not implemented yet");
  }

  /**
   * Looks up whatever was registered under (name, version), cast to {@code contract}.
   *
   * @throws ServiceNotFoundException if nothing is registered under that exact
   *     (name, version).
   */
  public <T> T lookup(String name, String version, Class<T> contract) {
    // TODO:
    //  1. Build a ServiceKey from name and version.
    //  2. Look it up in `services`.
    //  3. If absent, throw ServiceNotFoundException with a message that names the
    //     missing (name, version) - this is the failure a consumer sees when the
    //     catalog was never populated, or they typo'd the version string.
    //  4. If present, return it cast to `contract`. `contract.cast(...)` does this
    //     safely and gives a clear ClassCastException message if it's ever wrong,
    //     instead of a confusing one further downstream.
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

This is the "typesafe heterogeneous container" idiom: the catalog stores plain
`Object`s internally (it has no idea what a `DeploymentApprovalService` is, and
shouldn't), but every caller-facing method is generic in `T`, so callers of `lookup`
never see an `Object` or a manual cast.

## Step 3 — tests (copy-paste, must pass once step 2 is done)

`src/test/java/com/isaqb/practice/soa/catalog/ServiceCatalogTest.java`:

```java
package com.isaqb.practice.soa.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ServiceCatalogTest {

  interface Greeter {
    String greet();
  }

  @Test
  void lookupReturnsWhatWasRegistered() {
    ServiceCatalog catalog = new ServiceCatalog();
    Greeter greeter = () -> "hi";

    catalog.register("greeter", "v1", Greeter.class, greeter);

    assertEquals("hi", catalog.lookup("greeter", "v1", Greeter.class).greet());
  }

  @Test
  void lookupThrowsWhenNothingRegisteredUnderKey() {
    ServiceCatalog catalog = new ServiceCatalog();

    assertThrows(
        ServiceNotFoundException.class, () -> catalog.lookup("missing", "v1", Greeter.class));
  }

  @Test
  void differentVersionsOfSameNameAreDistinctEntries() {
    ServiceCatalog catalog = new ServiceCatalog();
    catalog.register("greeter", "v1", Greeter.class, () -> "v1-hi");
    catalog.register("greeter", "v2", Greeter.class, () -> "v2-hi");

    assertEquals("v1-hi", catalog.lookup("greeter", "v1", Greeter.class).greet());
    assertEquals("v2-hi", catalog.lookup("greeter", "v2", Greeter.class).greet());
  }

  @Test
  void registeringSameKeyTwiceThrows() {
    ServiceCatalog catalog = new ServiceCatalog();
    catalog.register("greeter", "v1", Greeter.class, () -> "hi");

    assertThrows(
        IllegalStateException.class,
        () -> catalog.register("greeter", "v1", Greeter.class, () -> "again"));
  }
}
```

Notice the test never mentions `DeploymentApprovalService` or any real PipelineForge
type — it proves the catalog's behavior in complete isolation from the case study,
using a throwaway `Greeter` contract. That's the payoff of building the catalog before
any real service: it's testable, and provably correct, on its own.

## Checkpoint

```bash
mvn -f patterns/05-soa/pom.xml clean verify
```

All four `ServiceCatalogTest` cases pass. You can explain, in one sentence, why
`register` throwing on a duplicate key matters more in a catalog serving many unrelated
consumers than it would in a single `new SomeService()` call.

Next: [`03-deployment-approval-service.md`](03-deployment-approval-service.md).
