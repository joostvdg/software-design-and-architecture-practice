package com.isaqb.practice.portsandadapters.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DefaultApprovalPolicyTest {

    private final DefaultApprovalPolicy policy = new DefaultApprovalPolicy();

    @Test
    void passesForAWellFormedRequest() {
        var request =
                new ApprovalRequest("alice", "bob", "payments-prod", "on-call approved", true);

        assertTrue(policy.violations(request).isEmpty());
    }

    @Test
    void rejectsSelfApproval() {
        var request = new ApprovalRequest("alice", "Alice ", "payments-prod", "looks fine", true);

        assertTrue(!policy.violations(request).isEmpty());
    }

    @Test
    void rejectsBlankJustification() {
        var request = new ApprovalRequest("alice", "bob", "payments-prod", "   ", true);

        assertTrue(!policy.violations(request).isEmpty());
    }

    @Test
    void rejectsBlankNamespace() {
        var request = new ApprovalRequest("alice", "bob", " ", "on-call approved", true);

        assertTrue(!policy.violations(request).isEmpty());
    }

    @Test
    void collectsMultipleViolationsAtOnce() {
        // All four blank-field rules fire; self-approval does not (both sides are blank,
        // so there's no named requester/approver to be "the same person" as - the blank
        // checks above already cover this case).
        var request = new ApprovalRequest(" ", " ", " ", " ", true);

        assertEquals(5, policy.violations(request).size());
    }
}