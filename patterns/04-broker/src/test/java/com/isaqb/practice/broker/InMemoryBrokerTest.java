package com.isaqb.practice.broker;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.isaqb.practice.broker.event.PipelineEvent;
import com.isaqb.practice.broker.event.RunFinished;
import com.isaqb.practice.broker.event.RunStarted;
import com.isaqb.practice.broker.event.StageCompleted;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryBrokerTest {

    @Test
    void deliversEventOnlyToSubscribersOfMatchingType() {
        Broker broker = new InMemoryBroker();
        List<PipelineEvent> runStartedReceived = new ArrayList<>();
        List<PipelineEvent> stageCompletedReceived = new ArrayList<>();
        broker.subscribe(RunStarted.class, runStartedReceived::add);
        broker.subscribe(StageCompleted.class, stageCompletedReceived::add);

        broker.publish(new RunStarted("run-1", "nightly-build"));

        assertEquals(1, runStartedReceived.size());
        assertTrue(stageCompletedReceived.isEmpty());
    }

    @Test
    void supportsMultipleSubscribersForTheSameEventType() {
        Broker broker = new InMemoryBroker();
        List<String> received = new ArrayList<>();
        broker.subscribe(RunFinished.class, event -> received.add("first"));
        broker.subscribe(RunFinished.class, event -> received.add("second"));

        broker.publish(new RunFinished("run-1", true));

        assertEquals(List.of("first", "second"), received);
    }

    @Test
    void publishingWithNoSubscribersIsASilentNoOp() {
        Broker broker = new InMemoryBroker();

        assertDoesNotThrow(() -> broker.publish(new RunFinished("run-1", true)));
    }

    @Test
    void aSubscriberThrowingDoesNotStopOtherSubscribersOrPropagate() {
        Broker broker = new InMemoryBroker();
        List<String> received = new ArrayList<>();
        broker.subscribe(RunStarted.class, event -> { throw new RuntimeException("boom"); });
        broker.subscribe(RunStarted.class, event -> received.add(event.runId()));

        assertDoesNotThrow(() -> broker.publish(new RunStarted("run-1", "nightly-build")));

        assertEquals(List.of("run-1"), received);
    }
}