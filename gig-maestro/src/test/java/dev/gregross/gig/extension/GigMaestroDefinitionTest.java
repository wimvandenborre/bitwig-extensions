package dev.gregross.gig.extension;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GigMaestroDefinitionTest {

    private final GigMaestroDefinition definition = new GigMaestroDefinition();

    @Test
    void name_returnsGigMaestro() {
        assertEquals("Gig Maestro", definition.getName());
    }

    @Test
    void author_returnsGregross() {
        assertEquals("gregross", definition.getAuthor());
    }

    @Test
    void apiVersion_returns25() {
        assertEquals(25, definition.getRequiredAPIVersion());
    }

    @Test
    void id_isNotNull() {
        assertNotNull(definition.getId());
    }

    @Test
    void midiPorts_correctCounts() {
        assertEquals(1, definition.getNumMidiInPorts());
        assertEquals(0, definition.getNumMidiOutPorts());
    }
}
