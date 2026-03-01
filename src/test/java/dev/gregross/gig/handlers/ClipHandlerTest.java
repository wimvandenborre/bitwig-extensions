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
    void registersExactlyTenMethods() {
        assertEquals(10, dispatcher.getRegisteredMethods().size());
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

    // --- Helpers ---

    private String rpc(String method, String params) {
        return "{\"jsonrpc\":\"2.0\",\"method\":\"" + method + "\",\"params\":" + params + ",\"id\":1}";
    }

    private void assertContains(String actual, String expected) {
        assertTrue(actual.contains(expected),
            "Expected '" + expected + "' in: " + actual);
    }
}
