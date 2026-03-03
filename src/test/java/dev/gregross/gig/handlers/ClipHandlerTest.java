package dev.gregross.gig.handlers;

import dev.gregross.gig.extension.StateCache;
import dev.gregross.gig.rpc.JsonRpcDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClipHandlerTest {

    private JsonRpcDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new JsonRpcDispatcher();
        // Pass null for Bitwig API objects — we only test parameter validation
        new ClipHandler(null, null, null, new StateCache()).register(dispatcher);
    }

    // --- Registration ---

    @Test
    void registersAllClipMethods() {
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("clip/launch"));
        assertTrue(methods.contains("clip/stop"));
        assertTrue(methods.contains("clip/record"));
        assertTrue(methods.contains("clip/create"));
        assertTrue(methods.contains("clip/select"));
        assertTrue(methods.contains("clip/delete"));
        assertTrue(methods.contains("clip/rename"));
        assertTrue(methods.contains("clip/duplicate"));
        assertTrue(methods.contains("clip/duplicateToSlot"));
        assertTrue(methods.contains("scene/launch"));
    }

    @Test
    void registersExactlyThirtyMethods() {
        // 10 original + 6 launch settings + 14 grid enhancements
        assertEquals(30, dispatcher.getRegisteredMethods().size());
    }

    @Test
    void registersClipLaunchSettingsMethods() {
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("clip/setLaunchQuantization"));
        assertTrue(methods.contains("clip/setLaunchMode"));
        assertTrue(methods.contains("clip/setShuffle"));
        assertTrue(methods.contains("clip/setAccent"));
        assertTrue(methods.contains("clip/setUseLoopStartAsQuantizationReference"));
        assertTrue(methods.contains("clip/getLaunchSettings"));
    }

    @Test
    void registersGridEnhancementMethods() {
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("clip/setColor"));
        assertTrue(methods.contains("clip/setPlayStart"));
        assertTrue(methods.contains("clip/setPlayStop"));
        assertTrue(methods.contains("clip/setLoopStart"));
        assertTrue(methods.contains("clip/setLoopLength"));
        assertTrue(methods.contains("clip/setLoopEnabled"));
        assertTrue(methods.contains("clip/getPlaybackSettings"));
        assertTrue(methods.contains("clip/quantize"));
        assertTrue(methods.contains("clip/transpose"));
        assertTrue(methods.contains("clip/duplicateContent"));
        assertTrue(methods.contains("clip/showInEditor"));
        assertTrue(methods.contains("clip/launchAlt"));
        assertTrue(methods.contains("clip/launchRelease"));
        assertTrue(methods.contains("clip/launchReleaseAlt"));
    }

    // --- clip/select validation ---

    @Test
    void clipSelect_emptySlot_returnsError() {
        // StateCache defaults to all slots empty (hasContent=false)
        String response = dispatcher.handle(rpc("clip/select", "{\"trackIndex\": 0, \"slotIndex\": 0}"));
        assertContains(response, "-32602");
        assertContains(response, "slot is empty");
    }

    // --- clip/rename validation ---

    @Test
    void clipRename_missingName_returnsError() {
        String response = dispatcher.handle(rpc("clip/rename", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "name");
    }

    // --- clip/duplicate validation ---

    @Test
    void clipDuplicate_missingTrackIndex_returnsError() {
        String response = dispatcher.handle(rpc("clip/duplicate", "{\"slotIndex\": 0}"));
        assertContains(response, "-32602");
        assertContains(response, "trackIndex");
    }

    @Test
    void clipDuplicate_missingSlotIndex_returnsError() {
        String response = dispatcher.handle(rpc("clip/duplicate", "{\"trackIndex\": 0}"));
        assertContains(response, "-32602");
        assertContains(response, "slotIndex");
    }

    // --- clip/duplicateToSlot validation ---

    @Test
    void clipDuplicateToSlot_missingSrcTrackIndex_returnsError() {
        String response = dispatcher.handle(rpc("clip/duplicateToSlot",
            "{\"srcSlotIndex\": 0, \"destTrackIndex\": 1, \"destSlotIndex\": 0}"));
        assertContains(response, "-32602");
        assertContains(response, "srcTrackIndex");
    }

    @Test
    void clipDuplicateToSlot_missingDestSlotIndex_returnsError() {
        String response = dispatcher.handle(rpc("clip/duplicateToSlot",
            "{\"srcTrackIndex\": 0, \"srcSlotIndex\": 0, \"destTrackIndex\": 1}"));
        assertContains(response, "-32602");
        assertContains(response, "destSlotIndex");
    }

    // --- clip/setLaunchQuantization validation ---

    @Test
    void setLaunchQuantization_missingParam_returnsError() {
        String response = dispatcher.handle(rpc("clip/setLaunchQuantization", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "quantization");
    }

    @Test
    void setLaunchQuantization_invalidValue_returnsError() {
        String response = dispatcher.handle(rpc("clip/setLaunchQuantization", "{\"quantization\": \"invalid\"}"));
        assertContains(response, "-32602");
        assertContains(response, "invalid launch quantization");
    }

    // --- clip/setLaunchMode validation ---

    @Test
    void setLaunchMode_missingParam_returnsError() {
        String response = dispatcher.handle(rpc("clip/setLaunchMode", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "launchMode");
    }

    @Test
    void setLaunchMode_invalidValue_returnsError() {
        String response = dispatcher.handle(rpc("clip/setLaunchMode", "{\"launchMode\": \"bad\"}"));
        assertContains(response, "-32602");
        assertContains(response, "invalid launch mode");
    }

    // --- clip/setAccent validation ---

    @Test
    void setAccent_missingParam_returnsError() {
        String response = dispatcher.handle(rpc("clip/setAccent", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "value");
    }

    @Test
    void setAccent_outOfRange_returnsError() {
        String response = dispatcher.handle(rpc("clip/setAccent", "{\"value\": 1.5}"));
        assertContains(response, "-32602");
        assertContains(response, "between 0.0 and 1.0");
    }

    // --- clip/launch with options validation ---

    @Test
    void clipLaunch_partialOptions_returnsError() {
        String response = dispatcher.handle(rpc("clip/launch",
            "{\"trackIndex\": 0, \"slotIndex\": 0, \"quantization\": \"1\"}"));
        assertContains(response, "-32602");
        assertContains(response, "both be provided");
    }

    // --- clip/setColor validation ---

    @Test
    void setColor_missingTrackIndex_returnsError() {
        String response = dispatcher.handle(rpc("clip/setColor",
            "{\"slotIndex\": 0, \"r\": 0.5, \"g\": 0.5, \"b\": 0.5}"));
        assertContains(response, "-32602");
        assertContains(response, "trackIndex");
    }

    @Test
    void setColor_rOutOfRange_returnsError() {
        String response = dispatcher.handle(rpc("clip/setColor",
            "{\"trackIndex\": 0, \"slotIndex\": 0, \"r\": 1.5, \"g\": 0.5, \"b\": 0.5}"));
        assertContains(response, "-32602");
        assertContains(response, "between 0.0 and 1.0");
    }

    // --- clip/quantize validation ---

    @Test
    void quantize_missingAmount_returnsError() {
        String response = dispatcher.handle(rpc("clip/quantize", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "amount");
    }

    @Test
    void quantize_outOfRange_returnsError() {
        String response = dispatcher.handle(rpc("clip/quantize", "{\"amount\": 1.5}"));
        assertContains(response, "-32602");
        assertContains(response, "between 0.0 and 1.0");
    }

    // --- clip/transpose validation ---

    @Test
    void transpose_missingSemitones_returnsError() {
        String response = dispatcher.handle(rpc("clip/transpose", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "semitones");
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
