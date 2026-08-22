package com.isaqb.practice.portsandadapters.adapter.driving.memory;

import com.isaqb.practice.portsandadapters.core.ApprovalDecision;
import com.isaqb.practice.portsandadapters.core.port.ApprovalRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The simplest possible ApprovalRepository: an in-process list. Good enough for the
 * CLI and for tests. A platform engineer can later swap this for FileApprovalRepository
 * (milestone 6) or a real database without ApprovalService, or either port, changing
 * by a single line.
 */
public class InMemoryApprovalRepository implements ApprovalRepository {

    private final List<ApprovalDecision> decisions = new CopyOnWriteArrayList<>();

    @Override
    public void save(ApprovalDecision decision) {
        // TODO: store `decision`.

        if (decision != null && !decisions.contains(decision)) {
            decisions.add(decision);
        }
    }

    @Override
    public List<ApprovalDecision> findByRequester(String requester) {
        // TODO: return every stored decision whose requester() equals `requester`, in the
        // order they were saved. Return an empty list (never null) if there are none.
        List<ApprovalDecision> foundDecisions = new ArrayList<>();
        for (ApprovalDecision decision : decisions) {
            if (decision.requester().trim().equals(requester.trim())) {
                foundDecisions.add(decision);
            }
        }
        return foundDecisions;
    }
}
