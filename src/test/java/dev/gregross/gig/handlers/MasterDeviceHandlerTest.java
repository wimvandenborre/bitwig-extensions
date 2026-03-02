package dev.gregross.gig.handlers;

import dev.gregross.gig.rpc.JsonRpcDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MasterDeviceHandlerTest {

    private JsonRpcDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new JsonRpcDispatcher();
        new MasterDeviceHandler(null, null, null, null).register(dispatcher);
    }

    // --- Registration ---

    @Test
    void registersTwelveMethods() {
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("masterDevice/selectNext"));
        assertTrue(methods.contains("masterDevice/selectPrevious"));
        assertTrue(methods.contains("masterDevice/setEnabled"));
        assertTrue(methods.contains("masterDevice/insertBitwigDevice"));
        assertTrue(methods.contains("masterDevice/insertPluginDevice"));
        assertTrue(methods.contains("masterDevice/remove"));
        assertTrue(methods.contains("masterDevice/selectPage"));
        assertTrue(methods.contains("masterDevice/nextPage"));
        assertTrue(methods.contains("masterDevice/previousPage"));
        assertTrue(methods.contains("masterDevice/setParameterValue"));
        assertTrue(methods.contains("masterDevice/enterSlot"));
        assertTrue(methods.contains("masterDevice/exitToParent"));
        assertEquals(12, methods.size());
    }

    // --- setEnabled validation ---

    @Test
    void setEnabled_missingEnabled_returnsError() {
        String response = dispatcher.handle(rpc("masterDevice/setEnabled", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "enabled");
    }

    // --- insertBitwigDevice validation ---

    @Test
    void insertBitwigDevice_missingName_returnsError() {
        String response = dispatcher.handle(rpc("masterDevice/insertBitwigDevice", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "name");
    }

    // --- insertPluginDevice validation ---

    @Test
    void insertPluginDevice_missingType_returnsError() {
        String response = dispatcher.handle(rpc("masterDevice/insertPluginDevice", "{\"id\":\"abc\"}"));
        assertContains(response, "-32602");
        assertContains(response, "type");
    }

    @Test
    void insertPluginDevice_missingId_returnsError() {
        String response = dispatcher.handle(rpc("masterDevice/insertPluginDevice", "{\"type\":\"vst3\"}"));
        assertContains(response, "-32602");
        assertContains(response, "id");
    }

    // --- enterSlot validation ---

    @Test
    void enterSlot_missingName_returnsError() {
        String response = dispatcher.handle(rpc("masterDevice/enterSlot", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "name");
    }

    // --- selectPage validation ---

    @Test
    void selectPage_missingIndex_returnsError() {
        String response = dispatcher.handle(rpc("masterDevice/selectPage", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "index");
    }

    // --- setParameterValue validation ---

    @Test
    void setParameterValue_missingIndex_returnsError() {
        String response = dispatcher.handle(rpc("masterDevice/setParameterValue", "{\"value\":0.5}"));
        assertContains(response, "-32602");
        assertContains(response, "index");
    }

    @Test
    void setParameterValue_missingValue_returnsError() {
        String response = dispatcher.handle(rpc("masterDevice/setParameterValue", "{\"index\":0}"));
        assertContains(response, "-32602");
        assertContains(response, "value");
    }

    @Test
    void setParameterValue_indexOutOfRange_returnsError() {
        String response = dispatcher.handle(rpc("masterDevice/setParameterValue", "{\"index\":8,\"value\":0.5}"));
        assertContains(response, "-32602");
        assertContains(response, "out of range");
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
