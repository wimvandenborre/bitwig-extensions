package dev.gregross.gig.handlers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.gregross.gig.extension.StateCache;
import dev.gregross.gig.rpc.JsonRpcDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArrangerHandlerTest {

    private JsonRpcDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new JsonRpcDispatcher();
        new ArrangerHandler(null, null, null, new StateCache()).register(dispatcher);
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
    void registersSevenCueMarkerMethods() {
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("cueMarker/addAtPlayhead"));
        assertTrue(methods.contains("cueMarker/list"));
        assertTrue(methods.contains("cueMarker/launch"));
        assertTrue(methods.contains("cueMarker/delete"));
        assertTrue(methods.contains("cueMarker/rename"));
        assertTrue(methods.contains("cueMarker/setPosition"));
        assertTrue(methods.contains("cueMarker/duplicate"));
    }

    @Test
    void registersExactlySeventeenMethods() {
        assertEquals(17, dispatcher.getRegisteredMethods().size());
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

    // --- Cue marker rename validation ---

    @Test
    void cueMarkerRename_missingIndex_returnsError() {
        String response = dispatcher.handle(rpc("cueMarker/rename", "{\"name\": \"Test\"}"));
        assertContains(response, "-32602");
        assertContains(response, "index");
    }

    @Test
    void cueMarkerRename_missingName_returnsError() {
        String response = dispatcher.handle(rpc("cueMarker/rename", "{\"index\": 0}"));
        assertContains(response, "-32602");
        assertContains(response, "name");
    }

    @Test
    void cueMarkerRename_negativeIndex_returnsError() {
        String response = dispatcher.handle(rpc("cueMarker/rename", "{\"index\": -1, \"name\": \"Test\"}"));
        assertContains(response, "-32602");
    }

    @Test
    void cueMarkerRename_indexTooHigh_returnsError() {
        String response = dispatcher.handle(rpc("cueMarker/rename", "{\"index\": 16, \"name\": \"Test\"}"));
        assertContains(response, "-32602");
    }

    // --- Cue marker setPosition validation ---

    @Test
    void cueMarkerSetPosition_missingIndex_returnsError() {
        String response = dispatcher.handle(rpc("cueMarker/setPosition", "{\"beats\": 4.0}"));
        assertContains(response, "-32602");
        assertContains(response, "index");
    }

    @Test
    void cueMarkerSetPosition_missingBeats_returnsError() {
        String response = dispatcher.handle(rpc("cueMarker/setPosition", "{\"index\": 0}"));
        assertContains(response, "-32602");
        assertContains(response, "beats");
    }

    @Test
    void cueMarkerSetPosition_negativeIndex_returnsError() {
        String response = dispatcher.handle(rpc("cueMarker/setPosition", "{\"index\": -1, \"beats\": 4.0}"));
        assertContains(response, "-32602");
    }

    @Test
    void cueMarkerSetPosition_indexTooHigh_returnsError() {
        String response = dispatcher.handle(rpc("cueMarker/setPosition", "{\"index\": 16, \"beats\": 4.0}"));
        assertContains(response, "-32602");
    }

    // --- Cue marker duplicate validation ---

    @Test
    void cueMarkerDuplicate_missingIndex_returnsError() {
        String response = dispatcher.handle(rpc("cueMarker/duplicate", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "index");
    }

    @Test
    void cueMarkerDuplicate_negativeIndex_returnsError() {
        String response = dispatcher.handle(rpc("cueMarker/duplicate", "{\"index\": -1}"));
        assertContains(response, "-32602");
    }

    @Test
    void cueMarkerDuplicate_indexTooHigh_returnsError() {
        String response = dispatcher.handle(rpc("cueMarker/duplicate", "{\"index\": 16}"));
        assertContains(response, "-32602");
    }

    // --- cueMarkerBank scroll registration ---

    @Test
    void registersThreeCueMarkerBankScrollMethods() {
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("cueMarkerBank/scrollTo"));
        assertTrue(methods.contains("cueMarkerBank/scrollBy"));
        assertTrue(methods.contains("cueMarkerBank/getScrollInfo"));
    }

    // --- cueMarkerBank/scrollTo validation ---

    @Test
    void cueMarkerBankScrollTo_missingPosition_returnsError() {
        String response = dispatcher.handle(rpc("cueMarkerBank/scrollTo", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "position");
    }

    @Test
    void cueMarkerBankScrollTo_negativePosition_returnsOutOfRange() {
        String response = dispatcher.handle(rpc("cueMarkerBank/scrollTo", "{\"position\": -1}"));
        assertContains(response, "-32001");
        assertContains(response, "POSITION_OUT_OF_RANGE");
    }

    @Test
    void cueMarkerBankScrollTo_positionBeyondItemCount_returnsOutOfRange() {
        // StateCache defaults to itemCount=0, so position 0 is out of range
        String response = dispatcher.handle(rpc("cueMarkerBank/scrollTo", "{\"position\": 0}"));
        assertContains(response, "-32001");
        assertContains(response, "itemCount");
        assertContains(response, "requestedPosition");
    }

    // --- cueMarkerBank/scrollBy validation ---

    @Test
    void cueMarkerBankScrollBy_missingAmount_returnsError() {
        String response = dispatcher.handle(rpc("cueMarkerBank/scrollBy", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "amount");
    }

    // --- cueMarkerBank/getScrollInfo ---

    @Test
    void cueMarkerBankGetScrollInfo_returnsAllFields() {
        String response = dispatcher.handle(rpc("cueMarkerBank/getScrollInfo", "{}"));
        assertContains(response, "scrollPosition");
        assertContains(response, "itemCount");
        assertContains(response, "bankSize");
        assertContains(response, "canScrollForwards");
        assertContains(response, "canScrollBackwards");
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
