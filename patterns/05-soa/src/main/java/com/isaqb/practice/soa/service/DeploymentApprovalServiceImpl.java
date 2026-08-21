package com.isaqb.practice.soa.service;

import com.isaqb.practice.soa.contract.ApprovalRequest;
import com.isaqb.practice.soa.contract.ApprovalResponse;
import com.isaqb.practice.soa.contract.DeploymentApprovalService;

/** PipelineForge's approval policy: the one implementation of DeploymentApprovalService. */
public class DeploymentApprovalServiceImpl implements DeploymentApprovalService {

    private static final String REQUIRED_APPROVER_FOR_HIGH_RISK = "release-manager";

    @Override
    public ApprovalResponse decide(ApprovalRequest request) {
        // TODO: implement PipelineForge's approval policy:
        //  - LOW or MEDIUM risk: always approved.
        //  - HIGH risk requested by exactly REQUIRED_APPROVER_FOR_HIGH_RISK: approved.
        //  - HIGH risk requested by anyone else: rejected, with a reason that says
        //    high-risk deployments require release-manager approval.
        // Use ApprovalResponse.approved(reason) / ApprovalResponse.rejected(reason) - both
        // already exist on the record from milestone 1.

        return switch (request.riskLevel()) {
            case LOW -> ApprovalResponse.approved("Low risk");
            case MEDIUM ->  ApprovalResponse.approved("Medium risk");
            case HIGH -> {
                if(request.requestedBy().equalsIgnoreCase(REQUIRED_APPROVER_FOR_HIGH_RISK)) {
                    yield ApprovalResponse.approved("High risk by release-manager");
                } else {
                    yield ApprovalResponse.rejected("High risk not by release-manager") ;
                }

            }
        };
    }
}
