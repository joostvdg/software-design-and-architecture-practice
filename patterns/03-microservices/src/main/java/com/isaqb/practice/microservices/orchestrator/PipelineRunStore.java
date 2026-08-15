package com.isaqb.practice.microservices.orchestrator;

import java.nio.channels.Pipe;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Orchestrator's own in-memory data: pipeline runs, keyed by id. Saving under an
 * id that's already known overwrites it - e.g. a RUNNING run being saved again as
 * COMPLETED once it finishes.
 */
public final class PipelineRunStore {

    private final Map<String, PipelineRun> runs = new ConcurrentHashMap<>();

    /** Stores (or overwrites) a run, keyed by run.id(). */
    public void save(PipelineRun run) {
        runs.put(run.id(), run);
    }

    /** Returns the run for `id`, or Optional.empty() if unknown. */
    public Optional<PipelineRun> findById(String id) {
        if (runs.containsKey(id)) {
            return Optional.of(runs.get(id));
        }
        return Optional.empty();
    }
}
