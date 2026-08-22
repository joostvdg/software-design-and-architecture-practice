package com.isaqb.practice.portsandadapters.core;

import java.util.ArrayList;
import java.util.List;

public class DefaultApprovalPolicy implements ApprovalPolicy {
    @Override
    public List<String> violations(ApprovalRequest request) {
        // TODO: return a list of human-readable violation messages, one per failing rule
        // below; return an empty list if every rule passes. Rules:
        //  1. requester must not be blank (null-safe: use request.requester(), which is
        //     never null thanks to the record's compact constructor, but it can be "" or
        //     all-whitespace).
        //  2. approver must not be blank.
        //  3. namespace must not be blank.
        //  4. justification must not be blank.
        //  5. requester and approver must not name the same person - compare
        //     case-insensitively, after trimming whitespace, so "alice" and " Alice "
        //     count as the same person. Nobody approves their own deployment.
        // Check every rule and collect every violation - don't stop at the first one, so
        // a caller sees the full picture in one pass. Hint: java.util.ArrayList<String>.
        var violations = new ArrayList<String>();

        if (request.requester().trim().isEmpty()) {
            violations.add("You must provide at least one requester");
        }

        if (request.approver().trim().isEmpty()) {
            violations.add("You must provide at least one approver");
        }

        if (request.namespace().trim().isEmpty()) {
            violations.add("You must provide at least one namespace");
        }

        if (request.justification().trim().isEmpty()) {
            violations.add("You must provide at least one justification");
        }

        if (request.approver().trim().equalsIgnoreCase(request.requester().trim())) {
            violations.add("Approver and requester cannot be the same");
        }


        return violations;
    }
}
