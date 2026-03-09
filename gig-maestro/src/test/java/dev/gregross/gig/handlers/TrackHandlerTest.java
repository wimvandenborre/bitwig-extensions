package dev.gregross.gig.handlers;

import dev.gregross.gig.extension.StateCache;
import dev.gregross.gig.rpc.JsonRpcDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TrackHandlerTest {

    private JsonRpcDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new JsonRpcDispatcher();
        new TrackHandler(null, null, null, null, new StateCache(), null).register(dispatcher);
    }

    // --- Registration ---

    @Test
    void registersTrackMethods() {
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("track/setVolume"));
        assertTrue(methods.contains("track/setPan"));
        assertTrue(methods.contains("track/setMute"));
        assertTrue(methods.contains("track/setSolo"));
        assertTrue(methods.contains("track/setArm"));
        assertTrue(methods.contains("track/createAudio"));
        assertTrue(methods.contains("track/createInstrument"));
        assertTrue(methods.contains("track/createEffect"));
        assertTrue(methods.contains("track/select"));
        assertTrue(methods.contains("track/rename"));
        assertTrue(methods.contains("track/deleteSelected"));
        assertTrue(methods.contains("track/duplicate"));
    }

    @Test
    void registersThreeScrollMethods() {
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("trackBank/scrollTo"));
        assertTrue(methods.contains("trackBank/scrollBy"));
        assertTrue(methods.contains("trackBank/getScrollInfo"));
    }

    @Test
    void registersMixerMethods() {
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("track/setColor"));
        assertTrue(methods.contains("track/setCrossfade"));
        assertTrue(methods.contains("track/setMonitor"));
    }

    @Test
    void registersGroupAndRoutingMethods() {
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("track/setGroupExpanded"));
        assertTrue(methods.contains("track/navigateInto"));
        assertTrue(methods.contains("track/navigateToParent"));
        assertTrue(methods.contains("track/createGroup"));
        assertTrue(methods.contains("track/addNoteSource"));
        assertTrue(methods.contains("track/removeNoteSource"));
    }

    @Test
    void registersExactlyTwentyFourMethods() {
        assertEquals(24, dispatcher.getRegisteredMethods().size());
    }

    // --- trackBank/scrollTo validation ---

    @Test
    void trackBankScrollTo_missingPosition_returnsError() {
        String response = dispatcher.handle(rpc("trackBank/scrollTo", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "position");
    }

    @Test
    void trackBankScrollTo_negativePosition_returnsOutOfRange() {
        String response = dispatcher.handle(rpc("trackBank/scrollTo", "{\"position\": -1}"));
        assertContains(response, "-32001");
        assertContains(response, "POSITION_OUT_OF_RANGE");
    }

    @Test
    void trackBankScrollTo_positionBeyondItemCount_returnsOutOfRange() {
        // StateCache defaults to itemCount=0, so position 0 is out of range
        String response = dispatcher.handle(rpc("trackBank/scrollTo", "{\"position\": 0}"));
        assertContains(response, "-32001");
        assertContains(response, "itemCount");
        assertContains(response, "requestedPosition");
    }

    // --- trackBank/scrollBy validation ---

    @Test
    void trackBankScrollBy_missingAmount_returnsError() {
        String response = dispatcher.handle(rpc("trackBank/scrollBy", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "amount");
    }

    // --- trackBank/getScrollInfo ---

    @Test
    void trackBankGetScrollInfo_returnsAllFields() {
        String response = dispatcher.handle(rpc("trackBank/getScrollInfo", "{}"));
        assertContains(response, "scrollPosition");
        assertContains(response, "itemCount");
        assertContains(response, "bankSize");
        assertContains(response, "canScrollForwards");
        assertContains(response, "canScrollBackwards");
    }

    // --- track/setGroupExpanded validation ---

    @Test
    void setGroupExpanded_missingBothParams_returnsError() {
        String response = dispatcher.handle(rpc("track/setGroupExpanded", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "must provide");
    }

    @Test
    void setGroupExpanded_bothParams_returnsError() {
        String response = dispatcher.handle(rpc("track/setGroupExpanded", "{\"expanded\":true,\"toggle\":true}"));
        assertContains(response, "-32602");
        assertContains(response, "mutually exclusive");
    }

    // --- track/select validation ---

    @Test
    void select_missingIndex_returnsError() {
        String response = dispatcher.handle(rpc("track/select", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "index");
    }

    // --- track/setCrossfade validation ---

    @Test
    void setCrossfade_missingIndex_returnsError() {
        String response = dispatcher.handle(rpc("track/setCrossfade", "{\"mode\":\"A\"}"));
        assertContains(response, "-32602");
        assertContains(response, "index");
    }

    // --- track/setMonitor validation ---

    @Test
    void setMonitor_missingIndex_returnsError() {
        String response = dispatcher.handle(rpc("track/setMonitor", "{\"mode\":\"ON\"}"));
        assertContains(response, "-32602");
        assertContains(response, "index");
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
