package com.isaqb.practice.cqrs.command;

/** Thrown when a command targets a run id that doesn't exist. */
public class PipelineRunNotFoundException extends RuntimeException{

    public PipelineRunNotFoundException(String runId){
        super("no pipeline run found with id: "+runId);
    }
}
