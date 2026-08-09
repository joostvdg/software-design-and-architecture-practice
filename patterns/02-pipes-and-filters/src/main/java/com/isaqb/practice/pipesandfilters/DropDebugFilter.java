package com.isaqb.practice.pipesandfilters;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Filter 2: drops noise. Removes DEBUG-level entries from the stream. Notice this
 * filter has no idea LogLineParser exists, or that a RedactSecretsFilter runs after
 * it - it only knows "given a list of LogEntry, produce a list of LogEntry". That's
 * the independence Pipes and Filters is built on.
 */
public class DropDebugFilter implements Filter<LogEntry, LogEntry> {

    @Override
    public List<LogEntry> apply(List<LogEntry> input) {
        // TODO: return a new list containing every entry from `input` whose level is not
        // "DEBUG" (case-sensitive is fine for this exercise), preserving relative order.

        return input.stream().filter(e -> !e.level().equalsIgnoreCase("DEBUG")).
                collect(Collectors.toList());
    }
}
