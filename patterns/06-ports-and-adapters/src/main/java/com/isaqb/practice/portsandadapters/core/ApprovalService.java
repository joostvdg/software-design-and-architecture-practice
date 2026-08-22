package com.isaqb.practice.portsandadapters.core;

import com.isaqb.practice.portsandadapters.core.port.ApprovalRepository;
import com.isaqb.practice.portsandadapters.core.port.RequestApprovalUseCase;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The core's implementation of the driving port. Reachable from outside only through
 * RequestApprovalUseCase, and reaches outside only through ApprovalRepository - never
 * naming a concrete adapter class. This is where "ports and adapters" earns its name:
 * everything this class touches is either another core type or an interface core
 * itself declared.
 */
public class ApprovalService implements RequestApprovalUseCase {

    private final ApprovalPolicy policy;
    private final ApprovalRepository repository;
    private final Clock clock;

    public ApprovalService(ApprovalPolicy policy, ApprovalRepository repository) {
        this(policy, repository, Clock.systemUTC());
    }

    public ApprovalService(ApprovalPolicy policy, ApprovalRepository repository, Clock clock) {
        this.policy = policy;
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public ApprovalDecision decide(ApprovalRequest request) {
        // TODO:
        //  1. Ask `policy.violations(request)`.
        //  2. If the list is non-empty, build an ApprovalDecision with approved=false and
        //     a `reason` that joins every violation into one string, e.g. with
        //     String.join("; ", violations) - the request is denied regardless of what
        //     the approver intended.
        //  3. If the list is empty, build an ApprovalDecision that reflects the request's
        //     own approve() flag as `approved`, and justification() as `reason`.
        //  4. Either way, stamp decidedAt with Instant.now(clock).
        //  5. Save the decision via repository.save(...) before returning it - the use
        //     case's job includes persisting its own outcome, not just computing it.

        List<String> violations = policy.violations(request);
        var decidedAt = Instant.now(clock);
        ApprovalDecision approvalDecision;
        if (violations.isEmpty()) {
            approvalDecision = new ApprovalDecision(
                    request.requester(),
                    request.approver(),
                    request.namespace(),
                    request.approve(),
                    request.justification(),
                    decidedAt);
        } else {
            String reason = violations.stream().collect(Collectors.joining(";"));
            approvalDecision = new ApprovalDecision(
                    request.requester(),
                    request.approver(),
                    request.namespace(),
                    false,
                    reason,
                    decidedAt
            );
        }

        repository.save(approvalDecision);
        return approvalDecision;
    }
}
