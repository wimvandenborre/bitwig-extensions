package com.gregross.bitwig.launchpadmk2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LaunchpadMk2ExtensionDefinitionTest {

    private final LaunchpadMk2ExtensionDefinition definition = new LaunchpadMk2ExtensionDefinition();

    @Test
    void name_returnsLaunchpadMk2() {
        assertEquals("Launchpad MK2", definition.getName());
    }

    @Test
    void author_returnsGregRoss() {
        assertEquals("Greg Ross", definition.getAuthor());
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
        assertEquals(1, definition.getNumMidiOutPorts());
    }

    @Test
    void helpFilePath_returnsReadme() {
        assertEquals("README.md", definition.getHelpFilePath());
    }
}
