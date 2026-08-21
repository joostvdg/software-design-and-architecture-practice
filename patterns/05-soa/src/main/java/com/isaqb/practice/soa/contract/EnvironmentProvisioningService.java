package com.isaqb.practice.soa.contract;

/** The Environment Provisioning Service's contract. Same rule as its sibling above. */
public interface EnvironmentProvisioningService {

    ProvisioningResponse provision(ProvisioningRequest request);
}
