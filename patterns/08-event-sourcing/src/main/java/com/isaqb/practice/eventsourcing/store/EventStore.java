package com.isaqb.practice.eventsourcing.store;

import com.isaqb.practice.eventsourcing.event.PipelineRunEvent;

import java.util.List;

/**
 * An append-only log: the only two operations are "add a fact" and "read every fact
 * recorded so far for a run", in the order they were recorded. There is deliberately no
 * update or delete - once an event is appended, it stays. An in-memory Map/List is
 * enough for this exercise; a production event store would be a durable, append-only
 * table or log (a Kafka topic, EventStoreDB, an append-only SQL table with no UPDATE
 * grants), but the contract - append, read-in-order, never mutate - is identical.
 */
public interface EventStore {

    /** Records {@code event} as the next fact for its run. Never overwrites or removes
     * anything already recorded. */
    void append(PipelineRunEvent event);;

    /** Every event recorded for {@code runId} so far, in the order it was appended.
     * Returns an empty list (never null) if nothing has been recorded for this runId. */
    List<PipelineRunEvent> eventsFor(String runId);
}
