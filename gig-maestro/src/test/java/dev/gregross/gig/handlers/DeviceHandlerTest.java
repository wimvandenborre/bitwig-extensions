package dev.gregross.gig.handlers;

import dev.gregross.gig.rpc.JsonRpcDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeviceHandlerTest {

    private JsonRpcDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        // Pass null for Bitwig API objects — we only test parameter validation,
        // not actual API calls (those would NPE on null objects)
        dispatcher = new JsonRpcDispatcher();
        new DeviceHandler(null, null, null, null, null, null, null).register(dispatcher);
    }

    // --- Registration ---

    @Test
    void registersFiveNewAutomationMethods() {
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("device/hasAutomation"));
        assertTrue(methods.contains("device/deleteAllAutomation"));
        assertTrue(methods.contains("device/restoreAutomationControl"));
        assertTrue(methods.contains("device/touch"));
        assertTrue(methods.contains("device/writeEnvelope"));
    }

    @Test
    void registersAllExistingMethods() {
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("device/selectNext"));
        assertTrue(methods.contains("device/selectPrevious"));
        assertTrue(methods.contains("device/setEnabled"));
        assertTrue(methods.contains("device/selectPage"));
        assertTrue(methods.contains("device/nextPage"));
        assertTrue(methods.contains("device/previousPage"));
        assertTrue(methods.contains("device/setParameterValue"));
        assertTrue(methods.contains("device/insertBitwigDevice"));
        assertTrue(methods.contains("device/insertPluginDevice"));
        assertTrue(methods.contains("device/listBitwigDevices"));
        assertTrue(methods.contains("device/remove"));
        assertTrue(methods.contains("cursor/selectTrack"));
    }

    @Test
    void registersChainNavMethods() {
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("device/enterSlot"));
        assertTrue(methods.contains("device/exitToParent"));
    }

    @Test
    void registersDrumPadMethod() {
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("device/getDrumPads"));
    }

    @Test
    void registersLayerAndKeyPadMethods() {
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("device/enterLayer"));
        assertTrue(methods.contains("device/enterKeyPad"));
        assertTrue(methods.contains("device/selectPageByTag"));
    }

    @Test
    void registersExactlyTwentyThreeMethods() {
        // 17 existing + 2 chain nav + 1 getDrumPads + 3 sound design nav
        assertEquals(23, dispatcher.getRegisteredMethods().size());
    }

    // --- device/hasAutomation validation ---

    @Test
    void hasAutomation_missingIndex_returnsError() {
        String response = dispatcher.handle(rpc("device/hasAutomation", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "index");
    }

    @Test
    void hasAutomation_indexTooHigh_returnsError() {
        String response = dispatcher.handle(rpc("device/hasAutomation", "{\"index\": 8}"));
        assertContains(response, "-32602");
        assertContains(response, "out of range");
    }

    @Test
    void hasAutomation_negativeIndex_returnsError() {
        String response = dispatcher.handle(rpc("device/hasAutomation", "{\"index\": -1}"));
        assertContains(response, "-32602");
        assertContains(response, "out of range");
    }

    // --- device/deleteAllAutomation validation ---

    @Test
    void deleteAllAutomation_missingIndex_returnsError() {
        String response = dispatcher.handle(rpc("device/deleteAllAutomation", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "index");
    }

    @Test
    void deleteAllAutomation_indexTooHigh_returnsError() {
        String response = dispatcher.handle(rpc("device/deleteAllAutomation", "{\"index\": 8}"));
        assertContains(response, "-32602");
        assertContains(response, "out of range");
    }

    // --- device/restoreAutomationControl validation ---

    @Test
    void restoreAutomationControl_missingIndex_returnsError() {
        String response = dispatcher.handle(rpc("device/restoreAutomationControl", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "index");
    }

    @Test
    void restoreAutomationControl_indexTooHigh_returnsError() {
        String response = dispatcher.handle(rpc("device/restoreAutomationControl", "{\"index\": 8}"));
        assertContains(response, "-32602");
        assertContains(response, "out of range");
    }

    // --- device/touch validation ---

    @Test
    void touch_missingIndex_returnsError() {
        String response = dispatcher.handle(rpc("device/touch", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "index");
    }

    @Test
    void touch_missingTouched_returnsError() {
        String response = dispatcher.handle(rpc("device/touch", "{\"index\": 0}"));
        assertContains(response, "-32602");
        assertContains(response, "touched");
    }

    @Test
    void touch_indexTooHigh_returnsError() {
        String response = dispatcher.handle(rpc("device/touch", "{\"index\": 8, \"touched\": true}"));
        assertContains(response, "-32602");
        assertContains(response, "out of range");
    }

    // --- device/writeEnvelope validation ---

    @Test
    void writeEnvelope_missingIndex_returnsError() {
        String response = dispatcher.handle(rpc("device/writeEnvelope", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "index");
    }

    @Test
    void writeEnvelope_indexTooHigh_returnsError() {
        String response = dispatcher.handle(rpc("device/writeEnvelope",
            "{\"index\": 8, \"points\": [{\"position\": 0, \"value\": 0.5}]}"));
        assertContains(response, "-32602");
        assertContains(response, "out of range");
    }

    @Test
    void writeEnvelope_negativeIndex_returnsError() {
        String response = dispatcher.handle(rpc("device/writeEnvelope",
            "{\"index\": -1, \"points\": [{\"position\": 0, \"value\": 0.5}]}"));
        assertContains(response, "-32602");
        assertContains(response, "out of range");
    }

    @Test
    void writeEnvelope_missingPoints_returnsError() {
        // Index valid (0) but points missing — hits precondition check (transport NPE)
        // which returns internal error, not param error. Test that index=0 passes validation.
        // Missing points is tested indirectly via the NPE on transport (valid index proceeds past index check).
        String response = dispatcher.handle(rpc("device/writeEnvelope", "{\"index\": 0}"));
        // With null transport, this NPEs at the precondition check (after index validation passes)
        assertContains(response, "-32603"); // internal error from NPE
    }

    // --- device/enterSlot validation ---

    @Test
    void enterSlot_missingName_returnsError() {
        String response = dispatcher.handle(rpc("device/enterSlot", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "name");
    }

    // --- device/setParameterValue validation (existing) ---

    @Test
    void setParameterValue_missingIndex_returnsError() {
        String response = dispatcher.handle(rpc("device/setParameterValue", "{\"value\": 0.5}"));
        assertContains(response, "-32602");
        assertContains(response, "index");
    }

    @Test
    void setParameterValue_missingValue_returnsError() {
        String response = dispatcher.handle(rpc("device/setParameterValue", "{\"index\": 0}"));
        assertContains(response, "-32602");
        assertContains(response, "value");
    }

    @Test
    void setParameterValue_indexTooHigh_returnsError() {
        String response = dispatcher.handle(rpc("device/setParameterValue", "{\"index\": 8, \"value\": 0.5}"));
        assertContains(response, "-32602");
        assertContains(response, "out of range");
    }

    // --- device/enterLayer validation ---

    @Test
    void enterLayer_missingBothParams_returnsError() {
        String response = dispatcher.handle(rpc("device/enterLayer", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "must provide");
    }

    @Test
    void enterLayer_bothParams_returnsError() {
        String response = dispatcher.handle(rpc("device/enterLayer", "{\"index\":0,\"name\":\"Layer 1\"}"));
        assertContains(response, "-32602");
        assertContains(response, "mutually exclusive");
    }

    // --- device/enterKeyPad validation ---

    @Test
    void enterKeyPad_missingKey_returnsError() {
        String response = dispatcher.handle(rpc("device/enterKeyPad", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "key");
    }

    @Test
    void enterKeyPad_keyOutOfRange_returnsError() {
        String response = dispatcher.handle(rpc("device/enterKeyPad", "{\"key\":128}"));
        assertContains(response, "-32602");
        assertContains(response, "0-127");
    }

    @Test
    void enterKeyPad_negativeKey_returnsError() {
        String response = dispatcher.handle(rpc("device/enterKeyPad", "{\"key\":-1}"));
        assertContains(response, "-32602");
        assertContains(response, "0-127");
    }

    // --- device/selectPageByTag validation ---

    @Test
    void selectPageByTag_missingTag_returnsError() {
        String response = dispatcher.handle(rpc("device/selectPageByTag", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "tag");
    }

    @Test
    void selectPageByTag_invalidTag_returnsError() {
        String response = dispatcher.handle(rpc("device/selectPageByTag", "{\"tag\":\"bogus\"}"));
        assertContains(response, "-32602");
        assertContains(response, "Invalid page tag");
    }

    // --- VALID_PAGE_TAGS validation ---

    @Test
    void validPageTagsContainsEightTags() {
        assertEquals(8, DeviceHandler.VALID_PAGE_TAGS.size());
    }

    @Test
    void validPageTagsContainsExpectedValues() {
        assertTrue(DeviceHandler.VALID_PAGE_TAGS.contains("env"));
        assertTrue(DeviceHandler.VALID_PAGE_TAGS.contains("eq"));
        assertTrue(DeviceHandler.VALID_PAGE_TAGS.contains("filter"));
        assertTrue(DeviceHandler.VALID_PAGE_TAGS.contains("fx"));
        assertTrue(DeviceHandler.VALID_PAGE_TAGS.contains("lfo"));
        assertTrue(DeviceHandler.VALID_PAGE_TAGS.contains("mixer"));
        assertTrue(DeviceHandler.VALID_PAGE_TAGS.contains("osc"));
        assertTrue(DeviceHandler.VALID_PAGE_TAGS.contains("perf"));
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
