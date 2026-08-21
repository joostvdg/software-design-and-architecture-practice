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

        String normalizedEnvironmentName = request.environmentName().trim().toLowerCase(Locale.ROOT);
        if (!KNOWN_ENVIRONMENTS.contains(normalizedEnvironmentName)) {
            return ProvisioningResponse.rejected("unknown environment: " + normalizedEnvironmentName);
        }
        String environmentId = String.format("%s-%s", normalizedEnvironmentName, UUID.randomUUID());
        return ProvisioningResponse.reserved(environmentId, String.format("Environment %s requested by %s", normalizedEnvironmentName, request.requestedBy()));
    }
}
