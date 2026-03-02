package dev.gregross.gig.handlers;

import dev.gregross.gig.rpc.JsonRpcDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BrowserHandlerTest {

    private JsonRpcDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new JsonRpcDispatcher();
        new BrowserHandler(null, null, null).register(dispatcher);
    }

    // --- Registration ---

    @Test
    void registersElevenMethods() {
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("browser/browsePresets"));
        assertTrue(methods.contains("browser/browseInsertDevice"));
        assertTrue(methods.contains("browser/selectNextFile"));
        assertTrue(methods.contains("browser/selectPreviousFile"));
        assertTrue(methods.contains("browser/selectFirstFile"));
        assertTrue(methods.contains("browser/selectLastFile"));
        assertTrue(methods.contains("browser/commit"));
        assertTrue(methods.contains("browser/cancel"));
        assertTrue(methods.contains("browser/setContentType"));
        assertTrue(methods.contains("browser/setShouldAudition"));
        assertTrue(methods.contains("browser/getState"));
        assertEquals(11, methods.size());
    }

    // --- setContentType validation ---

    @Test
    void setContentType_missingIndex_returnsError() {
        String response = dispatcher.handle(rpc("browser/setContentType", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "index");
    }

    // --- setShouldAudition validation ---

    @Test
    void setShouldAudition_missingEnabled_returnsError() {
        String response = dispatcher.handle(rpc("browser/setShouldAudition", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "enabled");
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
