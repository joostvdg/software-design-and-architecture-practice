package com.isaqb.practice.soa;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.isaqb.practice.soa.contract.ApprovalResponse;
import com.isaqb.practice.soa.contract.ProvisioningResponse;
import org.junit.jupiter.api.Test;

class ReportFormatterTest {

    @Test
    void formatsApprovedAndReservedReport() {
        var approval = ApprovalResponse.approved("looks fine");
        var provisioning = ProvisioningResponse.reserved("staging-abc123", "environment reserved");

        String report = ReportFormatter.format("dep-1", approval, provisioning);

        assertTrue(report.contains("dep-1"));
        assertTrue(report.toUpperCase().contains("APPROVED"));
        assertTrue(report.contains("staging-abc123"));
    }

    @Test
    void formatsRejectedReportWithoutProvisioning() {
        var approval =
                ApprovalResponse.rejected("high-risk deployments require release-manager approval");

        String report = ReportFormatter.format("dep-2", approval, null);

        assertTrue(report.toUpperCase().contains("REJECT"));
        assertTrue(report.contains("dep-2"));
    }

    @Test
    void formatsApprovedButNotReservedReport() {
        var approval = ApprovalResponse.approved("looks fine");
        var provisioning = ProvisioningResponse.rejected("unknown environment: nope");

        String report = ReportFormatter.format("dep-3", approval, provisioning);

        assertTrue(report.contains("dep-3"));
        assertTrue(report.contains("unknown environment"));
    }
}