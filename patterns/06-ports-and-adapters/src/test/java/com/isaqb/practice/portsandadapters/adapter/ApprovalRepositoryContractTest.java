package com.isaqb.practice.portsandadapters.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.isaqb.practice.portsandadapters.core.ApprovalDecision;
import com.isaqb.practice.portsandadapters.core.port.ApprovalRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Every ApprovalRepository implementation - in-memory, file-backed, or anything else
 * a platform engineer plugs in later - must satisfy this same behavior. Subclasses
 * only provide a fresh instance via newRepository(); the test bodies never change.
 * That's the proof, not just the claim, that adapters are interchangeable: the exact
 * same assertions run against every adapter. Public (and its abstract method
 * `protected`) because concrete subclasses live in sibling packages -
 * `adapter.driven.memory` here, `adapter.driven.file` in milestone 6.
 */
public abstract class ApprovalRepositoryContractTest {

    protected abstract ApprovalRepository newRepository();

    @Test
    void returnsEmptyListWhenNothingSaved() {
        var repository = newRepository();

        assertTrue(repository.findByRequester("alice").isEmpty());
    }

    @Test
    void findsASavedDecisionByRequester() {
        var repository = newRepository();
        var decision =
                new ApprovalDecision(
                        "alice", "bob", "payments-prod", true, "ok", Instant.parse("2026-08-01T09:00:00Z"));

        repository.save(decision);

        assertEquals(List.of(decision), repository.findByRequester("alice"));
    }

    @Test
    void doesNotReturnDecisionsForOtherRequesters() {
        var repository = newRepository();
        repository.save(
                new ApprovalDecision(
                        "alice", "bob", "payments-prod", true, "ok", Instant.parse("2026-08-01T09:00:00Z")));

        assertTrue(repository.findByRequester("carol").isEmpty());
    }

    @Test
    void keepsMultipleDecisionsForTheSameRequesterInInsertionOrder() {
        var repository = newRepository();
        var first =
                new ApprovalDecision(
                        "alice", "bob", "payments-prod", true, "first",
                        Instant.parse("2026-08-01T09:00:00Z"));
        var second =
                new ApprovalDecision(
                        "alice", "carol", "payments-staging", false, "second",
                        Instant.parse("2026-08-01T10:00:00Z"));

        repository.save(first);
        repository.save(second);

        assertEquals(List.of(first, second), repository.findByRequester("alice"));
    }
}