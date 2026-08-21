package com.isaqb.practice.soa.contract;

/** A request to reserve a target environment for a deployment. */
public record ProvisioningRequest(String environmentName, String requestedBy) {
}
