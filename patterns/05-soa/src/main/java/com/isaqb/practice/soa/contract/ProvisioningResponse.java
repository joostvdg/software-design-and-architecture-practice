package com.isaqb.practice.soa.contract;

/** The Environment Provisioning Service's result: what got reserved, and under what id. */
public record ProvisioningResponse(boolean reserved, String environmentId, String message) {

    public static ProvisioningResponse reserved(String environmentId, String message) {
        return new  ProvisioningResponse(true, environmentId, message);
    }

    public static ProvisioningResponse rejected(String message) {
        return new  ProvisioningResponse(false, "", message);
    }
}
