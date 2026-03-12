package com.gregross.bitwig.launchpadmk2;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class LaunchpadMk2InitTest extends LaunchpadMk2ExtensionTestBase {

    // Note: init() has already been called in @BeforeEach (test base).
    // These tests verify what init() did by checking mock interactions
    // BEFORE clearInvocations is called. We need to re-check the midiOut
    // since the base clears it. For init-specific assertions, we check
    // interactions that happened during setUp's init() call.

    @Test
    void init_sendsSessionLayoutSysEx() {
        // Re-create extension without clearing invocations to verify init behavior
        LaunchpadMk2ExtensionDefinition def = new LaunchpadMk2ExtensionDefinition();
        LaunchpadMk2Extension ext = new LaunchpadMk2Extension(def, mockHost);

        clearInvocations(mockMidiOut);
        ext.init();

        String expectedSysex = toHexString(LaunchpadMk2Colors.setSessionLayout());
        verify(mockMidiOut).sendSysex(expectedSysex);
    }

    @Test
    void init_sendsResetLedsSysEx() {
        LaunchpadMk2ExtensionDefinition def = new LaunchpadMk2ExtensionDefinition();
        LaunchpadMk2Extension ext = new LaunchpadMk2Extension(def, mockHost);

        clearInvocations(mockMidiOut);
        ext.init();

        String expectedSysex = toHexString(LaunchpadMk2Colors.resetLeds());
        verify(mockMidiOut).sendSysex(expectedSysex);
    }

    @Test
    void init_setsMidiCallback() {
        // The base already verified this via ArgumentCaptor; verify count
        verify(mockMidiIn, atLeastOnce()).setMidiCallback(any());
    }

    @Test
    void init_createsTrackBankWithCorrectParams() {
        verify(mockHost, atLeastOnce()).createMainTrackBank(GRID_SIZE, 0, GRID_SIZE);
    }

    @Test
    void init_marksDirty_firstFlushSendsLedData() {
        // After init, ledsDirty = true, so flush should process
        extension.flush();

        // Should have sent data for at least one grid cell
        verify(mockMidiOut, atLeastOnce()).sendMidi(anyInt(), anyInt(), anyInt());
    }

    // --- Helper ---

    private static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }
}
