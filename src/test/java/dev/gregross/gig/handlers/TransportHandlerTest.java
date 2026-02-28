package dev.gregross.gig.handlers;

import dev.gregross.gig.rpc.JsonRpcDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransportHandlerTest {

    private JsonRpcDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new JsonRpcDispatcher();
        new TransportHandler(null).register(dispatcher);
    }

    // --- Registration ---

    @Test
    void registersLoopRangeMethods() {
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("transport/setLoopRange"));
        assertTrue(methods.contains("transport/getLoopRange"));
    }

    @Test
    void registersPunchMethods() {
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("transport/setPunchIn"));
        assertTrue(methods.contains("transport/setPunchOut"));
    }

    @Test
    void registersAutomationMethods() {
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("transport/setAutomationWriteMode"));
        assertTrue(methods.contains("transport/setArrangerAutomationWrite"));
        assertTrue(methods.contains("transport/setClipLauncherAutomationWrite"));
        assertTrue(methods.contains("transport/resetAutomationOverrides"));
    }

    @Test
    void registersNineteenMethodsTotal() {
        // 11 original + 8 new = 19
        assertEquals(19, dispatcher.getRegisteredMethods().size());
    }

    // --- setLoopRange validation ---

    @Test
    void setLoopRange_missingStart_returnsError() {
        String response = dispatcher.handle(rpc("transport/setLoopRange",
            "{\"duration\": 16.0, \"enabled\": true}"));
        assertContains(response, "-32602");
        assertContains(response, "start");
    }

    @Test
    void setLoopRange_missingDuration_returnsError() {
        String response = dispatcher.handle(rpc("transport/setLoopRange",
            "{\"start\": 0.0, \"enabled\": true}"));
        assertContains(response, "-32602");
        assertContains(response, "duration");
    }

    @Test
    void setLoopRange_missingEnabled_returnsError() {
        String response = dispatcher.handle(rpc("transport/setLoopRange",
            "{\"start\": 0.0, \"duration\": 16.0}"));
        assertContains(response, "-32602");
        assertContains(response, "enabled");
    }

    // --- setPunchIn validation ---

    @Test
    void setPunchIn_missingPosition_returnsError() {
        String response = dispatcher.handle(rpc("transport/setPunchIn",
            "{\"enabled\": true}"));
        assertContains(response, "-32602");
        assertContains(response, "position");
    }

    @Test
    void setPunchIn_missingEnabled_returnsError() {
        String response = dispatcher.handle(rpc("transport/setPunchIn",
            "{\"position\": 4.0}"));
        assertContains(response, "-32602");
        assertContains(response, "enabled");
    }

    // --- setPunchOut validation ---

    @Test
    void setPunchOut_missingPosition_returnsError() {
        String response = dispatcher.handle(rpc("transport/setPunchOut",
            "{\"enabled\": true}"));
        assertContains(response, "-32602");
        assertContains(response, "position");
    }

    @Test
    void setPunchOut_missingEnabled_returnsError() {
        String response = dispatcher.handle(rpc("transport/setPunchOut",
            "{\"position\": 8.0}"));
        assertContains(response, "-32602");
        assertContains(response, "enabled");
    }

    // --- setAutomationWriteMode validation ---

    @Test
    void setAutomationWriteMode_missingMode_returnsError() {
        String response = dispatcher.handle(rpc("transport/setAutomationWriteMode", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "mode");
    }

    @Test
    void setAutomationWriteMode_invalidMode_returnsError() {
        String response = dispatcher.handle(rpc("transport/setAutomationWriteMode",
            "{\"mode\": \"invalid\"}"));
        assertContains(response, "-32602");
        assertContains(response, "invalid");
    }

    @Test
    void setAutomationWriteMode_emptyString_returnsError() {
        String response = dispatcher.handle(rpc("transport/setAutomationWriteMode",
            "{\"mode\": \"\"}"));
        assertContains(response, "-32602");
    }

    // --- setArrangerAutomationWrite validation ---

    @Test
    void setArrangerAutomationWrite_missingEnabled_returnsError() {
        String response = dispatcher.handle(rpc("transport/setArrangerAutomationWrite", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "enabled");
    }

    // --- setClipLauncherAutomationWrite validation ---

    @Test
    void setClipLauncherAutomationWrite_missingEnabled_returnsError() {
        String response = dispatcher.handle(rpc("transport/setClipLauncherAutomationWrite", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "enabled");
    }

    // --- Helpers ---

    private String rpc(String method, String params) {
        return "{\"jsonrpc\":\"2.0\",\"method\":\"" + method + "\",\"params\":" + params + ",\"id\":1}";
    }

    private void assertContains(String actual, String expected) {
        assertTrue(actual.contains(expected),
            "Expected '" + expected + "' in: " + actual);
    }
}
