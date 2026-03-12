package com.gregross.bitwig.launchpadmk2;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class LaunchpadMk2FlushTest extends LaunchpadMk2ExtensionTestBase {

    // --- Grid flush: clip state → LED output ---

    @Test
    void flushGrid_emptySlot_sendsVelocityOff() {
        // Default state: all slots empty, tracks not armed
        extension.flush();

        int note = expectedGridNote(0, 0);
        verify(mockMidiOut).sendMidi(0x90, note, LaunchpadMk2Colors.OFF);
    }

    @Test
    void flushGrid_armedEmptySlot_sendsTrackArmed() {
        when(trackArm[0].get()).thenReturn(true);

        extension.flush();

        // Track 0, scene 0
        int note = expectedGridNote(0, 0);
        verify(mockMidiOut).sendMidi(0x90, note, LaunchpadMk2Colors.TRACK_ARMED);
    }

    @Test
    void flushGrid_hasContent_sendsRgbSysEx() {
        setSlotState(0, 0, true, false, false, false, false, false);
        setSlotColor(0, 0, 1.0f, 0.0f, 0.0f); // pure red

        extension.flush();

        int note = expectedGridNote(0, 0);
        int packed = LaunchpadMk2Colors.packRgb(1.0f, 0.0f, 0.0f);
        String expectedSysex = toHexString(LaunchpadMk2Colors.setLedRgb(note, packed));
        verify(mockMidiOut).sendSysex(expectedSysex);
    }

    @Test
    void flushGrid_playing_sendsPulseRed() {
        setSlotState(0, 0, true, true, false, false, false, false);

        extension.flush();

        int note = expectedGridNote(0, 0);
        verify(mockMidiOut).sendMidi(0x92, note, 5); // pulse, bright red
    }

    @Test
    void flushGrid_recording_sendsVelocityRed() {
        setSlotState(0, 0, true, false, true, false, false, false);

        extension.flush();

        int note = expectedGridNote(0, 0);
        verify(mockMidiOut).sendMidi(0x90, note, LaunchpadMk2Colors.CLIP_RECORDING);
    }

    @Test
    void flushGrid_playbackQueued_sendsPulseAmber() {
        setSlotState(0, 0, true, false, false, true, false, false);

        extension.flush();

        int note = expectedGridNote(0, 0);
        verify(mockMidiOut).sendMidi(0x92, note, LaunchpadMk2Colors.CLIP_QUEUED);
    }

    @Test
    void flushGrid_stopQueued_sendsPulseAmber() {
        setSlotState(0, 0, true, false, false, false, true, false);

        extension.flush();

        int note = expectedGridNote(0, 0);
        verify(mockMidiOut).sendMidi(0x92, note, LaunchpadMk2Colors.CLIP_QUEUED);
    }

    @Test
    void flushGrid_recordingQueued_sendsRecording() {
        // isRecordingQueued is checked via isRecording() || isRecordingQueued()
        setSlotState(0, 0, true, false, false, false, false, true);

        extension.flush();

        int note = expectedGridNote(0, 0);
        verify(mockMidiOut).sendMidi(0x90, note, LaunchpadMk2Colors.CLIP_RECORDING);
    }

    @Test
    void flushGrid_noteMapping_invertedRowCol() {
        // Track 0, scene 0 → padRow = 7 (inverted), padCol = 7 (inverted)
        // gridNote(7, 7) = 8*10 + 8 = 88
        setSlotState(0, 0, true, true, false, false, false, false);

        extension.flush();

        verify(mockMidiOut).sendMidi(0x92, 88, 5);
    }

    @Test
    void flushGrid_track7scene7_correctNote() {
        // Track 7, scene 7 → padRow = 0, padCol = 0
        // gridNote(0, 0) = 1*10 + 1 = 11
        setSlotState(7, 7, true, true, false, false, false, false);

        extension.flush();

        verify(mockMidiOut).sendMidi(0x92, 11, 5);
    }

    @Test
    void flushGrid_cachedState_noResend() {
        setSlotState(0, 0, true, true, false, false, false, false);

        extension.flush();
        clearInvocations(mockMidiOut);

        // Flush again — same state, nothing should be sent for this slot
        // But ledsDirty is false after first flush, so we need to mark dirty
        // Actually flush sets ledsDirty = false, so second flush exits early
        extension.flush();

        verifyNoInteractions(mockMidiOut);
    }

    @Test
    void flushGrid_priorityOrder_recordingOverPlaying() {
        // Recording takes priority over playing
        setSlotState(0, 0, true, true, true, false, false, false);

        extension.flush();

        int note = expectedGridNote(0, 0);
        verify(mockMidiOut).sendMidi(0x90, note, LaunchpadMk2Colors.CLIP_RECORDING);
        verify(mockMidiOut, never()).sendMidi(0x92, note, 5);
    }

    @Test
    void flushGrid_priorityOrder_queuedOverPlaying() {
        // Queued takes priority over playing
        setSlotState(0, 0, true, true, false, true, false, false);

        extension.flush();

        int note = expectedGridNote(0, 0);
        verify(mockMidiOut).sendMidi(0x92, note, LaunchpadMk2Colors.CLIP_QUEUED);
    }

    // --- Scene launch flush ---

    @Test
    void flushSceneLaunch_idle_sendsRgbSysEx() {
        // No clips with content → idle, shows scene color
        setSceneColor(0, 0.0f, 1.0f, 0.0f); // green

        extension.flush();

        int note = expectedSceneNote(0);
        int packed = LaunchpadMk2Colors.packRgb(0.0f, 1.0f, 0.0f);
        String expectedSysex = toHexString(LaunchpadMk2Colors.setLedRgb(note, packed));
        verify(mockMidiOut).sendSysex(expectedSysex);
    }

    @Test
    void flushSceneLaunch_allPlaying_sendsPulseRed() {
        // All tracks with content in scene 0 are playing
        for (int t = 0; t < 3; t++) {
            setSlotState(t, 0, true, true, false, false, false, false);
        }

        extension.flush();

        int note = expectedSceneNote(0);
        verify(mockMidiOut).sendMidi(0x92, note, LaunchpadMk2Colors.CLIP_RECORDING);
    }

    @Test
    void flushSceneLaunch_someButNotAllPlaying_sendsPulseOrange() {
        // 2 tracks with content, only 1 playing
        setSlotState(0, 0, true, true, false, false, false, false);
        setSlotState(1, 0, true, false, false, false, false, false);

        extension.flush();

        int note = expectedSceneNote(0);
        verify(mockMidiOut).sendMidi(0x92, note, 9); // orange
    }

    @Test
    void flushSceneLaunch_anyQueued_sendsPulseAmber() {
        setSlotState(0, 0, true, false, false, true, false, false);

        extension.flush();

        int note = expectedSceneNote(0);
        verify(mockMidiOut).sendMidi(0x92, note, LaunchpadMk2Colors.CLIP_QUEUED);
    }

    @Test
    void flushSceneLaunch_noContent_allPlayingFalse() {
        // No content in any track for scene 0 → allPlaying should be false → idle RGB
        extension.flush();

        int note = expectedSceneNote(0);
        // Should get RGB SysEx (idle scene color), not pulse
        verify(mockMidiOut, never()).sendMidi(eq(0x92), eq(note), anyInt());
    }

    // --- Top row flush: utility modes ---

    @Test
    void flushTopRow_globalMode_playingGreenPulse() {
        when(transportIsPlaying.get()).thenReturn(true);

        extension.flush();

        // Mixer button (index 7) → CC_UP + 7 = 111, pulse green (21)
        verify(mockMidiOut).sendMidi(0xB2, LaunchpadMk2Colors.CC_MIXER, 21);
    }

    @Test
    void flushTopRow_globalMode_stoppedDimGreen() {
        when(transportIsPlaying.get()).thenReturn(false);

        extension.flush();

        // Mixer button → dim green (23)
        verify(mockMidiOut).sendMidi(0xB0, LaunchpadMk2Colors.CC_MIXER, 23);
    }

    @Test
    void flushTopRow_globalMode_recordingRedPulse() {
        when(transportIsRecording.get()).thenReturn(true);

        extension.flush();

        // User1 button (index 5) → CC_UP + 5 = 109, pulse red (5)
        verify(mockMidiOut).sendMidi(0xB2, LaunchpadMk2Colors.CC_USER1, 5);
    }

    @Test
    void flushTopRow_globalMode_notRecordingDimRed() {
        when(transportIsRecording.get()).thenReturn(false);

        extension.flush();

        // User1 → dim red (7)
        verify(mockMidiOut).sendMidi(0xB0, LaunchpadMk2Colors.CC_USER1, 7);
    }

    @Test
    void flushTopRow_globalMode_stopButton() {
        extension.flush();

        // User2 button (index 6) → static red (5)
        verify(mockMidiOut).sendMidi(0xB0, LaunchpadMk2Colors.CC_USER2, 5);
    }

    @Test
    void flushTopRow_trackMode_armedRedPulse() {
        // Switch to track mode by cycling once
        sendCC(LaunchpadMk2Colors.CC_SESSION);
        clearInvocations(mockMidiOut);

        when(cursorArm.get()).thenReturn(true);

        extension.flush();

        verify(mockMidiOut).sendMidi(0xB2, LaunchpadMk2Colors.CC_USER1, 5);
    }

    @Test
    void flushTopRow_trackMode_soloYellowPulse() {
        sendCC(LaunchpadMk2Colors.CC_SESSION);
        clearInvocations(mockMidiOut);

        when(cursorSolo.get()).thenReturn(true);

        extension.flush();

        verify(mockMidiOut).sendMidi(0xB2, LaunchpadMk2Colors.CC_USER2, 13);
    }

    @Test
    void flushTopRow_trackMode_mutedOrangePulse() {
        sendCC(LaunchpadMk2Colors.CC_SESSION);
        clearInvocations(mockMidiOut);

        when(cursorMute.get()).thenReturn(true);

        extension.flush();

        verify(mockMidiOut).sendMidi(0xB2, LaunchpadMk2Colors.CC_MIXER, 11);
    }

    @Test
    void flushTopRow_utilityMode_staticColors() {
        // Cycle to utility mode (2 presses)
        sendCC(LaunchpadMk2Colors.CC_SESSION);
        sendCC(LaunchpadMk2Colors.CC_SESSION);
        clearInvocations(mockMidiOut);

        extension.flush();

        // User1 = capture scene (yellow 13)
        verify(mockMidiOut).sendMidi(0xB0, LaunchpadMk2Colors.CC_USER1, 13);
        // User2 = undo (white 3)
        verify(mockMidiOut).sendMidi(0xB0, LaunchpadMk2Colors.CC_USER2, 3);
        // Mixer = redo (white 3)
        verify(mockMidiOut).sendMidi(0xB0, LaunchpadMk2Colors.CC_MIXER, 3);
    }

    @Test
    void flushTopRow_navSceneActive() {
        when(sceneCanScrollBack.get()).thenReturn(true);

        extension.flush();

        verify(mockMidiOut).sendMidi(0xB0, LaunchpadMk2Colors.CC_UP, LaunchpadMk2Colors.NAV_SCENE_ACTIVE);
    }

    @Test
    void flushTopRow_navInactive() {
        when(sceneCanScrollBack.get()).thenReturn(false);

        extension.flush();

        verify(mockMidiOut).sendMidi(0xB0, LaunchpadMk2Colors.CC_UP, LaunchpadMk2Colors.NAV_INACTIVE);
    }

    @Test
    void flushTopRow_sessionButtonAlwaysWhite() {
        extension.flush();

        // CC_SESSION (index 4) = mode toggle, always white (3)
        verify(mockMidiOut).sendMidi(0xB0, LaunchpadMk2Colors.CC_SESSION, 3);
    }

    @Test
    void flush_notDirty_skipsAll() {
        // First flush clears dirty flag
        extension.flush();
        clearInvocations(mockMidiOut);

        // Second flush without marking dirty — should skip entirely
        extension.flush();

        verifyNoInteractions(mockMidiOut);
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
