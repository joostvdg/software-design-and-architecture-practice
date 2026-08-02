# Milestone 4 — Environment Provisioning Service

## Goal

Implement `EnvironmentProvisioningServiceImpl`: the business logic behind the
Environment Provisioning Service's contract. Same shape as milestone 3 - plain domain
logic, no catalog, no callers, fully testable on its own.

## Step 1 — the class shell (write the body yourself)

`src/main/java/com/isaqb/practice/soa/service/EnvironmentProvisioningServiceImpl.java`:

```java
package com.isaqb.practice.soa.service;

import com.isaqb.practice.soa.contract.EnvironmentProvisioningService;
import com.isaqb.practice.soa.contract.ProvisioningRequest;
import com.isaqb.practice.soa.contract.ProvisioningResponse;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** PipelineForge's environment provisioning policy: the one implementation of the contract. */
public class EnvironmentProvisioningServiceImpl implements EnvironmentProvisioningService {

  private static final Set<String> KNOWN_ENVIRONMENTS = Set.of("staging", "qa", "production");

  @Override
  public ProvisioningResponse provision(ProvisioningRequest request) {
    // TODO: implement PipelineForge's provisioning policy:
    //  1. Normalize request.environmentName(): trim it and lower-case it (Locale.ROOT).
    //  2. If the normalized name is NOT in KNOWN_ENVIRONMENTS, return
    //     ProvisioningResponse.rejected(...) with a message naming the unknown
    //     environment.
    //  3. Otherwise, generate a fresh environment id - normalizedName + "-" +
    //     UUID.randomUUID() is enough - and return
    //     ProvisioningResponse.reserved(environmentId, message) with a message that
    //     mentions request.requestedBy().
    throw new UnsupportedOperationException("not implemented yet");
  }
}
```

## Step 2 — tests (copy-paste, must pass once step 1 is done)

`src/test/java/com/isaqb/practice/soa/service/EnvironmentProvisioningServiceImplTest.java`:

```java
package com.isaqb.practice.soa.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.isaqb.practice.soa.contract.ProvisioningRequest;
import org.junit.jupiter.api.Test;

class EnvironmentProvisioningServiceImplTest {

  private final EnvironmentProvisioningServiceImpl service =
      new EnvironmentProvisioningServiceImpl();

  @Test
  void reservesKnownEnvironment() {
    var response = service.provision(new ProvisioningRequest("staging", "platform-engineer"));

    assertTrue(response.reserved());
    assertTrue(response.environmentId().startsWith("staging-"));
  }

  @Test
  void rejectsUnknownEnvironment() {
    var response =
        service.provision(new ProvisioningRequest("does-not-exist", "platform-engineer"));

    assertFalse(response.reserved());
  }

  @Test
  void environmentNameIsTrimmedAndLowerCased() {
    var response = service.provision(new ProvisioningRequest("  STAGING  ", "platform-engineer"));

    assertTrue(response.reserved());
    assertTrue(response.environmentId().startsWith("staging-"));
  }

  @Test
  void twoReservationsOfTheSameEnvironmentGetDifferentIds() {
    var first = service.provision(new ProvisioningRequest("qa", "platform-engineer"));
    var second = service.provision(new ProvisioningRequest("qa", "platform-engineer"));

    assertTrue(first.reserved());
    assertTrue(second.reserved());
    assertFalse(first.environmentId().equals(second.environmentId()));
  }
}
```

## Checkpoint

```bash
mvn -f patterns/05-soa/pom.xml clean verify
```

All four `EnvironmentProvisioningServiceImplTest` cases pass. Both services now exist
and are fully tested, but still nothing in the module actually calls them through the
catalog yet - that's the next (and final code) milestone.

Next: [`05-callers-and-composition-root.md`](05-callers-and-composition-root.md).
