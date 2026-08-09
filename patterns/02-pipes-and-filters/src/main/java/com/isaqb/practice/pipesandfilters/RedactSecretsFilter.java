package com.isaqb.practice.pipesandfilters;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Filter 3: redacts anything that looks like a credential before entries are stored or
 * searched. Same Filter<LogEntry, LogEntry> shape as DropDebugFilter - a different
 * transformation, wired the same way.
 */
public class RedactSecretsFilter implements Filter<LogEntry, LogEntry> {

    // Matches "token=<value>", where <value> is a run of letters, digits, '-' or '_'.
    private static final Pattern TOKEN_PATTERN = Pattern.compile("token=([A-Za-z0-9_-]+)");

    @Override
    public List<LogEntry> apply(List<LogEntry> input) {

        // TODO: for each entry in `input`, replace every match of TOKEN_PATTERN in its
        // message with the literal text "token=***REDACTED***", leaving the timestamp and
        // level unchanged. Return a new list of LogEntry with the redacted messages, same
        // order as the input.
        // Hint: LogEntry has no setters (it's a record) - build a *new* LogEntry per
        // entry rather than trying to mutate one. Matcher#replaceAll (or
        // String#replaceAll(TOKEN_PATTERN.pattern(), ...)) does the substitution itself.

        List<LogEntry> result = new ArrayList<>();

        for (LogEntry entry : input) {
            if (isToken(entry.message())) {
                result.add(cleanedLogEntry(entry));
            } else {
                result.add(entry);
            }
        }

        return result;

    }

    private LogEntry cleanedLogEntry(LogEntry entry) {
        String cleanedMessage = entry.message().replaceAll(TOKEN_PATTERN.pattern(), "token=***REDACTED***");
        return new LogEntry(entry.timestamp(), entry.level(), cleanedMessage);
    }

    private boolean isToken(String messageToEvaluate) {
        return TOKEN_PATTERN.matcher(messageToEvaluate).find();
    }
}
