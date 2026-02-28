package dev.gregross.gig.handlers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.gregross.gig.rpc.JsonRpcDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArrangerHandlerTest {

    private JsonRpcDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        // Pass null for Bitwig API objects — we only test parameter validation,
        // not actual API calls (those would NPE on null objects)
        dispatcher = new JsonRpcDispatcher();
        new ArrangerHandler(null, null, null).register(dispatcher);
    }

    // --- Registration ---

    @Test
    void registersAllSevenVisibilityMethods() {
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("arranger/setPlaybackFollow"));
        assertTrue(methods.contains("arranger/setClipLauncherVisible"));
        assertTrue(methods.contains("arranger/setTimelineVisible"));
        assertTrue(methods.contains("arranger/setCueMarkersVisible"));
        assertTrue(methods.contains("arranger/setEffectTracksVisible"));
        assertTrue(methods.contains("arranger/setIoSectionVisible"));
        assertTrue(methods.contains("arranger/setDoubleRowTrackHeight"));
    }

    @Test
    void registersFourCueMarkerMethods() {
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("cueMarker/addAtPlayhead"));
        assertTrue(methods.contains("cueMarker/list"));
        assertTrue(methods.contains("cueMarker/launch"));
        assertTrue(methods.contains("cueMarker/delete"));
    }

    @Test
    void registersExactlyElevenMethods() {
        assertEquals(11, dispatcher.getRegisteredMethods().size());
    }

    // --- Visibility toggle validation ---

    @Test
    void setPlaybackFollow_missingEnabled_returnsError() {
        String response = dispatcher.handle(rpc("arranger/setPlaybackFollow", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "enabled");
    }

    @Test
    void setClipLauncherVisible_missingEnabled_returnsError() {
        String response = dispatcher.handle(rpc("arranger/setClipLauncherVisible", "{}"));
        assertContains(response, "-32602");
    }

    @Test
    void setTimelineVisible_missingEnabled_returnsError() {
        String response = dispatcher.handle(rpc("arranger/setTimelineVisible", "{}"));
        assertContains(response, "-32602");
    }

    @Test
    void setCueMarkersVisible_missingEnabled_returnsError() {
        String response = dispatcher.handle(rpc("arranger/setCueMarkersVisible", "{}"));
        assertContains(response, "-32602");
    }

    @Test
    void setEffectTracksVisible_missingEnabled_returnsError() {
        String response = dispatcher.handle(rpc("arranger/setEffectTracksVisible", "{}"));
        assertContains(response, "-32602");
    }

    @Test
    void setIoSectionVisible_missingEnabled_returnsError() {
        String response = dispatcher.handle(rpc("arranger/setIoSectionVisible", "{}"));
        assertContains(response, "-32602");
    }

    @Test
    void setDoubleRowTrackHeight_missingEnabled_returnsError() {
        String response = dispatcher.handle(rpc("arranger/setDoubleRowTrackHeight", "{}"));
        assertContains(response, "-32602");
    }

    // --- Cue marker validation ---

    @Test
    void cueMarkerLaunch_missingIndex_returnsError() {
        String response = dispatcher.handle(rpc("cueMarker/launch", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "index");
    }

    @Test
    void cueMarkerLaunch_negativeIndex_returnsError() {
        String response = dispatcher.handle(rpc("cueMarker/launch", "{\"index\": -1}"));
        assertContains(response, "-32602");
    }

    @Test
    void cueMarkerLaunch_indexTooHigh_returnsError() {
        String response = dispatcher.handle(rpc("cueMarker/launch", "{\"index\": 16}"));
        assertContains(response, "-32602");
    }

    @Test
    void cueMarkerDelete_missingIndex_returnsError() {
        String response = dispatcher.handle(rpc("cueMarker/delete", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "index");
    }

    @Test
    void cueMarkerDelete_negativeIndex_returnsError() {
        String response = dispatcher.handle(rpc("cueMarker/delete", "{\"index\": -1}"));
        assertContains(response, "-32602");
    }

    @Test
    void cueMarkerDelete_indexTooHigh_returnsError() {
        String response = dispatcher.handle(rpc("cueMarker/delete", "{\"index\": 16}"));
        assertContains(response, "-32602");
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
