package dev.gregross.gig.handlers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.gregross.gig.extension.StateCache;
import dev.gregross.gig.rpc.JsonRpcDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProjectHandlerTest {

    private JsonRpcDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new JsonRpcDispatcher();
        new ProjectHandler(null, new StateCache()).register(dispatcher);
    }

    // --- Registration ---

    @Test
    void registersFourMethods() {
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("project/unsoloAll"));
        assertTrue(methods.contains("project/unmuteAll"));
        assertTrue(methods.contains("project/unarmAll"));
        assertTrue(methods.contains("project/getState"));
        assertEquals(4, methods.size());
    }

    // --- project/getState ---

    @Test
    void getState_returnsStateCacheData() {
        String response = dispatcher.handle(
            "{\"jsonrpc\":\"2.0\",\"method\":\"project/getState\",\"params\":{},\"id\":1}");
        JsonObject result = JsonParser.parseString(response).getAsJsonObject()
            .getAsJsonObject("result");
        assertNotNull(result);
        assertTrue(result.has("hasSoloedTracks"));
        assertTrue(result.has("hasMutedTracks"));
        assertTrue(result.has("hasArmedTracks"));
        assertTrue(result.has("isModified"));
    }
}
