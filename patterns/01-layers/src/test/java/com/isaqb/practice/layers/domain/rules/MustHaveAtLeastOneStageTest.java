package com.isaqb.practice.layers.domain.rules;

import com.isaqb.practice.layers.domain.PipelineConfig;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MustHaveAtLeastOneStageTest {
    private final MustHaveAtLeastOneStage rule = new MustHaveAtLeastOneStage();

    @Test
    void passesOnAtLeastOneStage() {
        var config = new PipelineConfig("name", List.of("compile"));
        assertTrue(rule.check(config).isEmpty());
    }

    @Test
    void failsOnZeroStages() {
        var config = new PipelineConfig("name", Collections.emptyList());
        assertTrue(rule.check(config).isPresent());
    }
}