package com.isaqb.practice.layers.domain.rules;

import com.isaqb.practice.layers.domain.PipelineConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class StageNameMustBeUniqueTest {

    private final StageNamesMustBeUnique rule =  new StageNamesMustBeUnique();

    @Test
    public void failsOnDuplicateStages() {
        var config = new PipelineConfig("Test", List.of("test", "test"));
        assertTrue(rule.check(config).isPresent());
    }

    @Test
    public void passesOnUniqueStages() {
        var config = new PipelineConfig("Test", List.of("compile", "test"));
        assertTrue(rule.check(config).isEmpty());
    }
}
