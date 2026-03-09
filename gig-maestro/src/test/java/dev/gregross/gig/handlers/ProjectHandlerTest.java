package dev.gregross.gig.handlers;

import com.bitwig.extension.controller.api.Project;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.gregross.gig.extension.StateCache;
import dev.gregross.gig.rpc.JsonRpcDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProjectHandlerTest {

    @Mock private Project mockProject;

    private JsonRpcDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new JsonRpcDispatcher();
        new ProjectHandler(mockProject, new StateCache()).register(dispatcher);
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

    // --- Behavioral tests (Mockito) ---

    @Test
    void unsoloAll_callsProjectUnsolo() {
        dispatcher.handle(rpc("project/unsoloAll", "{}"));
        verify(mockProject).unsoloAll();
    }

    @Test
    void unmuteAll_callsProjectUnmute() {
        dispatcher.handle(rpc("project/unmuteAll", "{}"));
        verify(mockProject).unmuteAll();
    }

    @Test
    void unarmAll_callsProjectUnarm() {
        dispatcher.handle(rpc("project/unarmAll", "{}"));
        verify(mockProject).unarmAll();
    }

    // --- Helpers ---

    private String rpc(String method, String params) {
        return "{\"jsonrpc\":\"2.0\",\"method\":\"" + method + "\",\"params\":" + params + ",\"id\":1}";
    }
}
