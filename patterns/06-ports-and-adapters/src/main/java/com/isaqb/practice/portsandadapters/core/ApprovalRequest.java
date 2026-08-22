package com.isaqb.practice.portsandadapters.core;

import java.util.Objects;

/**
 * What's being asked: an approver's response to a requester's ask, before policy has
 * been applied. `approve` is the approver's stated intent - the core may still turn
 * this into a denial if the request violates policy (see ApprovalPolicy).
 */
public record ApprovalRequest(
        String requester, String approver, String namespace, String justification, boolean approve
) {

    public ApprovalRequest{
        Objects.requireNonNull(requester, "requester");
        Objects.requireNonNull(approver, "approver");
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(justification,  "justification");
    }
}
