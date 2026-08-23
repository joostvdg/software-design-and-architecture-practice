package com.isaqb.practice.cqrs.command;

/** Thrown when a command would violate a pipeline run's lifecycle rules. */
public class InvalidCommandException extends RuntimeException {
    public InvalidCommandException(String message) {
        super(message);
    }
}
