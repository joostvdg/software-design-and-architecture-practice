package com.isaqb.practice.microservices.registry;

/** The outcome of handling one request: an HTTP status code and a response body. */
public record HttpResult(int status, String body) {
}
