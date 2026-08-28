package com.isaqb.practice.eventsourcing.store;

import com.isaqb.practice.eventsourcing.event.PipelineRunEvent;

import java.util.*;

/**
 * An in-memory EventStore: every run's events are kept in a List, keyed by runId, for
 * the lifetime of this object. Not durable across process restarts - that's a real
 * limitation for production use, but irrelevant to what this exercise is teaching: the
 * append-only, replay-to-derive-state discipline works identically whether the log
 * lives in a HashMap or a distributed log.
 */
public class InMemoryEventStore  implements EventStore {

    private final Map<String, List<PipelineRunEvent>> eventsByRunId = new HashMap<>();

    @Override
    public void append(PipelineRunEvent event) {
        // TODO: append `event` to the list kept for event.runId(), preserving append order.
        // Create the list on first use for a runId (Map.computeIfAbsent with a fresh
        // ArrayList<>() as the default is enough). This method only ever grows a run's log -
        // never remove or replace an existing entry.
        eventsByRunId.computeIfAbsent(event.runId(), k-> new ArrayList<PipelineRunEvent>());
        eventsByRunId.get(event.runId()).add(event);
    }

    @Override
    public List<PipelineRunEvent> eventsFor(String runId) {
        // TODO: return every event appended for `runId`, in append order, or an empty list
        // if nothing has ever been appended for this runId (never return null).
        // Important: the returned list must be a *defensive, unmodifiable copy* - callers
        // must never be able to mutate this store's internal log by mutating what
        // eventsFor() handed them. List.copyOf(...) on whatever you looked up (or List.of()
        // for an unknown runId) gives you that for free.

        if (!eventsByRunId.containsKey(runId)) {
            return new ArrayList<>();
        }

        var events = eventsByRunId.get(runId);
        return List.copyOf(events);
    }
}
