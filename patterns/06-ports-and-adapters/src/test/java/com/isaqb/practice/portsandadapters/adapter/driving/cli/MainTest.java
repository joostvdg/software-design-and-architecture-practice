package com.isaqb.practice.portsandadapters.adapter.driving.cli;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.isaqb.practice.portsandadapters.core.ApprovalDecision;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MainTest {

    private static final Instant WHEN = Instant.parse("2026-08-01T09:00:00Z");

    @Test
    void formatsAnApprovedDecision() {
        var decision =
                new ApprovalDecision("alice", "bob", "payments-prod", true, "on-call approved", WHEN);

        String formatted = Main.formatDecision(decision);

        assertTrue(formatted.toUpperCase().contains("APPROVED"));
        assertTrue(formatted.contains("payments-prod"));
        assertTrue(formatted.contains("on-call approved"));
    }

    @Test
    void formatsADeniedDecision() {
        var decision =
                new ApprovalDecision("alice", "alice", "payments-prod", false, "self-approval", WHEN);

        String formatted = Main.formatDecision(decision);

        assertTrue(formatted.toUpperCase().contains("DENIED"));
        assertTrue(formatted.contains("self-approval"));
    }
}