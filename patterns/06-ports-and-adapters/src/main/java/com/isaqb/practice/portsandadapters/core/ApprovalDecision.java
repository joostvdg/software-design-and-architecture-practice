package com.isaqb.practice.portsandadapters.core;

import java.time.Instant;
import java.util.Objects;

/**
 * The outcome the core produced for one ApprovalRequest: whether the deployment is
 * actually approved, and why. `reason` is either the approver's own justification (if
 * the request was policy-compliant) or an explanation of which policy rule failed (if
 * it wasn't) - see ApprovalService in milestone 3.
 */
public record ApprovalDecision(
        String requester,
        String approver,
        String namespace,
        boolean approved,
        String reason,
        Instant decidedAt
) {

    public ApprovalDecision {
        Objects.requireNonNull(requester, "requester");
        Objects.requireNonNull(approver, "approver");
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(decidedAt, "decidedAt");
    }
}
