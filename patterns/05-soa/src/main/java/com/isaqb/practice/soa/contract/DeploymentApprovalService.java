package com.isaqb.practice.soa.contract;

/**
 * The Deployment Approval Service's contract. Consumers depend on this interface only -
 * never on whichever class implements it, and never construct that class themselves.
 */
public interface DeploymentApprovalService {

    ApprovalResponse decide(ApprovalRequest request);
}
