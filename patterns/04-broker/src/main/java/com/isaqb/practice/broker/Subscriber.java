package com.isaqb.practice.broker;

import com.isaqb.practice.broker.event.PipelineEvent;

/**
 * Something that wants to react to pipeline events. A Subscriber never knows who
 * published the event it receives, and never knows what other subscribers exist -
 * it only implements this one method.
 */
@FunctionalInterface
public interface Subscriber {

    void onEvent(PipelineEvent event);
}
