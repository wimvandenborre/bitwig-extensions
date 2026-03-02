package dev.gregross.gig.handlers;

import dev.gregross.gig.rpc.JsonRpcDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MasterHandlerTest {

    private JsonRpcDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new JsonRpcDispatcher();
        new MasterHandler(null).register(dispatcher);
    }

    // --- Registration ---

    @Test
    void registersFiveMasterMethods() {
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("master/setVolume"));
        assertTrue(methods.contains("master/setPan"));
        assertTrue(methods.contains("master/setMute"));
        assertTrue(methods.contains("master/setSolo"));
        assertTrue(methods.contains("master/setColor"));
        assertEquals(5, methods.size());
    }

    // --- master/setMute validation ---

    @Test
    void masterSetMute_missingValue_returnsError() {
        String response = dispatcher.handle(rpc("master/setMute", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "value");
    }

    // --- master/setSolo validation ---

    @Test
    void masterSetSolo_missingValue_returnsError() {
        String response = dispatcher.handle(rpc("master/setSolo", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "value");
    }

    // --- master/setColor validation ---

    @Test
    void masterSetColor_missingParams_returnsError() {
        String response = dispatcher.handle(rpc("master/setColor", "{}"));
        assertContains(response, "error");
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
