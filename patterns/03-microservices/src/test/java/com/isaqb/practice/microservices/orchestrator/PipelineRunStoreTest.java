package com.isaqb.practice.microservices.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PipelineRunStoreTest {

    private final PipelineRunStore store = new PipelineRunStore();

    @Test
    void findByIdIsEmptyWhenUnknown() {
        assertTrue(store.findById("run-1").isEmpty());
    }

    @Test
    void saveThenFindByIdReturnsIt() {
        var run = PipelineRun.running("run-1");

        store.save(run);

        assertEquals(run, store.findById("run-1").orElseThrow());
    }

    @Test
    void savingAgainUnderSameIdOverwrites() {
        store.save(PipelineRun.running("run-1"));
        var completed =
                new PipelineRun("run-1", RunStatus.COMPLETED, "web-app", "1.4.2", "sha256:abc123");

        store.save(completed);

        var found = store.findById("run-1").orElseThrow();
        assertEquals(RunStatus.COMPLETED, found.status());
        assertEquals("web-app", found.artifactName());
    }
}