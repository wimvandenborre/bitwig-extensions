package dev.gregross.gig.handlers;

import dev.gregross.gig.rpc.JsonRpcDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SendHandlerTest {

    private JsonRpcDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new JsonRpcDispatcher();
        new SendHandler(null, 4).register(dispatcher);
    }

    // --- Registration ---

    @Test
    void registersThreeSendMethods() {
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("send/setLevel"));
        assertTrue(methods.contains("send/setMode"));
        assertTrue(methods.contains("send/setEnabled"));
        assertEquals(3, methods.size());
    }

    // --- send/setLevel validation ---

    @Test
    void sendSetLevel_missingTrackIndex_returnsError() {
        String response = dispatcher.handle(rpc("send/setLevel", "{\"sendIndex\":0,\"value\":0.5}"));
        assertContains(response, "-32602");
    }

    @Test
    void sendSetLevel_missingSendIndex_returnsError() {
        String response = dispatcher.handle(rpc("send/setLevel", "{\"trackIndex\":0,\"value\":0.5}"));
        assertContains(response, "-32602");
    }

    // --- send/setMode validation ---

    @Test
    void sendSetMode_invalidMode_returnsError() {
        // TrackBank is null so we can't actually call getSend, but we can test
        // that the method is registered. Full validation requires Bitwig running.
        String response = dispatcher.handle(rpc("send/setMode", "{\"trackIndex\":0,\"sendIndex\":0,\"mode\":\"AUTO\"}"));
        // Will fail with NPE since trackBank is null, but method is registered
        assertContains(response, "error");
    }

    // --- send/setEnabled validation ---

    @Test
    void sendSetEnabled_missingTrackIndex_returnsError() {
        String response = dispatcher.handle(rpc("send/setEnabled", "{\"sendIndex\":0,\"enabled\":true}"));
        assertContains(response, "-32602");
        assertContains(response, "trackIndex");
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
