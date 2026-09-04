package com.isaqb.practice.plugin;

import com.isaqb.practice.plugin.plugins.NotifyStepPlugin;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class NotifyStepPluginTest {

    @Test
    void testNotifyStepPlugin() {
        PipelineStepPlugin plugin = new NotifyStepPlugin();
        Map<String, String> params = new HashMap<>();
        params.put("channel", "email");
        params.put("message", "Hello World!");
        StepContext context = new StepContext(params);
        var result = plugin.execute(context);
        assertTrue(result.success());
        assertEquals("notified email: Hello World!", result.message());
    }

    @Test
    void failIfMessageIsEmpty() {
        PipelineStepPlugin plugin = new NotifyStepPlugin();
        Map<String, String> params = new HashMap<>();
        params.put("channel", "email");
        params.put("message", "");
        StepContext context = new StepContext(params);
        var result = plugin.execute(context);
        assertFalse(result.success());
    }

    @Test
    void failIfUnknownChannel() {
        PipelineStepPlugin plugin = new NotifyStepPlugin();
        Map<String, String> params = new HashMap<>();
        params.put("channel", "twitter");
        params.put("message", "abc");
        StepContext context = new StepContext(params);
        var result = plugin.execute(context);
        assertFalse(result.success());
    }
}
