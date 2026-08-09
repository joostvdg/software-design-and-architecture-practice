package com.isaqb.practice.pipesandfilters;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class LogLineParser implements Filter<String, LogEntry> {

    @Override
    public List<LogEntry> apply(List<String> input) {
        // TODO: for each raw line in `input`, in order:
        //  - if the line is blank (empty or all whitespace), skip it - it does not produce
        //    a LogEntry.
        //  - otherwise split it into at most 3 parts on spaces (the message itself may
        //    contain spaces, so a plain 3-way split is what you want -
        //    String.split(" ", 3) keeps the third part intact). If you don't get 3 parts,
        //    the line is malformed: throw an IllegalArgumentException naming the
        //    offending line.
        //  - parse the first part as a timestamp with java.time.Instant.parse(...). It
        //    throws java.time.format.DateTimeParseException on bad input, which is NOT an
        //    IllegalArgumentException - catch it and wrap it into one (or throw a new
        //    IllegalArgumentException naming the offending line) so callers only ever have
        //    to handle one exception type from this filter.
        //  - the second part is the level, used as-is (e.g. "INFO", "DEBUG", "ERROR").
        //  - the third part is the message, used as-is.
        // Return one LogEntry per non-blank input line, preserving the original order.
        List<LogEntry> logEntries = new ArrayList<>();

        for (String line : input) {
            if (line.isEmpty() || line.startsWith("#") || line.trim().isEmpty()) {
                continue; // not a Log Entry
            }

            String[] parts = line.split(" ", 3);
            if (parts.length < 3) {
                throw new IllegalArgumentException("Invalid log line: " + line);
            }

            // assume first segment is a timestamp
            Instant timestamp = null;
            try {
                timestamp = Instant.parse(parts[0]);
            } catch (DateTimeParseException dateTimeParseException) {
                throw new IllegalArgumentException(dateTimeParseException);
            }

            // assume second part is level -> validate
            String logLevel = parts[1];

            // last segment is the message
            String logMessage = parts[2];

            var logEntry = new LogEntry(timestamp, logLevel, logMessage);
            logEntries.add(logEntry);
        }

        return logEntries;
    }
}
