package com.isaqb.practice.eventsourcing;

import com.isaqb.practice.eventsourcing.event.PipelineRunEvent;
import com.isaqb.practice.eventsourcing.projection.PipelineRunProjector;
import com.isaqb.practice.eventsourcing.state.PipelineRunState;
import com.isaqb.practice.eventsourcing.store.EventStore;
import java.util.List;

/**
 * Stands in for the case study's Auditor: for compliance review, it doesn't just want
 * "what is the state now" - it wants to see how the state evolved, fact by fact. This
 * is only possible because every fact is still in the log; a design that only kept a
 * mutable "current status" row could never answer "what did we know after event 3,"
 * only "what do we know now."
 */
public final class Auditor {

    private Auditor() {}

    public static void printHistory(EventStore store, String runId) {
        List<PipelineRunEvent> events = store.eventsFor(runId);
        for (int n = 1; n <= events.size(); n++) {
            PipelineRunState asOfN = PipelineRunProjector.replay(runId, events.subList(0, n));
            System.out.printf(
                    "  after event %d (%s): status=%s%n",
                    n, events.get(n - 1).getClass().getSimpleName(), asOfN.status());
        }
    }
}