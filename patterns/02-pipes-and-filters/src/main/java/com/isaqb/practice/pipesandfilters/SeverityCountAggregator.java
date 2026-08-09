package com.isaqb.practice.pipesandfilters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Filter 4: the pipeline's final step, for the Observability team - collapses a list
 * of LogEntry into one SeverityCount per distinct level. Unlike the earlier filters,
 * this one changes the "shape" of the data (many entries in, a handful of summary rows
 * out), but it's still just a Filter<LogEntry, SeverityCount> - same contract, same
 * composability as every filter before it.
 */
public class SeverityCountAggregator implements Filter<LogEntry, SeverityCount>{
    @Override
    public List<SeverityCount> apply(List<LogEntry> input) {
        // TODO: group `input` by level and count entries per level. Return one
        // SeverityCount per distinct level present in `input`; order doesn't matter.
        // Hint: java.util.stream.Collectors.groupingBy(LogEntry::level, Collectors.counting())
        // gets you a Map<String, Long> in one line; turn each entry of that map into a
        // SeverityCount.
        Map<String, Long> byLevel = input.stream().collect(Collectors.groupingBy(LogEntry::level, Collectors.counting()));
        List<SeverityCount> result = new ArrayList<>();
        byLevel.forEach((level, count) -> result.add(new SeverityCount(level, count)));

        return result;
    }
}
