package com.isaqb.practice.soa.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.isaqb.practice.soa.contract.ApprovalRequest;
import com.isaqb.practice.soa.contract.RiskLevel;
import org.junit.jupiter.api.Test;

class DeploymentApprovalServiceImplTest {

    private final DeploymentApprovalServiceImpl service = new DeploymentApprovalServiceImpl();

    @Test
    void lowRiskIsAlwaysApproved() {
        var response =
                service.decide(new ApprovalRequest("dep-1", "staging", "anyone", RiskLevel.LOW));

        assertTrue(response.approved());
    }

    @Test
    void mediumRiskIsAlwaysApproved() {
        var response =
                service.decide(new ApprovalRequest("dep-1", "staging", "anyone", RiskLevel.MEDIUM));

        assertTrue(response.approved());
    }

    @Test
    void highRiskByReleaseManagerIsApproved() {
        var response =
                service.decide(
                        new ApprovalRequest("dep-1", "production", "release-manager", RiskLevel.HIGH));

        assertTrue(response.approved());
    }

    @Test
    void highRiskByAnyoneElseIsRejected() {
        var response =
                service.decide(new ApprovalRequest("dep-1", "production", "ci-bot", RiskLevel.HIGH));

        assertFalse(response.approved());
        assertTrue(response.reason().toLowerCase().contains("release-manager"));
    }
}