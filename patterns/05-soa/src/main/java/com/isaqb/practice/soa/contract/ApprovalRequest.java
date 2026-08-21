package com.isaqb.practice.soa.contract;

/** A request to decide whether a deployment may proceed. */
public record ApprovalRequest(
        String deploymentId, String environment, String requestedBy, RiskLevel riskLevel
) {
}
