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
    void registersFifteenMethods() {
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
        assertTrue(methods.contains("masterDevice/enterLayer"));
        assertTrue(methods.contains("masterDevice/enterKeyPad"));
        assertTrue(methods.contains("masterDevice/selectPageByTag"));
        assertEquals(15, methods.size());
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

    // --- masterDevice/enterLayer validation ---

    @Test
    void enterLayer_missingBothParams_returnsError() {
        String response = dispatcher.handle(rpc("masterDevice/enterLayer", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "must provide");
    }

    @Test
    void enterLayer_bothParams_returnsError() {
        String response = dispatcher.handle(rpc("masterDevice/enterLayer", "{\"index\":0,\"name\":\"Layer 1\"}"));
        assertContains(response, "-32602");
        assertContains(response, "mutually exclusive");
    }

    // --- masterDevice/enterKeyPad validation ---

    @Test
    void enterKeyPad_missingKey_returnsError() {
        String response = dispatcher.handle(rpc("masterDevice/enterKeyPad", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "key");
    }

    @Test
    void enterKeyPad_keyOutOfRange_returnsError() {
        String response = dispatcher.handle(rpc("masterDevice/enterKeyPad", "{\"key\":128}"));
        assertContains(response, "-32602");
        assertContains(response, "0-127");
    }

    // --- masterDevice/selectPageByTag validation ---

    @Test
    void selectPageByTag_missingTag_returnsError() {
        String response = dispatcher.handle(rpc("masterDevice/selectPageByTag", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "tag");
    }

    @Test
    void selectPageByTag_invalidTag_returnsError() {
        String response = dispatcher.handle(rpc("masterDevice/selectPageByTag", "{\"tag\":\"bogus\"}"));
        assertContains(response, "-32602");
        assertContains(response, "Invalid page tag");
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
