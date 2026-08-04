package com.isaqb.practice.layers.domain;

import java.util.List;

/** A pipeline configuration as submitted for validation, before any rule has run. */
public record PipelineConfig(String name, List<String> stages) {
    public PipelineConfig {
        stages = List.copyOf(stages);
    }
}