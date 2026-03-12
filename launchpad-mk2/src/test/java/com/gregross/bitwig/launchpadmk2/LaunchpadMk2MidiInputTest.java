package com.gregross.bitwig.launchpadmk2;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class LaunchpadMk2MidiInputTest extends LaunchpadMk2ExtensionTestBase {

    // --- Grid pad presses ---

    @Test
    void gridPad_stoppedClip_launches() {
        // Track 0, scene 0 has content but not playing → launch
        setSlotState(0, 0, true, false, false, false, false, false);

        // CCW rotation: track 0 → padCol 7, scene 0 → padRow 7
        // note = gridNote(7, 7) = 88
        sendNoteOn(88, 127);

        verify(mockSlots[0][0]).launch();
    }

    @Test
    void gridPad_playingClip_stops() {
        setSlotState(0, 0, true, true, false, false, false, false);

        sendNoteOn(88, 127);

        verify(mockTracks[0]).stop();
    }

    @Test
    void gridPad_invertedMapping_correctTrackScene() {
        // Hardware note 11 = gridNote(0, 0) → padRow 0, padCol 0
        // scene = (7 - 0) = 7, track = (7 - 0) = 7
        setSlotState(7, 7, true, false, false, false, false, false);

        sendNoteOn(11, 127);

        verify(mockSlots[7][7]).launch();
    }

    @Test
    void gridPad_middlePosition_correctMapping() {
        // Track 3, scene 4 → padCol = 4, padRow = 3
        // note = gridNote(3, 4) = 4*10 + 5 = 45
        setSlotState(3, 4, true, false, false, false, false, false);

        sendNoteOn(45, 127);

        verify(mockSlots[3][4]).launch();
    }

    @Test
    void gridPad_outOfBounds_ignored() {
        // Note 100 → col = 9, out of grid range
        sendNoteOn(100, 127);

        // No slot interactions
        for (int t = 0; t < GRID_SIZE; t++)
            for (int s = 0; s < GRID_SIZE; s++)
                verify(mockSlots[t][s], never()).launch();
    }

    @Test
    void gridPad_noteOff_ignored() {
        setSlotState(0, 0, true, false, false, false, false, false);

        // data2 = 0 means note off → should be ignored
        sendNoteOn(88, 0);

        verify(mockSlots[0][0], never()).launch();
    }

    // --- Scene launch button presses ---

    @Test
    void sceneLaunch_notAllPlaying_launchesScene() {
        // Scene 0: some tracks have content, not all playing
        setSlotState(0, 0, true, false, false, false, false, false);
        setSlotState(1, 0, true, true, false, false, false, false);

        // Scene launch button for scene 0: padRow = 7, col = 8
        // sceneLaunchNote(7) = 8*10 + 9 = 89
        sendNoteOn(89, 127);

        verify(mockScenes[0]).launch();
    }

    @Test
    void sceneLaunch_allPlaying_stopsAllTracks() {
        // Scene 0: all tracks with content are playing
        for (int t = 0; t < 3; t++) {
            setSlotState(t, 0, true, true, false, false, false, false);
        }

        sendNoteOn(89, 127);

        for (int t = 0; t < GRID_SIZE; t++) {
            verify(mockTracks[t]).stop();
        }
    }

    @Test
    void sceneLaunch_noContent_launchesScene() {
        // No content in any track for scene 0 → allPlaying=false, launches
        sendNoteOn(89, 127);

        verify(mockScenes[0]).launch();
    }

    @Test
    void sceneLaunch_scene7_correctNote() {
        // Scene 7 → padRow = 0, sceneLaunchNote(0) = 1*10 + 9 = 19
        sendNoteOn(19, 127);

        verify(mockScenes[7]).launch();
    }

    // --- CC navigation ---

    @Test
    void cc_up_scrollsBackwards() {
        sendCC(LaunchpadMk2Colors.CC_UP);

        verify(mockSceneBank).scrollBackwards();
    }

    @Test
    void cc_down_scrollsForwards() {
        sendCC(LaunchpadMk2Colors.CC_DOWN);

        verify(mockSceneBank).scrollForwards();
    }

    @Test
    void cc_left_selectsNext() {
        // After CCW rotation, left arrow = next track
        sendCC(LaunchpadMk2Colors.CC_LEFT);

        verify(mockCursorTrack).selectNext();
    }

    @Test
    void cc_right_selectsPrevious() {
        // After CCW rotation, right arrow = previous track
        sendCC(LaunchpadMk2Colors.CC_RIGHT);

        verify(mockCursorTrack).selectPrevious();
    }

    // --- Utility mode cycling ---

    @Test
    void cc_session_cyclesUtilityMode() {
        // Press 3 times: Global → Track → Utility → Global
        // After 1 press: track mode
        sendCC(LaunchpadMk2Colors.CC_SESSION);
        clearInvocations(mockMidiOut);

        // Verify track mode by pressing User1 → should toggle arm (not record)
        sendCC(LaunchpadMk2Colors.CC_USER1);
        verify(cursorArm).toggle();
        verify(transportIsRecording, never()).toggle();

        // Press session again → utility mode
        sendCC(LaunchpadMk2Colors.CC_SESSION);
        clearInvocations(cursorArm);

        sendCC(LaunchpadMk2Colors.CC_USER1);
        // Utility mode User1 = capture scene
        verify(mockAction).invoke();

        // Press session again → back to global mode
        sendCC(LaunchpadMk2Colors.CC_SESSION);

        sendCC(LaunchpadMk2Colors.CC_USER1);
        verify(transportIsRecording).toggle();
    }

    // --- Modal actions: Global mode (default) ---

    @Test
    void cc_user1_globalMode_togglesRecord() {
        sendCC(LaunchpadMk2Colors.CC_USER1);

        verify(transportIsRecording).toggle();
    }

    @Test
    void cc_user2_globalMode_stopsTransport() {
        sendCC(LaunchpadMk2Colors.CC_USER2);

        verify(mockTransport).stop();
    }

    @Test
    void cc_mixer_globalMode_playsTransport() {
        sendCC(LaunchpadMk2Colors.CC_MIXER);

        verify(mockTransport).play();
    }

    // --- Modal actions: Track mode ---

    @Test
    void cc_user1_trackMode_togglesArm() {
        sendCC(LaunchpadMk2Colors.CC_SESSION); // → track mode

        sendCC(LaunchpadMk2Colors.CC_USER1);

        verify(cursorArm).toggle();
    }

    @Test
    void cc_user2_trackMode_togglesSolo() {
        sendCC(LaunchpadMk2Colors.CC_SESSION); // → track mode

        sendCC(LaunchpadMk2Colors.CC_USER2);

        verify(cursorSolo).toggle();
    }

    @Test
    void cc_mixer_trackMode_togglesMute() {
        sendCC(LaunchpadMk2Colors.CC_SESSION); // → track mode

        sendCC(LaunchpadMk2Colors.CC_MIXER);

        verify(cursorMute).toggle();
    }

    // --- Modal actions: Utility mode ---

    @Test
    void cc_user1_utilityMode_capturesScene() {
        sendCC(LaunchpadMk2Colors.CC_SESSION); // → track
        sendCC(LaunchpadMk2Colors.CC_SESSION); // → utility

        sendCC(LaunchpadMk2Colors.CC_USER1);

        verify(mockApplication).getAction("Create Scene From Playing Launcher Clips");
        verify(mockAction).invoke();
    }

    @Test
    void cc_user2_utilityMode_undoes() {
        sendCC(LaunchpadMk2Colors.CC_SESSION); // → track
        sendCC(LaunchpadMk2Colors.CC_SESSION); // → utility

        sendCC(LaunchpadMk2Colors.CC_USER2);

        verify(mockApplication).undo();
    }

    @Test
    void cc_mixer_utilityMode_redoes() {
        sendCC(LaunchpadMk2Colors.CC_SESSION); // → track
        sendCC(LaunchpadMk2Colors.CC_SESSION); // → utility

        sendCC(LaunchpadMk2Colors.CC_MIXER);

        verify(mockApplication).redo();
    }
}
