package com.isaqb.practice.layers.domain;

/** One rule violation: which rule failed, and a human-readable reason. */
public record ValidationError(String rule, String message) {
}
