package com.isaqb.practice.eventsourcing.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.isaqb.practice.eventsourcing.event.PipelineRunEvent;
import com.isaqb.practice.eventsourcing.event.RunFinished;
import com.isaqb.practice.eventsourcing.event.RunStarted;
import com.isaqb.practice.eventsourcing.event.StageCompleted;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryEventStoreTest {

    private final EventStore store = new InMemoryEventStore();

    @Test
    void returnsAppendedEventsInAppendOrder() {
        store.append(new RunStarted("run-1", "nightly-build", Instant.parse("2026-01-01T00:00:00Z")));
        store.append(new StageCompleted("run-1", "compile", Instant.parse("2026-01-01T00:01:00Z")));
        store.append(new RunFinished("run-1", true, Instant.parse("2026-01-01T00:02:00Z")));

        List<PipelineRunEvent> events = store.eventsFor("run-1");

        assertEquals(3, events.size());
        assertTrue(events.get(0) instanceof RunStarted);
        assertTrue(events.get(1) instanceof StageCompleted);
        assertTrue(events.get(2) instanceof RunFinished);
    }

    @Test
    void unknownRunIdReturnsEmptyList() {
        assertEquals(List.of(), store.eventsFor("no-such-run"));
    }

    @Test
    void keepsDifferentRunsSeparate() {
        store.append(new RunStarted("run-1", "nightly-build", Instant.now()));
        store.append(new RunStarted("run-2", "release-build", Instant.now()));

        assertEquals(1, store.eventsFor("run-1").size());
        assertEquals(1, store.eventsFor("run-2").size());
    }

    @Test
    void eventsForResultCannotBeMutatedByTheCaller() {
        store.append(new RunStarted("run-1", "nightly-build", Instant.now()));

        List<PipelineRunEvent> events = store.eventsFor("run-1");

        assertThrows(
                UnsupportedOperationException.class,
                () -> events.add(new RunFinished("run-1", true, Instant.now())));
    }

    @Test
    void appendingLaterDoesNotAffectAPreviouslyReturnedList() {
        store.append(new RunStarted("run-1", "nightly-build", Instant.now()));
        List<PipelineRunEvent> firstRead = store.eventsFor("run-1");

        store.append(new RunFinished("run-1", true, Instant.now()));

        assertEquals(1, firstRead.size());
        assertEquals(2, store.eventsFor("run-1").size());
    }
}