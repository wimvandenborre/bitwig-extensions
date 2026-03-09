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
    void registersTwentyOneMethods() {
        var methods = dispatcher.getRegisteredMethods();
        // Phase 17 — 11 methods
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
        // Phase 18 — 10 methods
        assertTrue(methods.contains("browser/filterSelectNext"));
        assertTrue(methods.contains("browser/filterSelectPrevious"));
        assertTrue(methods.contains("browser/filterSelectFirst"));
        assertTrue(methods.contains("browser/filterSelectLast"));
        assertTrue(methods.contains("browser/filterSelectParent"));
        assertTrue(methods.contains("browser/filterSelectFirstChild"));
        assertTrue(methods.contains("browser/filterReset"));
        assertTrue(methods.contains("browser/getFilters"));
        assertTrue(methods.contains("browser/getResults"));
        assertTrue(methods.contains("browser/scrollResults"));
        assertEquals(21, methods.size());
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

    // --- Filter validation ---

    @Test
    void filterSelectNext_missingColumn_returnsError() {
        String response = dispatcher.handle(rpc("browser/filterSelectNext", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "column");
    }

    @Test
    void filterSelectNext_invalidColumn_returnsError() {
        String response = dispatcher.handle(rpc("browser/filterSelectNext",
            "{\"column\": \"invalid\"}"));
        assertContains(response, "-32602");
        assertContains(response, "invalid");
    }

    @Test
    void filterReset_missingColumn_returnsError() {
        String response = dispatcher.handle(rpc("browser/filterReset", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "column");
    }

    // --- scrollResults validation ---

    @Test
    void scrollResults_missingDirection_returnsError() {
        String response = dispatcher.handle(rpc("browser/scrollResults", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "direction");
    }

    @Test
    void scrollResults_invalidDirection_returnsError() {
        String response = dispatcher.handle(rpc("browser/scrollResults",
            "{\"direction\": \"sideways\"}"));
        assertContains(response, "-32602");
        assertContains(response, "sideways");
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
