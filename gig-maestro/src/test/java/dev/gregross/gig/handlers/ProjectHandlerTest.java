package dev.gregross.gig.handlers;

import dev.gregross.gig.rpc.JsonRpcDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProjectHandlerTest {

    private JsonRpcDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new JsonRpcDispatcher();
        new ProjectHandler(null, null).register(dispatcher);
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
}
