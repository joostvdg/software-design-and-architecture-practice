package com.isaqb.practice.pipesandfilters;

import java.time.Instant;

public record LogEntry(Instant timestamp, String level, String message) {
}
