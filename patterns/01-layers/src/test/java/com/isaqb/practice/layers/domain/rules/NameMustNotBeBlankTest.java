package com.isaqb.practice.layers.domain.rules;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.isaqb.practice.layers.domain.PipelineConfig;
import java.util.List;
import org.junit.jupiter.api.Test;

class NameMustNotBeBlankTest {

    private final NameMustNotBeBlank rule = new NameMustNotBeBlank();

    @Test
    void failsOnBlankName() {
        var config = new PipelineConfig("   ", List.of("compile"));
        assertTrue(rule.check(config).isPresent());
    }

    @Test
    void passesOnNonBlankName() {
        var config = new PipelineConfig("nightly-build", List.of("compile"));
        assertTrue(rule.check(config).isEmpty());
    }
}
