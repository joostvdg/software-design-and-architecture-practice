package com.isaqb.practice.broker;

import com.isaqb.practice.broker.event.PipelineEvent;

/**
 * The intermediary: publishers publish events without knowing who (if anyone) is
 * listening; subscribers register interest in an event type without knowing who (if
 * anyone) will ever publish one. Neither side ever references the other directly -
 * they only ever reference Broker.
 */
public interface Broker {

    /**
     * Registers {@code subscriber} to be notified of every future event whose runtime
     * type is exactly {@code eventType}. Multiple subscribers may register for the same
     * event type; all of them must be notified, in the order they subscribed.
     */
    void subscribe(Class<? extends PipelineEvent> eventType, Subscriber subscriber);

    /**
     * Notifies every subscriber currently registered for {@code event}'s runtime type.
     * If nobody is subscribed, this is a silent no-op - not an error. Callers of publish
     * never find out who (if anyone) received the event.
     */
    void publish(PipelineEvent event);
}
