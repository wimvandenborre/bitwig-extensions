package dev.gregross.gig.handlers;

import dev.gregross.gig.extension.StateCache;
import dev.gregross.gig.rpc.JsonRpcDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NoteInputHandlerTest {

    private JsonRpcDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new JsonRpcDispatcher();
        new NoteInputHandler(null, null, null, new StateCache()).register(dispatcher);
    }

    // --- Registration ---

    @Test
    void registersNoteInputMethods() {
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("noteInput/sendNote"));
        assertTrue(methods.contains("noteInput/sendMidi"));
    }

    @Test
    void registersArpeggiatorMethods() {
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("arpeggiator/configure"));
        assertTrue(methods.contains("arpeggiator/setEnabled"));
        assertTrue(methods.contains("arpeggiator/releaseNotes"));
        assertTrue(methods.contains("arpeggiator/getState"));
    }

    @Test
    void registersNoteLatchMethods() {
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("noteLatch/configure"));
        assertTrue(methods.contains("noteLatch/setEnabled"));
        assertTrue(methods.contains("noteLatch/releaseNotes"));
        assertTrue(methods.contains("noteLatch/getState"));
    }

    @Test
    void registersExactlyTenMethods() {
        assertEquals(10, dispatcher.getRegisteredMethods().size());
    }

    // --- Arpeggiator mode validation ---

    @Test
    @SuppressWarnings("unchecked")
    void validArpModesContainsAllSeventeenModes() throws Exception {
        Field field = NoteInputHandler.class.getDeclaredField("VALID_ARP_MODES");
        field.setAccessible(true);
        Set<String> modes = (Set<String>) field.get(null);
        assertEquals(17, modes.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void validArpModesContainsExpectedValues() throws Exception {
        Field field = NoteInputHandler.class.getDeclaredField("VALID_ARP_MODES");
        field.setAccessible(true);
        Set<String> modes = (Set<String>) field.get(null);
        assertTrue(modes.contains("all"));
        assertTrue(modes.contains("up"));
        assertTrue(modes.contains("up-down"));
        assertTrue(modes.contains("up-then-down"));
        assertTrue(modes.contains("down"));
        assertTrue(modes.contains("down-up"));
        assertTrue(modes.contains("down-then-up"));
        assertTrue(modes.contains("flow"));
        assertTrue(modes.contains("random"));
        assertTrue(modes.contains("converge-up"));
        assertTrue(modes.contains("converge-down"));
        assertTrue(modes.contains("diverge-up"));
        assertTrue(modes.contains("diverge-down"));
        assertTrue(modes.contains("thumb-up"));
        assertTrue(modes.contains("thumb-down"));
        assertTrue(modes.contains("pinky-up"));
        assertTrue(modes.contains("pinky-down"));
    }

    // --- Note latch mode validation ---

    @Test
    @SuppressWarnings("unchecked")
    void validLatchModesContainsThreeModes() throws Exception {
        Field field = NoteInputHandler.class.getDeclaredField("VALID_LATCH_MODES");
        field.setAccessible(true);
        Set<String> modes = (Set<String>) field.get(null);
        assertEquals(3, modes.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void validLatchModesContainsExpectedValues() throws Exception {
        Field field = NoteInputHandler.class.getDeclaredField("VALID_LATCH_MODES");
        field.setAccessible(true);
        Set<String> modes = (Set<String>) field.get(null);
        assertTrue(modes.contains("chord"));
        assertTrue(modes.contains("toggle"));
        assertTrue(modes.contains("velocity"));
    }
}
