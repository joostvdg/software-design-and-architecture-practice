package com.isaqb.practice.broker;

import com.isaqb.practice.broker.event.PipelineEvent;

import java.util.*;

/**
 * A simple, in-process Broker: subscriptions are held in memory, and publish()
 * dispatches synchronously, on the calling thread, before returning. This decouples
 * *location* (publishers and subscribers never reference each other) but not *time*
 * (there is no queue - an event published to nobody is simply lost, and a subscriber
 * that isn't registered yet will never see events published before it subscribed).
 */
public class InMemoryBroker implements Broker {

    private final Map<Class<? extends PipelineEvent>, List<Subscriber>> subscribersByType = new HashMap<>();

    @Override
    public void subscribe(Class<? extends PipelineEvent> eventType, Subscriber subscriber) {
        // TODO: register `subscriber` under `eventType` in `subscribersByType`. Multiple
        // subscribers for the same eventType must all be kept (don't overwrite), and must
        // stay in the order they were registered. Hint: Map.computeIfAbsent with a fresh
        // ArrayList<>() as the default is enough - you don't need anything more elaborate.
        List<Subscriber> subscribers = subscribersByType.get(eventType);
        if (subscribers == null) {
            subscribers = new ArrayList<>();
        }
        subscribers.add(subscriber);
        subscribersByType.put(eventType, subscribers);
    }

    @Override
    public void publish(PipelineEvent event) {
        // TODO:
        //  1. Look up the subscribers registered for event.getClass() (there may be none -
        //     that's fine, treat it as an empty list, not an error).
        //  2. Call onEvent(event) on each one, in registration order.
        //  3. If a subscriber's onEvent throws, catch the exception right there (a plain
        //     `catch (RuntimeException e)` is fine - print something to System.err with
        //     the subscriber and the exception) and keep going with the remaining
        //     subscribers. One broken subscriber must never stop other subscribers from
        //     being notified, and must never propagate back out of publish() to whatever
        //     called it - the Pipeline Runner (milestone 4) has no idea subscribers exist
        //     at all, and a subscriber's bug is not the Pipeline Runner's problem.
        var subscribers = subscribersByType.get(event.getClass());
        if (subscribers == null) {
            return;
        }
        int numberNotifiedSuccesfully = 0;
        int numberNotifiedFailed = 0;
        for (Subscriber subscriber : subscribers) {
            try {
                subscriber.onEvent(event);
                numberNotifiedFailed++;
            } catch (RuntimeException e) {
                numberNotifiedFailed++;
                String errorMessage = String.format("Error when running onEvent for subscriber %s: %s", subscriber.getClass().getName(), e.getMessage());
                System.err.println(errorMessage);
            }
        }
        String endOfRunMessage = String.format("Finished notifying subscribers: %d success, %d failed", numberNotifiedSuccesfully, numberNotifiedFailed);

    }
}
