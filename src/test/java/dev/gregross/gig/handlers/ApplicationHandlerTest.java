package dev.gregross.gig.handlers;

import dev.gregross.gig.rpc.JsonRpcDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationHandlerTest {

    private JsonRpcDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new JsonRpcDispatcher();
        new ApplicationHandler(null, null).register(dispatcher);
    }

    // --- Registration ---

    @Test
    void registersSevenMethods() {
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("app/undo"));
        assertTrue(methods.contains("app/redo"));
        assertTrue(methods.contains("app/getState"));
        assertTrue(methods.contains("app/activateEngine"));
        assertTrue(methods.contains("app/deactivateEngine"));
        assertTrue(methods.contains("app/showNotification"));
        assertTrue(methods.contains("app/setPanelLayout"));
        assertEquals(7, methods.size());
    }

    // --- showNotification validation ---

    @Test
    void showNotification_missingText_returnsError() {
        String response = dispatcher.handle(rpc("app/showNotification", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "text");
    }

    // --- setPanelLayout validation ---

    @Test
    void setPanelLayout_missingLayout_returnsError() {
        String response = dispatcher.handle(rpc("app/setPanelLayout", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "layout");
    }

    @Test
    void setPanelLayout_invalidLayout_returnsError() {
        String response = dispatcher.handle(rpc("app/setPanelLayout",
            "{\"layout\": \"INVALID\"}"));
        assertContains(response, "-32602");
        assertContains(response, "INVALID");
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
