package com.gregross.bitwig.launchpadmk2;

import com.bitwig.extension.callback.ShortMidiDataReceivedCallback;
import com.bitwig.extension.controller.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
abstract class LaunchpadMk2ExtensionTestBase {

    protected static final int GRID_SIZE = 8;

    @Mock protected ControllerHost mockHost;
    @Mock protected MidiIn mockMidiIn;
    @Mock protected MidiOut mockMidiOut;
    @Mock protected TrackBank mockTrackBank;
    @Mock protected SceneBank mockSceneBank;
    @Mock protected CursorTrack mockCursorTrack;
    @Mock protected Transport mockTransport;
    @Mock protected Application mockApplication;
    @Mock protected Action mockAction;

    protected Track[] mockTracks = new Track[GRID_SIZE];
    protected ClipLauncherSlotBank[] mockSlotBanks = new ClipLauncherSlotBank[GRID_SIZE];
    protected ClipLauncherSlot[][] mockSlots = new ClipLauncherSlot[GRID_SIZE][GRID_SIZE];
    protected Scene[] mockScenes = new Scene[GRID_SIZE];

    // Per-slot observable state
    protected BooleanValue[][] slotHasContent = new BooleanValue[GRID_SIZE][GRID_SIZE];
    protected BooleanValue[][] slotIsPlaying = new BooleanValue[GRID_SIZE][GRID_SIZE];
    protected BooleanValue[][] slotIsRecording = new BooleanValue[GRID_SIZE][GRID_SIZE];
    protected BooleanValue[][] slotIsPlaybackQueued = new BooleanValue[GRID_SIZE][GRID_SIZE];
    protected BooleanValue[][] slotIsStopQueued = new BooleanValue[GRID_SIZE][GRID_SIZE];
    protected BooleanValue[][] slotIsRecordingQueued = new BooleanValue[GRID_SIZE][GRID_SIZE];
    protected SettableColorValue[][] slotColor = new SettableColorValue[GRID_SIZE][GRID_SIZE];

    // Per-track state
    protected SettableBooleanValue[] trackArm = new SettableBooleanValue[GRID_SIZE];
    protected SettableColorValue[] trackColor = new SettableColorValue[GRID_SIZE];

    // Scene colors
    protected SettableColorValue[] sceneColor = new SettableColorValue[GRID_SIZE];

    // Transport state
    protected SettableBooleanValue transportIsPlaying;
    protected SettableBooleanValue transportIsRecording;

    // CursorTrack state
    protected BooleanValue cursorHasPrevious;
    protected BooleanValue cursorHasNext;
    protected SettableBooleanValue cursorArm;
    protected SoloValue cursorSolo;
    protected SettableBooleanValue cursorMute;

    // SceneBank scroll state
    protected BooleanValue sceneCanScrollBack;
    protected BooleanValue sceneCanScrollForward;

    // Captured MIDI callback
    protected ShortMidiDataReceivedCallback midiCallback;

    protected LaunchpadMk2Extension extension;

