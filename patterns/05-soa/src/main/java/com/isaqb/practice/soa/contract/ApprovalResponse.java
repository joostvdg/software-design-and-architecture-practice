package com.isaqb.practice.soa.contract;

/** The Deployment Approval Service's decision, and why it decided that. */
public record ApprovalResponse(boolean approved, String reason) {

    public static ApprovalResponse approved(String reason) {
        return new ApprovalResponse(true, reason);
    }

    public static ApprovalResponse rejected(String reason) {
        return new ApprovalResponse(false, reason);
    }
}
