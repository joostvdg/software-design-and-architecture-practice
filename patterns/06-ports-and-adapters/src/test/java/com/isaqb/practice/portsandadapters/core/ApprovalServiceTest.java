package com.isaqb.practice.portsandadapters.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.isaqb.practice.portsandadapters.core.port.ApprovalRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApprovalServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-08-01T09:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    // A fake driven adapter, written right here in the test - this is the payoff of
    // depending on a port instead of a concrete class: no InMemoryApprovalRepository
    // needs to exist yet for this test to run.
    private static final class RecordingRepository implements ApprovalRepository {
        final List<ApprovalDecision> saved = new ArrayList<>();

        @Override
        public void save(ApprovalDecision decision) {
            saved.add(decision);
        }

        @Override
        public List<ApprovalDecision> findByRequester(String requester) {
            throw new UnsupportedOperationException("not needed for this test");
        }
    }

    @Test
    void approvesAWellFormedRequestAndPersistsIt() {
        var repository = new RecordingRepository();
        var service = new ApprovalService(new DefaultApprovalPolicy(), repository, FIXED_CLOCK);
        var request =
                new ApprovalRequest("alice", "bob", "payments-prod", "on-call approved", true);

        ApprovalDecision decision = service.decide(request);

        assertTrue(decision.approved());
        assertEquals("on-call approved", decision.reason());
        assertEquals(FIXED_NOW, decision.decidedAt());
        assertEquals(1, repository.saved.size());
        assertEquals(decision, repository.saved.get(0));
    }

    @Test
    void denyingApproverStillProducesADenial() {
        var repository = new RecordingRepository();
        var service = new ApprovalService(new DefaultApprovalPolicy(), repository, FIXED_CLOCK);
        var request =
                new ApprovalRequest("alice", "bob", "payments-prod", "not ready yet", false);

        ApprovalDecision decision = service.decide(request);

        assertFalse(decision.approved());
        assertEquals("not ready yet", decision.reason());
    }

    @Test
    void policyViolationOverridesApproverIntent() {
        var repository = new RecordingRepository();
        var service = new ApprovalService(new DefaultApprovalPolicy(), repository, FIXED_CLOCK);
        // approve=true, but this is self-approval - policy must win.
        var request = new ApprovalRequest("alice", "alice", "payments-prod", "seems fine", true);

        ApprovalDecision decision = service.decide(request);

        assertFalse(decision.approved());
        assertEquals(1, repository.saved.size());
    }
}