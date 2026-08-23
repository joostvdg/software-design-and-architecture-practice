package com.isaqb.practice.cqrs.command;

/** The lifecycle state of one stage within a pipeline run. */
public enum StageStatus {
    PENDING,
    COMPLETE
}
