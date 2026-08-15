package com.isaqb.practice.microservices.orchestrator;

/** Thrown when the Artifact Registry can't be reached, or rejects a request. */
public class RegistryUnavailableException extends Exception {

    public RegistryUnavailableException(String message) {
        super(message);
    }

    public RegistryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