    @BeforeEach
    void setUp() {
        // Host → MIDI ports
        when(mockHost.getMidiInPort(0)).thenReturn(mockMidiIn);
        when(mockHost.getMidiOutPort(0)).thenReturn(mockMidiOut);

        // Host → TrackBank → SceneBank
        when(mockHost.createMainTrackBank(GRID_SIZE, 0, GRID_SIZE)).thenReturn(mockTrackBank);
        when(mockTrackBank.sceneBank()).thenReturn(mockSceneBank);

        // Host → CursorTrack
        when(mockHost.createCursorTrack(anyString(), anyString(), anyInt(), anyInt(), anyBoolean()))
                .thenReturn(mockCursorTrack);

        // Host → Transport
        when(mockHost.createTransport()).thenReturn(mockTransport);

        // Host → Application
        when(mockHost.createApplication()).thenReturn(mockApplication);
        when(mockApplication.getAction(anyString())).thenReturn(mockAction);

        // Build 8×8 grid of track/slot mocks
        for (int t = 0; t < GRID_SIZE; t++) {
            mockTracks[t] = mock(Track.class);
            mockSlotBanks[t] = mock(ClipLauncherSlotBank.class);
            when(mockTrackBank.getItemAt(t)).thenReturn(mockTracks[t]);
            when(mockTracks[t].clipLauncherSlotBank()).thenReturn(mockSlotBanks[t]);

            trackArm[t] = mockSettableBooleanValue();
            trackColor[t] = mockColorValue(0f, 0f, 0f);
            when(mockTracks[t].arm()).thenReturn(trackArm[t]);
            when(mockTracks[t].color()).thenReturn(trackColor[t]);

            for (int s = 0; s < GRID_SIZE; s++) {
                mockSlots[t][s] = mock(ClipLauncherSlot.class);
                when(mockSlotBanks[t].getItemAt(s)).thenReturn(mockSlots[t][s]);

                slotHasContent[t][s] = mockBooleanValue();
                slotIsPlaying[t][s] = mockBooleanValue();
                slotIsRecording[t][s] = mockBooleanValue();
                slotIsPlaybackQueued[t][s] = mockBooleanValue();
                slotIsStopQueued[t][s] = mockBooleanValue();
                slotIsRecordingQueued[t][s] = mockBooleanValue();
                slotColor[t][s] = mockColorValue(0f, 0f, 0f);

                when(mockSlots[t][s].hasContent()).thenReturn(slotHasContent[t][s]);
                when(mockSlots[t][s].isPlaying()).thenReturn(slotIsPlaying[t][s]);
                when(mockSlots[t][s].isRecording()).thenReturn(slotIsRecording[t][s]);
                when(mockSlots[t][s].isPlaybackQueued()).thenReturn(slotIsPlaybackQueued[t][s]);
                when(mockSlots[t][s].isStopQueued()).thenReturn(slotIsStopQueued[t][s]);
                when(mockSlots[t][s].isRecordingQueued()).thenReturn(slotIsRecordingQueued[t][s]);
                when(mockSlots[t][s].color()).thenReturn(slotColor[t][s]);
            }
        }

        // Build scene mocks
        for (int s = 0; s < GRID_SIZE; s++) {
            mockScenes[s] = mock(Scene.class);
            sceneColor[s] = mockColorValue(0f, 0f, 0f);
            when(mockSceneBank.getScene(s)).thenReturn(mockScenes[s]);
            when(mockSceneBank.getItemAt(s)).thenReturn(mockScenes[s]);
            when(mockScenes[s].color()).thenReturn(sceneColor[s]);
        }

        // Transport observables
        transportIsPlaying = mockSettableBooleanValue();
        transportIsRecording = mockSettableBooleanValue();
        when(mockTransport.isPlaying()).thenReturn(transportIsPlaying);
        when(mockTransport.isArrangerRecordEnabled()).thenReturn(transportIsRecording);

        // CursorTrack observables
        cursorHasPrevious = mockBooleanValue();
        cursorHasNext = mockBooleanValue();
        cursorArm = mockSettableBooleanValue();
        cursorSolo = mock(SoloValue.class);
        when(cursorSolo.get()).thenReturn(false);
        cursorMute = mockSettableBooleanValue();
        when(mockCursorTrack.hasPrevious()).thenReturn(cursorHasPrevious);
        when(mockCursorTrack.hasNext()).thenReturn(cursorHasNext);
        when(mockCursorTrack.arm()).thenReturn(cursorArm);
        when(mockCursorTrack.solo()).thenReturn(cursorSolo);
        when(mockCursorTrack.mute()).thenReturn(cursorMute);

        // SceneBank scroll observables
        sceneCanScrollBack = mockBooleanValue();
        sceneCanScrollForward = mockBooleanValue();
        when(mockSceneBank.canScrollBackwards()).thenReturn(sceneCanScrollBack);
        when(mockSceneBank.canScrollForwards()).thenReturn(sceneCanScrollForward);

        // Create extension and call init
        LaunchpadMk2ExtensionDefinition definition = new LaunchpadMk2ExtensionDefinition();
        extension = new LaunchpadMk2Extension(definition, mockHost);
        extension.init();

        // Capture the MIDI callback registered during init
        ArgumentCaptor<ShortMidiDataReceivedCallback> captor =
                ArgumentCaptor.forClass(ShortMidiDataReceivedCallback.class);
        verify(mockMidiIn).setMidiCallback(captor.capture());
        midiCallback = captor.getValue();

        // Reset midiOut interactions so tests start clean (init sends SysEx)
        clearInvocations(mockMidiOut);
    }

