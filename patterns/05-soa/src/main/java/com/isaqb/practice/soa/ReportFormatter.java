package com.isaqb.practice.soa;

import com.isaqb.practice.soa.contract.ApprovalResponse;
import com.isaqb.practice.soa.contract.ProvisioningResponse;

/** Turns a decision (and, if any, a provisioning result) into one human-readable report. */
public final class ReportFormatter {

    private ReportFormatter() {}

    /**
     * @param provisioning may be null - callers only attempt provisioning when the
     *     deployment was approved, so a rejected deployment never has one.
     */
    public static String format(
            String deploymentId, ApprovalResponse approval, ProvisioningResponse provisioning) {
        // TODO:
        //  - First line: "deployment <deploymentId>: APPROVED (<reason>)" or
        //    "deployment <deploymentId>: REJECTED (<reason>)", matching approval.approved().
        //  - If provisioning is null, add a second line saying the environment was not
        //    provisioned (because the deployment was rejected).
        //  - If provisioning is non-null and reserved(), add a second line naming the
        //    reserved environmentId.
        //  - If provisioning is non-null and NOT reserved(), add a second line with its
        //    message explaining why.
        //  Join the lines with "\n". Exact wording is up to you; the tests below only check
        //  for specific substrings, the same way MainTest did in 01-layers.

        String approvalNote = "REJECTED";
        if (approval != null && approval.approved() ) {
            approvalNote = "APPROVED";
        }
        String reasonNote = "N/A";
        if (approval != null) {
            reasonNote = approval.reason();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("deployment ").append(deploymentId).append(": ").append(approvalNote).append("(").append(reasonNote).append(")").append("\n");
        if (provisioning == null) {
            sb.append("the environment is not provisioned, likely because the deployment was rejected");
        } else if (provisioning.reserved()) {
            sb.append("the environment is provisioned with environmentId: ").append(provisioning.environmentId());
        } else {
            sb.append("the environment is not provisioned, because the request was rejected: ").append(provisioning.message());
        }
        sb.append("\n");
        var report = sb.toString();
        System.out.println(report);
        return report;
    }
}
