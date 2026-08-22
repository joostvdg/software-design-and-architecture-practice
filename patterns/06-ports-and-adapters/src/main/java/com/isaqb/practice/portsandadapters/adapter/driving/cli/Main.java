package com.isaqb.practice.portsandadapters.adapter.driving.cli;

import com.isaqb.practice.portsandadapters.adapter.driving.memory.InMemoryApprovalRepository;
import com.isaqb.practice.portsandadapters.core.ApprovalDecision;
import com.isaqb.practice.portsandadapters.core.ApprovalRequest;
import com.isaqb.practice.portsandadapters.core.ApprovalService;
import com.isaqb.practice.portsandadapters.core.DefaultApprovalPolicy;
import com.isaqb.practice.portsandadapters.core.port.RequestApprovalUseCase;

/**
 * Composition root and driving adapter in one: the only class allowed to know about
 * every concrete class in the module at once. Wires a driven adapter
 * (InMemoryApprovalRepository) and the core's own policy into the core's use case,
 * then drives that use case from the command line.
 */
public final class Main {


    private Main() {}

    public static void main(final String[] args) {
        if (args.length != 5) {
            System.err.println(
                    "usage: ports-and-adapters <requester> <approver> <namespace> <true|false> <justification>");
            System.exit(2);
            return;
        }

        RequestApprovalUseCase useCase = new ApprovalService(
                new DefaultApprovalPolicy(),
                new InMemoryApprovalRepository()
        );
        var request = new ApprovalRequest(args[0], args[1], args[2], args[4], Boolean.parseBoolean(args[3]));

        ApprovalDecision decision = useCase.decide(request);
        System.out.println(formatDecision(decision));
        System.exit(decision.approved() ? 0 : 1);
    }

    static String formatDecision(ApprovalDecision decision) {
        String decisionMessage = "";
        if (decision.approved()) {
            decisionMessage = String.format("APPROVED: %s for %s, approved by %s- %s",
                decision.namespace(),
                decision.requester(),
                decision.approver(),
                decision.reason());
        } else {
            decisionMessage = String.format("DENIED: %s for %s - %s",
                    decision.namespace(),
                    decision.requester(),
                    decision.reason());
        }

        return decisionMessage;
    }
}
