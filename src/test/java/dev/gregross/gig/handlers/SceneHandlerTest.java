package dev.gregross.gig.handlers;

import dev.gregross.gig.extension.StateCache;
import dev.gregross.gig.rpc.JsonRpcDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SceneHandlerTest {

    private JsonRpcDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new JsonRpcDispatcher();
        // StateCache with defaults (itemCount=0) — sufficient for param validation tests
        new SceneHandler(null, null, new StateCache()).register(dispatcher);
    }

    // --- Registration ---

    @Test
    void registersFiveSceneMethods() {
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("scene/create"));
        assertTrue(methods.contains("scene/createFromPlaying"));
        assertTrue(methods.contains("scene/duplicate"));
        assertTrue(methods.contains("scene/rename"));
        assertTrue(methods.contains("scene/delete"));
    }

    @Test
    void registersThreeScrollMethods() {
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("sceneBank/scrollTo"));
        assertTrue(methods.contains("sceneBank/scrollBy"));
        assertTrue(methods.contains("sceneBank/getScrollInfo"));
    }

    @Test
    void registersExactlyEightMethods() {
        assertEquals(8, dispatcher.getRegisteredMethods().size());
    }

    // --- scene/duplicate validation ---

    @Test
    void sceneDuplicate_missingIndex_returnsError() {
        String response = dispatcher.handle(rpc("scene/duplicate", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "index");
    }

    @Test
    void sceneDuplicate_negativeIndex_returnsError() {
        String response = dispatcher.handle(rpc("scene/duplicate", "{\"index\": -1}"));
        assertContains(response, "-32602");
    }

    @Test
    void sceneDuplicate_indexTooHigh_returnsError() {
        String response = dispatcher.handle(rpc("scene/duplicate", "{\"index\": 8}"));
        assertContains(response, "-32602");
    }

    // --- scene/rename validation ---

    @Test
    void sceneRename_missingIndex_returnsError() {
        String response = dispatcher.handle(rpc("scene/rename", "{\"name\": \"Test\"}"));
        assertContains(response, "-32602");
        assertContains(response, "index");
    }

    @Test
    void sceneRename_missingName_returnsError() {
        String response = dispatcher.handle(rpc("scene/rename", "{\"index\": 0}"));
        assertContains(response, "-32602");
        assertContains(response, "name");
    }

    @Test
    void sceneRename_negativeIndex_returnsError() {
        String response = dispatcher.handle(rpc("scene/rename", "{\"index\": -1, \"name\": \"Test\"}"));
        assertContains(response, "-32602");
    }

    @Test
    void sceneRename_indexTooHigh_returnsError() {
        String response = dispatcher.handle(rpc("scene/rename", "{\"index\": 8, \"name\": \"Test\"}"));
        assertContains(response, "-32602");
    }

    // --- scene/delete validation ---

    @Test
    void sceneDelete_missingIndex_returnsError() {
        String response = dispatcher.handle(rpc("scene/delete", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "index");
    }

    @Test
    void sceneDelete_negativeIndex_returnsError() {
        String response = dispatcher.handle(rpc("scene/delete", "{\"index\": -1}"));
        assertContains(response, "-32602");
    }

    @Test
    void sceneDelete_indexTooHigh_returnsError() {
        String response = dispatcher.handle(rpc("scene/delete", "{\"index\": 8}"));
        assertContains(response, "-32602");
    }

    // --- sceneBank/scrollTo validation ---

    @Test
    void sceneBankScrollTo_missingPosition_returnsError() {
        String response = dispatcher.handle(rpc("sceneBank/scrollTo", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "position");
    }

    @Test
    void sceneBankScrollTo_negativePosition_returnsOutOfRange() {
        String response = dispatcher.handle(rpc("sceneBank/scrollTo", "{\"position\": -1}"));
        assertContains(response, "-32001");
        assertContains(response, "POSITION_OUT_OF_RANGE");
    }

    @Test
    void sceneBankScrollTo_positionBeyondItemCount_returnsOutOfRange() {
        // StateCache defaults to itemCount=0, so position 0 is out of range
        String response = dispatcher.handle(rpc("sceneBank/scrollTo", "{\"position\": 0}"));
        assertContains(response, "-32001");
        assertContains(response, "itemCount");
        assertContains(response, "requestedPosition");
    }

    // --- sceneBank/scrollBy validation ---

    @Test
    void sceneBankScrollBy_missingAmount_returnsError() {
        String response = dispatcher.handle(rpc("sceneBank/scrollBy", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "amount");
    }

    // --- sceneBank/getScrollInfo ---

    @Test
    void sceneBankGetScrollInfo_returnsAllFields() {
        String response = dispatcher.handle(rpc("sceneBank/getScrollInfo", "{}"));
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
