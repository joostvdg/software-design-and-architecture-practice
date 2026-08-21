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