    // --- Helpers ---

    protected BooleanValue mockBooleanValue() {
        BooleanValue bv = mock(BooleanValue.class);
        when(bv.get()).thenReturn(false);
        return bv;
    }

    protected SettableBooleanValue mockSettableBooleanValue() {
        SettableBooleanValue sbv = mock(SettableBooleanValue.class);
        when(sbv.get()).thenReturn(false);
        return sbv;
    }

    protected SettableColorValue mockColorValue(float r, float g, float b) {
        SettableColorValue cv = mock(SettableColorValue.class);
        when(cv.red()).thenReturn(r);
        when(cv.green()).thenReturn(g);
        when(cv.blue()).thenReturn(b);
        return cv;
    }

    protected void setSlotState(int track, int scene,
                                boolean hasContent, boolean isPlaying,
                                boolean isRecording, boolean isPlaybackQueued,
                                boolean isStopQueued, boolean isRecordingQueued) {
        when(slotHasContent[track][scene].get()).thenReturn(hasContent);
        when(slotIsPlaying[track][scene].get()).thenReturn(isPlaying);
        when(slotIsRecording[track][scene].get()).thenReturn(isRecording);
        when(slotIsPlaybackQueued[track][scene].get()).thenReturn(isPlaybackQueued);
        when(slotIsStopQueued[track][scene].get()).thenReturn(isStopQueued);
        when(slotIsRecordingQueued[track][scene].get()).thenReturn(isRecordingQueued);
    }

    protected void setSlotColor(int track, int scene, float r, float g, float b) {
        when(slotColor[track][scene].red()).thenReturn(r);
        when(slotColor[track][scene].green()).thenReturn(g);
        when(slotColor[track][scene].blue()).thenReturn(b);
    }

    protected void setSceneColor(int scene, float r, float g, float b) {
        when(sceneColor[scene].red()).thenReturn(r);
        when(sceneColor[scene].green()).thenReturn(g);
        when(sceneColor[scene].blue()).thenReturn(b);
    }

    /**
     * Compute the expected MIDI note for a grid pad given Bitwig track/scene indices.
     * The extension inverts both row and column for CCW rotation.
     */
    protected int expectedGridNote(int track, int scene) {
        int padRow = (GRID_SIZE - 1) - scene;
        int padCol = (GRID_SIZE - 1) - track;
        return LaunchpadMk2Colors.gridNote(padRow, padCol);
    }

    /**
     * Compute the expected scene launch note for a Bitwig scene index.
     */
    protected int expectedSceneNote(int scene) {
        int padRow = (GRID_SIZE - 1) - scene;
        return LaunchpadMk2Colors.sceneLaunchNote(padRow);
    }

    /**
     * Send a note-on MIDI message through the captured callback.
     */
    protected void sendNoteOn(int note, int velocity) {
        midiCallback.midiReceived(0x90, note, velocity);
    }

    /**
     * Send a CC MIDI message through the captured callback.
     */
    protected void sendCC(int cc) {
        midiCallback.midiReceived(0xB0, cc, 127);
    }
}
