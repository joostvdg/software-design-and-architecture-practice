package com.isaqb.practice.eventsourcing.projection;

import com.isaqb.practice.eventsourcing.event.*;
import com.isaqb.practice.eventsourcing.state.PipelineRunState;
import com.isaqb.practice.eventsourcing.state.RunStatus;
import com.isaqb.practice.eventsourcing.state.StageState;
import com.isaqb.practice.eventsourcing.state.StageStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

/**
 * The heart of this exercise: turns an ordered list of facts into the single derived
 * value that answers "what is the state of this run" - and, just as important, "what
 * was its state after only the first N events" (see Auditor, milestone 6).
 * PipelineRunState is never stored directly anywhere in this module; it is always
 * computed on demand by replaying events through this class.
 */
public final class PipelineRunProjector {

    private PipelineRunProjector() {}

    /**
     * Replays every event in {@code events}, in order, starting from
     * {@link PipelineRunState#unknown(String)}. Exactly
     * {@code replayFrom(PipelineRunState.unknown(runId), events)} - given here so you
     * don't have to spell that out at every call site.
     */
    public static PipelineRunState replay(String runId, List<PipelineRunEvent> events) {
        return replayFrom(PipelineRunState.unknown(runId), events);
    }

    /**
     * Folds {@code events}, in order, onto {@code initialState} and returns the result.
     * This is a pure function: it never mutates {@code initialState}, never mutates
     * {@code events}, performs no I/O, and calling it twice with the same arguments
     * always returns an equal result. That purity is exactly what makes it safe to unit
     * test with nothing but a hand-built {@code List<PipelineRunEvent>} - no EventStore
     * needed - and it's also what milestone 4's snapshot-assisted replay depends on:
     * folding onto a previously-saved state must behave identically to folding onto a
     * fresh one.
     *
     * TODO: implement the fold. Starting from {@code initialState}, apply each event in
     * {@code events}, in order:
     *
     *  - RunStarted: set pipelineName and startedAt from the event; status -> RUNNING.
     *  - StageStarted: if no StageState with this stageName exists yet, add a new one -
     *    attempts=1, status=RUNNING, startedAt=this event's time, finishedAt=null. If one
     *    already exists (this is a retry), replace it - attempts = old attempts + 1,
     *    status=RUNNING, startedAt=this event's time (the new attempt's start),
     *    finishedAt=null. Keep stages in the order they were *first* started, even across
     *    retries.
     *  - StageCompleted: replace the matching StageState with status=SUCCEEDED,
     *    finishedAt=this event's time (name/attempts/startedAt unchanged).
     *  - StageFailed: replace the matching StageState with status=FAILED,
     *    finishedAt=this event's time (name/attempts/startedAt unchanged).
     *  - RunFinished: status -> SUCCEEDED if event.success() else FAILED; finishedAt =
     *    this event's time.
     *
     *  A StageCompleted or StageFailed for a stage name with no prior StageStarted in
     *  {@code initialState} or {@code events} so far is malformed input for this
     *  exercise: throw IllegalStateException naming the runId and stage. The projector
     *  trusts the log is well-formed - a malformed log is a bug in whatever appended it,
     *  not something a reader should silently paper over.
     *
     * Hint: a LinkedHashMap<String, StageState> keyed by stage name preserves insertion
     * order for you, and re-`put`-ting an existing key updates its value without moving
     * its position - convenient for "keep stages in first-started order, but let later
     * events update them in place." Build the map from initialState.stages(), apply every
     * event to it, then take map.values() as your final stages list.
     */
    public static PipelineRunState replayFrom(
            PipelineRunState initialState, List<PipelineRunEvent> events) {


        LinkedHashMap<String, StageState> stagesProcessed = new  LinkedHashMap<>();
        Instant pipelineStarted = initialState.startedAt();
        Instant pipelineFinished = initialState.finishedAt();
        RunStatus pipelineRunStatus = initialState.status();
        String pipelineName = initialState.pipelineName();
        String runId = initialState.runId();
        Map<String, Integer> stageAttempts = new LinkedHashMap<>();

        for (StageState stageState : initialState.stages()) {
            stagesProcessed.put(stageState.name(), stageState);
            stageAttempts.put(stageState.name(), stageState.attempts());
        }

        for (PipelineRunEvent event : events) {
            if (!initialState.runId().equals(event.runId())) {
                throw new IllegalStateException("Attempt to replay an unknown run id " + event.runId());
            }

            switch (event) {
                case RunStarted e ->  {
                    pipelineRunStatus = RunStatus.RUNNING;
                    pipelineStarted = e.occurredAt();
                    pipelineName = e.pipelineName();
                    runId = e.runId();
                    System.out.println("Pipeline started: " + e.pipelineName() + "- at " + pipelineStarted);
                }
                case StageStarted e -> {
                    int attempts = 1;
                    if (stageAttempts.containsKey(e.stageName())) {
                        attempts = stageAttempts.get(e.stageName()) + 1;
                    }
                    stageAttempts.put(e.stageName(), attempts);
                    System.out.println("Stage started: " + e.stageName() + " (attempt #" + attempts + ")");
                    StageState stageState = new StageState(
                            e.stageName(),
                            StageStatus.RUNNING,
                            attempts,
                            e.occurredAt(),
                            null
                    );
                    stagesProcessed.put(e.stageName(), stageState);
                }
                case StageCompleted e -> {
                    if (!stagesProcessed.containsKey(e.stageName())) {
                        throw new IllegalStateException("Attempt to replay an unknown stage name " + e.stageName());
                    }

                    StageState previousStageState = stagesProcessed.get(e.stageName());
                    Instant stageStarted= null;
                    if (previousStageState != null) {
                        stageStarted = previousStageState.startedAt();
                    }

                    int attempts = 1;
                    if (stageAttempts.containsKey(e.stageName())) {
                        attempts = stageAttempts.get(e.stageName());
                    }
                    System.out.println("Stage completed: " + e.stageName() + " (attempt #" + attempts + ")");
                    StageState stageState = new StageState(
                            e.stageName(),
                            StageStatus.SUCCEEDED,
                            attempts,
                            stageStarted,
                            e.occurredAt()
                    );
                    stagesProcessed.put(e.stageName(), stageState);
                }
                case StageFailed e -> {
                    if (!stagesProcessed.containsKey(e.stageName())) {
                        throw new IllegalStateException("Attempt to replay an unknown stage name " + e.stageName());
                    }
                    int attempts = 1;
                    if (stageAttempts.containsKey(e.stageName())) {
                        attempts = stageAttempts.get(e.stageName());
                    }
                    System.out.println("Stage failed: " + e.stageName() + " (attempt #" + attempts + ")");

                    StageState previousStageState = stagesProcessed.get(e.stageName());
                    Instant stageStarted= null;
                    if (previousStageState != null) {
                        stageStarted = previousStageState.startedAt();
                    }

                    StageState stageState = new StageState(
                            e.stageName(),
                            StageStatus.FAILED,
                            attempts,
                            stageStarted,
                            e.occurredAt()
                    );
                    stagesProcessed.put(e.stageName(), stageState);
                }
                case RunFinished e -> {
                    if (e.success()) {
                        pipelineRunStatus = RunStatus.SUCCEEDED;
                    } else {
                        pipelineRunStatus = RunStatus.FAILED;
                    }
                    pipelineFinished = e.occurredAt();
                }
            }
        }

        List<StageState> stageStates = new LinkedList<>(stagesProcessed.values());

        System.out.println("[Pipeline " + pipelineName + " - started at: " + pipelineStarted + " - finished at: " + pipelineFinished + "]"); ;
        var runState = new PipelineRunState(
                runId,
                pipelineRunStatus,
                pipelineName,
                pipelineStarted,
                pipelineFinished,
                stageStates);
        System.out.println("Run state: " + runState);
        return runState;
    }
}
