package com.isaqb.practice.plugin;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EchoPluginTest {

    @Test
    void testEchoPluginFollowsContract(){
        PipelineStepPlugin plugin = new EchoPlugin();
        Map<String,String> params = new HashMap<>();
        params.put("message","hi");
        var context = new StepContext(params);

        assertEquals("echo", plugin.id());
        assertEquals("hi", plugin.execute(context).message());
    }

    @Test
    void testEchoPluginErrorWhenNotMeetingRequirements(){
        PipelineStepPlugin plugin = new EchoPlugin();
        Map<String,String> params = new HashMap<>();
        var context = new StepContext(params);
        assertThrows(IllegalArgumentException.class,() -> plugin.execute(context));
    }
}
