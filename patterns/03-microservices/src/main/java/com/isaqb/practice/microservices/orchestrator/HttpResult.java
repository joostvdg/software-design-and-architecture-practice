package com.isaqb.practice.microservices.orchestrator;

/** The outcome of handling one request: an HTTP status code and a response body. */
public record HttpResult(int status, String body) {
}
