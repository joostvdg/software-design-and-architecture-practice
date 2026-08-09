package com.isaqb.practice.pipesandfilters;

/** One severity level and how many entries at that level passed through the pipeline. */
public record SeverityCount(String level, long count) {
}
