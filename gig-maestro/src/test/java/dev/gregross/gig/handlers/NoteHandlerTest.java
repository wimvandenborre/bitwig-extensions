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
        assertEquals(11, dispatcher.getRegisteredMethods().size());
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
}
