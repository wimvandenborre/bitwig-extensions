package dev.gregross.gig.extension;

import com.bitwig.extension.callback.BooleanValueChangedCallback;
import com.bitwig.extension.callback.ClipLauncherSlotBankPlaybackStateChangedCallback;
import com.bitwig.extension.callback.ColorValueChangedCallback;
import com.bitwig.extension.callback.DoubleValueChangedCallback;
import com.bitwig.extension.callback.IndexedBooleanValueChangedCallback;
import com.bitwig.extension.callback.IndexedStringValueChangedCallback;
import com.bitwig.extension.callback.IntegerValueChangedCallback;
import com.bitwig.extension.callback.StringArrayValueChangedCallback;
import com.bitwig.extension.callback.StringValueChangedCallback;
import com.bitwig.extension.controller.api.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class StateCache {

    private static final int TRACK_COUNT = 64;
    private static final int SCENE_COUNT = 8;

    // Transport state
    private volatile boolean isPlaying;
    private volatile boolean isRecording;
    private volatile double tempo;
    private volatile double playPosition;
    private volatile int timeSignatureNumerator;
    private volatile int timeSignatureDenominator;
    private volatile boolean isLoopEnabled;
    private volatile boolean isMetronomeEnabled;

    // Track state
    private final String[] trackNames = new String[TRACK_COUNT];
    private final double[] trackVolumes = new double[TRACK_COUNT];
    private final double[] trackPans = new double[TRACK_COUNT];
    private final boolean[] trackMutes = new boolean[TRACK_COUNT];
    private final boolean[] trackSolos = new boolean[TRACK_COUNT];
    private final boolean[] trackArms = new boolean[TRACK_COUNT];
    private final float[][] trackColors = new float[TRACK_COUNT][3]; // r, g, b

    // Clip state — [trackIndex][slotIndex]
    private final boolean[][] clipHasContent = new boolean[TRACK_COUNT][SCENE_COUNT];
    private final boolean[][] clipIsPlaying = new boolean[TRACK_COUNT][SCENE_COUNT];
    private final boolean[][] clipIsRecording = new boolean[TRACK_COUNT][SCENE_COUNT];
    private final boolean[][] clipIsPlaybackQueued = new boolean[TRACK_COUNT][SCENE_COUNT];
    private final boolean[][] clipIsRecordingQueued = new boolean[TRACK_COUNT][SCENE_COUNT];
    private final boolean[][] clipIsStopQueued = new boolean[TRACK_COUNT][SCENE_COUNT];
    private final String[][] clipNames = new String[TRACK_COUNT][SCENE_COUNT];
    private final float[][][] clipColors = new float[TRACK_COUNT][SCENE_COUNT][3];

    // Scene state
    private final String[] sceneNames = new String[SCENE_COUNT];
    private final int[] sceneClipCounts = new int[SCENE_COUNT];

    // Master state
    private volatile double masterVolume;
    private volatile double masterPan;

    // Device state (cursor device)
    private static final int PARAM_COUNT = 8;
    private volatile String cursorTrackName = "";
    private volatile String deviceName = "";
    private volatile boolean deviceEnabled;
    private volatile boolean deviceIsPlugin;
    private volatile int devicePosition;
    private volatile String presetName = "";
    private volatile String presetCategory = "";
    private volatile String presetCreator = "";
    private volatile boolean isWindowOpen;
    private volatile boolean isExpanded;

    // Remote controls state
    private volatile int pageIndex;
    private volatile int pageCount;
    private volatile String[] devicePageNames = new String[0];
    private final String[] paramNames = new String[PARAM_COUNT];
    private final double[] paramValues = new double[PARAM_COUNT];
    private final String[] paramDisplayedValues = new String[PARAM_COUNT];

    // Application state
    private volatile String projectName = "";
    private volatile boolean canUndo;
    private volatile boolean canRedo;
    private volatile boolean hasActiveEngine;

    // Delta detection — previous section hashes
    private int prevTransportHash;
    private int prevTracksHash;
    private int prevScenesHash;
    private int prevDeviceHash;
    private int prevMasterHash;
    private int prevApplicationHash;

    public void registerObservers(Transport transport, TrackBank trackBank,
                                   MasterTrack masterTrack, Application application) {
        // Transport observers
        transport.isPlaying().markInterested();
        transport.isPlaying().addValueObserver((BooleanValueChangedCallback) v -> isPlaying = v);

        transport.isArrangerRecordEnabled().markInterested();
        transport.isArrangerRecordEnabled().addValueObserver((BooleanValueChangedCallback) v -> isRecording = v);

        transport.tempo().value().markInterested();
        transport.tempo().value().addRawValueObserver((DoubleValueChangedCallback) v -> tempo = v);

        transport.playPosition().markInterested();
        transport.playPosition().addValueObserver((DoubleValueChangedCallback) v -> playPosition = v);

        transport.timeSignature().numerator().markInterested();
        transport.timeSignature().numerator().addValueObserver((IntegerValueChangedCallback) v -> timeSignatureNumerator = v);

        transport.timeSignature().denominator().markInterested();
        transport.timeSignature().denominator().addValueObserver((IntegerValueChangedCallback) v -> timeSignatureDenominator = v);

        transport.isArrangerLoopEnabled().markInterested();
        transport.isArrangerLoopEnabled().addValueObserver((BooleanValueChangedCallback) v -> isLoopEnabled = v);

        transport.isMetronomeEnabled().markInterested();
        transport.isMetronomeEnabled().addValueObserver((BooleanValueChangedCallback) v -> isMetronomeEnabled = v);

        // Track observers
        for (int i = 0; i < TRACK_COUNT; i++) {
            final int idx = i;
            Track track = (Track) trackBank.getItemAt(i);

            track.name().markInterested();
            track.name().addValueObserver((StringValueChangedCallback) v -> trackNames[idx] = (String) v);
            trackNames[i] = "";

            track.volume().value().markInterested();
            track.volume().value().addValueObserver((DoubleValueChangedCallback) v -> trackVolumes[idx] = v);

            track.pan().value().markInterested();
            track.pan().value().addValueObserver((DoubleValueChangedCallback) v -> trackPans[idx] = v);

            track.mute().markInterested();
            track.mute().addValueObserver((BooleanValueChangedCallback) v -> trackMutes[idx] = v);

            track.solo().markInterested();
            track.solo().addValueObserver((BooleanValueChangedCallback) v -> trackSolos[idx] = v);

            track.arm().markInterested();
            track.arm().addValueObserver((BooleanValueChangedCallback) v -> trackArms[idx] = v);

            track.color().markInterested();
            track.color().addValueObserver((ColorValueChangedCallback) (r, g, b) -> {
                trackColors[idx][0] = r;
                trackColors[idx][1] = g;
                trackColors[idx][2] = b;
            });
        }

        // Master track observers
        masterTrack.volume().value().markInterested();
        masterTrack.volume().value().addValueObserver((DoubleValueChangedCallback) v -> masterVolume = v);

        masterTrack.pan().value().markInterested();
        masterTrack.pan().value().addValueObserver((DoubleValueChangedCallback) v -> masterPan = v);

        // Application observers
        application.projectName().markInterested();
        application.projectName().addValueObserver((StringValueChangedCallback) v -> projectName = (String) v);

        application.canUndo().markInterested();
        application.canUndo().addValueObserver((BooleanValueChangedCallback) v -> canUndo = v);

        application.canRedo().markInterested();
        application.canRedo().addValueObserver((BooleanValueChangedCallback) v -> canRedo = v);

        application.hasActiveEngine().markInterested();
        application.hasActiveEngine().addValueObserver((BooleanValueChangedCallback) v -> hasActiveEngine = v);
    }

    public void registerClipObservers(TrackBank trackBank) {
        for (int i = 0; i < TRACK_COUNT; i++) {
            final int trackIdx = i;
            Track track = (Track) trackBank.getItemAt(i);
            ClipLauncherSlotBank slotBank = track.clipLauncherSlotBank();

            // Bank-level indexed observers
            slotBank.addHasContentObserver((IndexedBooleanValueChangedCallback) (slotIndex, value) ->
                clipHasContent[trackIdx][slotIndex] = value);

            slotBank.addIsPlayingObserver((IndexedBooleanValueChangedCallback) (slotIndex, value) ->
                clipIsPlaying[trackIdx][slotIndex] = value);

            slotBank.addIsRecordingObserver((IndexedBooleanValueChangedCallback) (slotIndex, value) ->
                clipIsRecording[trackIdx][slotIndex] = value);

            slotBank.addNameObserver((IndexedStringValueChangedCallback) (slotIndex, value) ->
                clipNames[trackIdx][slotIndex] = value);

            // Playback state observer for queued states
            slotBank.addPlaybackStateObserver((ClipLauncherSlotBankPlaybackStateChangedCallback)
                (slotIndex, playbackState, isQueued) -> {
                    clipIsPlaybackQueued[trackIdx][slotIndex] = (playbackState == 1 && isQueued);
                    clipIsRecordingQueued[trackIdx][slotIndex] = (playbackState == 2 && isQueued);
                    clipIsStopQueued[trackIdx][slotIndex] = (playbackState == 0 && isQueued);
                });

            // Per-slot color observers
            for (int j = 0; j < SCENE_COUNT; j++) {
                final int slotIdx = j;
                ClipLauncherSlot slot = (ClipLauncherSlot) slotBank.getItemAt(j);
                slot.color().markInterested();
                slot.color().addValueObserver((ColorValueChangedCallback) (r, g, b) -> {
                    clipColors[trackIdx][slotIdx][0] = r;
                    clipColors[trackIdx][slotIdx][1] = g;
                    clipColors[trackIdx][slotIdx][2] = b;
                });
            }
        }

        // Scene observers
        SceneBank sceneBank = trackBank.sceneBank();
        for (int i = 0; i < SCENE_COUNT; i++) {
            final int sceneIdx = i;
            Scene scene = sceneBank.getScene(sceneIdx);
            sceneNames[sceneIdx] = "";
            scene.name().markInterested();
            scene.name().addValueObserver((StringValueChangedCallback) v -> sceneNames[sceneIdx] = (String) v);
            scene.clipCount().markInterested();
            scene.clipCount().addValueObserver((IntegerValueChangedCallback) v -> sceneClipCounts[sceneIdx] = v);
        }
    }

    public void registerDeviceObservers(CursorTrack cursorTrack, CursorDevice cursorDevice,
                                         CursorRemoteControlsPage remoteControlsPage) {
        // Initialize param arrays
        for (int i = 0; i < PARAM_COUNT; i++) {
            paramNames[i] = "";
            paramDisplayedValues[i] = "";
        }

        // Cursor track name
        cursorTrack.name().markInterested();
        cursorTrack.name().addValueObserver((StringValueChangedCallback) v -> cursorTrackName = (String) v);

        // Device properties
        cursorDevice.name().markInterested();
        cursorDevice.name().addValueObserver((StringValueChangedCallback) v -> deviceName = (String) v);

        cursorDevice.isEnabled().markInterested();
        cursorDevice.isEnabled().addValueObserver((BooleanValueChangedCallback) v -> deviceEnabled = v);

        cursorDevice.isPlugin().markInterested();
        cursorDevice.isPlugin().addValueObserver((BooleanValueChangedCallback) v -> deviceIsPlugin = v);

        cursorDevice.position().markInterested();
        cursorDevice.position().addValueObserver((IntegerValueChangedCallback) v -> devicePosition = v);

        cursorDevice.presetName().markInterested();
        cursorDevice.presetName().addValueObserver((StringValueChangedCallback) v -> presetName = (String) v);

        cursorDevice.presetCategory().markInterested();
        cursorDevice.presetCategory().addValueObserver((StringValueChangedCallback) v -> presetCategory = (String) v);

        cursorDevice.presetCreator().markInterested();
        cursorDevice.presetCreator().addValueObserver((StringValueChangedCallback) v -> presetCreator = (String) v);

        cursorDevice.isWindowOpen().markInterested();
        cursorDevice.isWindowOpen().addValueObserver((BooleanValueChangedCallback) v -> isWindowOpen = v);

        cursorDevice.isExpanded().markInterested();
        cursorDevice.isExpanded().addValueObserver((BooleanValueChangedCallback) v -> isExpanded = v);

        // Remote controls page state
        remoteControlsPage.selectedPageIndex().markInterested();
        remoteControlsPage.selectedPageIndex().addValueObserver((IntegerValueChangedCallback) v -> pageIndex = v);

        remoteControlsPage.pageCount().markInterested();
        remoteControlsPage.pageCount().addValueObserver((IntegerValueChangedCallback) v -> pageCount = v);

        remoteControlsPage.pageNames().markInterested();
        remoteControlsPage.pageNames().addValueObserver((StringArrayValueChangedCallback) v -> devicePageNames = (String[]) v);

        // Per-parameter observers
        for (int i = 0; i < PARAM_COUNT; i++) {
            final int idx = i;
            RemoteControl param = remoteControlsPage.getParameter(i);

            param.name().markInterested();
            param.name().addValueObserver((StringValueChangedCallback) v -> paramNames[idx] = (String) v);

            param.value().markInterested();
            param.value().addValueObserver((DoubleValueChangedCallback) v -> paramValues[idx] = v);

            param.value().displayedValue().markInterested();
            param.value().displayedValue().addValueObserver((StringValueChangedCallback) v -> paramDisplayedValues[idx] = (String) v);
        }
    }

    public JsonObject getSnapshot() {
        JsonObject snapshot = new JsonObject();
        snapshot.add("transport", getTransportState());
        snapshot.add("tracks", getTracksState());
        snapshot.add("scenes", getScenesState());
        snapshot.add("device", getDeviceState());
        snapshot.add("master", getMasterState());
        snapshot.add("application", getApplicationState());
        return snapshot;
    }

    /**
     * Compare current section hashes against previous flush.
     * Returns a list of section names that changed, or empty if nothing changed.
     * Updates the stored hashes for the next comparison.
     */
    public List<String> getChangedSections() {
        List<String> changed = new ArrayList<>();

        int h;

        h = getTransportState().toString().hashCode();
        if (h != prevTransportHash) { changed.add("transport"); prevTransportHash = h; }

        h = getTracksState().toString().hashCode();
        if (h != prevTracksHash) { changed.add("tracks"); prevTracksHash = h; }

        h = getScenesState().toString().hashCode();
        if (h != prevScenesHash) { changed.add("scenes"); prevScenesHash = h; }

        h = getDeviceState().toString().hashCode();
        if (h != prevDeviceHash) { changed.add("device"); prevDeviceHash = h; }

        h = getMasterState().toString().hashCode();
        if (h != prevMasterHash) { changed.add("master"); prevMasterHash = h; }

        h = getApplicationState().toString().hashCode();
        if (h != prevApplicationHash) { changed.add("application"); prevApplicationHash = h; }

        return changed;
    }

    private JsonObject getTransportState() {
        JsonObject obj = new JsonObject();
        obj.addProperty("isPlaying", isPlaying);
        obj.addProperty("isRecording", isRecording);
        obj.addProperty("tempo", tempo);
        obj.addProperty("playPosition", playPosition);
        obj.addProperty("timeSignatureNumerator", timeSignatureNumerator);
        obj.addProperty("timeSignatureDenominator", timeSignatureDenominator);
        obj.addProperty("isLoopEnabled", isLoopEnabled);
        obj.addProperty("isMetronomeEnabled", isMetronomeEnabled);
        return obj;
    }

    private JsonArray getTracksState() {
        JsonArray arr = new JsonArray();
        for (int i = 0; i < TRACK_COUNT; i++) {
            JsonObject track = new JsonObject();
            track.addProperty("index", i);
            track.addProperty("name", trackNames[i] != null ? trackNames[i] : "");
            track.addProperty("volume", trackVolumes[i]);
            track.addProperty("pan", trackPans[i]);
            track.addProperty("mute", trackMutes[i]);
            track.addProperty("solo", trackSolos[i]);
            track.addProperty("arm", trackArms[i]);

            JsonObject color = new JsonObject();
            color.addProperty("r", trackColors[i][0]);
            color.addProperty("g", trackColors[i][1]);
            color.addProperty("b", trackColors[i][2]);
            track.add("color", color);

            JsonArray clips = new JsonArray();
            for (int j = 0; j < SCENE_COUNT; j++) {
                JsonObject clip = new JsonObject();
                clip.addProperty("slotIndex", j);
                clip.addProperty("hasContent", clipHasContent[i][j]);
                clip.addProperty("isPlaying", clipIsPlaying[i][j]);
                clip.addProperty("isRecording", clipIsRecording[i][j]);
                clip.addProperty("isPlaybackQueued", clipIsPlaybackQueued[i][j]);
                clip.addProperty("isRecordingQueued", clipIsRecordingQueued[i][j]);
                clip.addProperty("isStopQueued", clipIsStopQueued[i][j]);
                clip.addProperty("name", clipNames[i][j] != null ? clipNames[i][j] : "");

                JsonObject clipColor = new JsonObject();
                clipColor.addProperty("r", clipColors[i][j][0]);
                clipColor.addProperty("g", clipColors[i][j][1]);
                clipColor.addProperty("b", clipColors[i][j][2]);
                clip.add("color", clipColor);

                clips.add(clip);
            }
            track.add("clips", clips);

            arr.add(track);
        }
        return arr;
    }

    private JsonArray getScenesState() {
        JsonArray arr = new JsonArray();
        for (int i = 0; i < SCENE_COUNT; i++) {
            JsonObject scene = new JsonObject();
            scene.addProperty("index", i);
            scene.addProperty("name", sceneNames[i] != null ? sceneNames[i] : "");
            scene.addProperty("clipCount", sceneClipCounts[i]);
            arr.add(scene);
        }
        return arr;
    }

    private JsonObject getDeviceState() {
        JsonObject obj = new JsonObject();
        obj.addProperty("cursorTrackName", cursorTrackName);
        obj.addProperty("name", deviceName);
        obj.addProperty("isEnabled", deviceEnabled);
        obj.addProperty("isPlugin", deviceIsPlugin);
        obj.addProperty("position", devicePosition);
        obj.addProperty("presetName", presetName);
        obj.addProperty("presetCategory", presetCategory);
        obj.addProperty("presetCreator", presetCreator);
        obj.addProperty("isWindowOpen", isWindowOpen);
        obj.addProperty("isExpanded", isExpanded);

        JsonObject remoteControls = new JsonObject();
        remoteControls.addProperty("pageIndex", pageIndex);
        remoteControls.addProperty("pageCount", pageCount);

        JsonArray pageNamesArr = new JsonArray();
        String[] names = devicePageNames;
        if (names != null) {
            for (String name : names) {
                pageNamesArr.add(name != null ? name : "");
            }
        }
        remoteControls.add("pageNames", pageNamesArr);

        JsonArray params = new JsonArray();
        for (int i = 0; i < PARAM_COUNT; i++) {
            JsonObject param = new JsonObject();
            param.addProperty("index", i);
            param.addProperty("name", paramNames[i] != null ? paramNames[i] : "");
            param.addProperty("value", paramValues[i]);
            param.addProperty("displayedValue", paramDisplayedValues[i] != null ? paramDisplayedValues[i] : "");
            params.add(param);
        }
        remoteControls.add("parameters", params);

        obj.add("remoteControls", remoteControls);
        return obj;
    }

    private JsonObject getMasterState() {
        JsonObject obj = new JsonObject();
        obj.addProperty("volume", masterVolume);
        obj.addProperty("pan", masterPan);
        return obj;
    }

    private JsonObject getApplicationState() {
        JsonObject obj = new JsonObject();
        obj.addProperty("projectName", projectName);
        obj.addProperty("canUndo", canUndo);
        obj.addProperty("canRedo", canRedo);
        obj.addProperty("hasActiveEngine", hasActiveEngine);
        return obj;
    }
}
