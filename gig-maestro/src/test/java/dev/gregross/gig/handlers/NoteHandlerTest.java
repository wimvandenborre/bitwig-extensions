package dev.gregross.gig.handlers;

import dev.gregross.gig.extension.StateCache;
import dev.gregross.gig.rpc.JsonRpcDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NoteHandlerTest {

    private JsonRpcDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new JsonRpcDispatcher();
        new NoteHandler(null, new StateCache()).register(dispatcher);
    }

    // --- Registration ---

    @Test
    void registersAllNoteBaseMethods() {
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("clip/setNotes"));
        assertTrue(methods.contains("clip/clearNote"));
        assertTrue(methods.contains("clip/clearAllNotes"));
        assertTrue(methods.contains("clip/getNotes"));
        assertTrue(methods.contains("clip/setChance"));
        assertTrue(methods.contains("clip/setStepSize"));
        assertTrue(methods.contains("clip/scrollSteps"));
    }

    @Test
    void registersSetNoteExpressions() {
        assertTrue(dispatcher.getRegisteredMethods().contains("clip/setNoteExpressions"));
    }

    @Test
    void registersSetNoteRepeat() {
        assertTrue(dispatcher.getRegisteredMethods().contains("clip/setNoteRepeat"));
    }

    @Test
    void registersSetNoteOccurrence() {
        assertTrue(dispatcher.getRegisteredMethods().contains("clip/setNoteOccurrence"));
    }

    @Test
    void registersSetNoteRecurrence() {
        assertTrue(dispatcher.getRegisteredMethods().contains("clip/setNoteRecurrence"));
    }

    @Test
    void registersExactlyElevenMethods() {
        // 7 base + 4 expressive (setNoteExpressions, setNoteRepeat, setNoteOccurrence, setNoteRecurrence)
        // 7 base + 4 expressive (setNoteExpressions, setNoteRepeat, setNoteOccurrence, setNoteRecurrence)
        assertEquals(11, dispatcher.getRegisteredMethods().size());
    }

    // --- NoteOccurrence enum coverage ---

    // --- clip/setNotes validation ---

    @Test
    void setNotes_missingNotes_returnsError() {
        String response = dispatcher.handle(rpc("clip/setNotes", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "notes");
    }

    @Test
    void setNotes_xOutOfRange_returnsError() {
        String response = dispatcher.handle(rpc("clip/setNotes",
            "{\"notes\":[{\"x\":256,\"y\":60,\"velocity\":100,\"duration\":1}]}"));
        assertContains(response, "-32602");
        assertContains(response, "out of range");
    }

    @Test
    void setNotes_yOutOfRange_returnsError() {
        String response = dispatcher.handle(rpc("clip/setNotes",
            "{\"notes\":[{\"x\":0,\"y\":128,\"velocity\":100,\"duration\":1}]}"));
        assertContains(response, "-32602");
        assertContains(response, "out of range");
    }

    // --- clip/setChance validation ---

    @Test
    void setChance_chanceOutOfRange_returnsError() {
        String response = dispatcher.handle(rpc("clip/setChance",
            "{\"notes\":[{\"x\":0,\"y\":60,\"chance\":1.5}]}"));
        assertContains(response, "-32602");
        assertContains(response, "between 0.0 and 1.0");
    }

    // --- clip/setNoteOccurrence validation ---

    @Test
    void setNoteOccurrence_unknownCondition_returnsError() {
        String response = dispatcher.handle(rpc("clip/setNoteOccurrence",
            "{\"notes\":[{\"x\":0,\"y\":60,\"condition\":\"INVALID\"}]}"));
        assertContains(response, "-32602");
        assertContains(response, "unknown occurrence");
    }

    // --- clip/setNoteRepeat validation ---

    @Test
    void setNoteRepeat_countOutOfRange_returnsError() {
        String response = dispatcher.handle(rpc("clip/setNoteRepeat",
            "{\"notes\":[{\"x\":0,\"y\":60,\"count\":200,\"curve\":0.0,\"velocityEnd\":0.0,\"velocityCurve\":0.0}]}"));
        assertContains(response, "-32602");
        assertContains(response, "between -127 and 127");
    }

    // --- clip/setNoteRecurrence validation ---

    @Test
    void setNoteRecurrence_lengthOutOfRange_returnsError() {
        String response = dispatcher.handle(rpc("clip/setNoteRecurrence",
            "{\"notes\":[{\"x\":0,\"y\":60,\"length\":10,\"mask\":1}]}"));
        assertContains(response, "-32602");
        assertContains(response, "between 1 and 8");
    }

    // --- clip/setNoteExpressions validation ---

    @Test
    void setNoteExpressions_missingNotes_returnsError() {
        String response = dispatcher.handle(rpc("clip/setNoteExpressions", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "notes");
    }

    // --- NoteOccurrence enum coverage ---

    @Test
    void noteOccurrenceEnumHasElevenValues() {
        // Verify all expected NoteOccurrence values exist in the Bitwig API
        var values = com.bitwig.extension.controller.api.NoteOccurrence.values();
        assertEquals(11, values.length, "NoteOccurrence should have 11 enum values");
    }

    @Test
    void noteOccurrenceEnumContainsExpectedValues() {
        var names = java.util.Arrays.stream(
            com.bitwig.extension.controller.api.NoteOccurrence.values())
            .map(Enum::name)
            .toList();
        assertTrue(names.contains("ALWAYS"));
        assertTrue(names.contains("FIRST"));
        assertTrue(names.contains("NOT_FIRST"));
        assertTrue(names.contains("PREV"));
        assertTrue(names.contains("NOT_PREV"));
        assertTrue(names.contains("PREV_CHANNEL"));
        assertTrue(names.contains("NOT_PREV_CHANNEL"));
        assertTrue(names.contains("PREV_KEY"));
        assertTrue(names.contains("NOT_PREV_KEY"));
        assertTrue(names.contains("FILL"));
        assertTrue(names.contains("NOT_FILL"));
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
