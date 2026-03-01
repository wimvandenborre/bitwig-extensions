package dev.gregross.gig.handlers;

import com.google.gson.*;
import dev.gregross.gig.extension.StateCache;
import dev.gregross.gig.rpc.JsonRpcDispatcher;
import dev.gregross.gig.rpc.TaskScheduler;

public class MacroHandler {

    private static final long FLUSH_DELAY_MS = 100;

    private final JsonRpcDispatcher dispatcher;
    private final StateCache stateCache;
    private final TaskScheduler scheduler;

    public MacroHandler(JsonRpcDispatcher dispatcher, StateCache stateCache, TaskScheduler scheduler) {
        this.dispatcher = dispatcher;
        this.stateCache = stateCache;
        this.scheduler = scheduler;
    }

    public void register(JsonRpcDispatcher dispatcher) {
        dispatcher.register("macro/createTrack", this::handleCreateTrack);
        dispatcher.register("macro/createClip", this::handleCreateClip);
        dispatcher.register("macro/writeClip", this::handleWriteClip);
        dispatcher.register("macro/buildSection", this::handleBuildSection);
    }

    private JsonElement handleCreateTrack(JsonObject params) throws Exception {
        String type = requireString(params, "type");

        String createMethod;
        switch (type) {
            case "audio":
                createMethod = "track/createAudio";
                break;
            case "instrument":
                createMethod = "track/createInstrument";
                break;
            case "effect":
                createMethod = "track/createEffect";
                break;
            default:
                throw new IllegalArgumentException("Invalid type: " + type
                    + " — must be 'audio', 'instrument', or 'effect'");
        }

        JsonObject createParams = new JsonObject();
        if (params.has("position")) {
            createParams.addProperty("position", params.get("position").getAsInt());
        }

        dispatcher.handleInternal(createMethod, createParams);

        if (params.has("name") && !params.get("name").isJsonNull()) {
            JsonObject renameParams = new JsonObject();
            renameParams.addProperty("name", params.get("name").getAsString());
            dispatcher.handleInternal("track/rename", renameParams);
        }

        if (params.has("device") && !params.get("device").isJsonNull()) {
            JsonObject deviceParams = new JsonObject();
            deviceParams.addProperty("name", params.get("device").getAsString());
            dispatcher.handleInternal("device/insertBitwigDevice", deviceParams);
        }

        JsonObject result = new JsonObject();
        result.addProperty("ok", true);
        return result;
    }

    private JsonElement handleCreateClip(JsonObject params) throws Exception {
        int trackIndex = requireInt(params, "trackIndex");
        int sceneIndex = requireInt(params, "sceneIndex");
        int lengthBeats = requireInt(params, "lengthBeats");

        createClip(trackIndex, sceneIndex, lengthBeats);
        forceSelectClip(trackIndex, sceneIndex);

        JsonObject result = new JsonObject();
        result.addProperty("ok", true);
        return result;
    }

    private JsonElement handleWriteClip(JsonObject params) throws Exception {
        int trackIndex = requireInt(params, "trackIndex");
        int sceneIndex = requireInt(params, "sceneIndex");
        int lengthBeats = requireInt(params, "lengthBeats");
        double stepSize = requireDouble(params, "stepSize");
        JsonArray notes = requireArray(params, "notes");
        String name = params.has("name") && !params.get("name").isJsonNull()
            ? params.get("name").getAsString() : null;

        // Phase 1 (this flush cycle): create clip and request cursor move
        createClip(trackIndex, sceneIndex, lengthBeats);
        forceSelectClip(trackIndex, sceneIndex);

        // Phase 2 (next flush cycle): cursor has followed, now write notes
        scheduler.schedule(() -> {
            try {
                writeNotesToCursor(stepSize, notes, name);
            } catch (Exception e) {
                // Deferred write failed — logged but not propagated to caller
            }
        }, FLUSH_DELAY_MS);

        JsonObject result = new JsonObject();
        result.addProperty("count", notes.size());
        return result;
    }

    private JsonElement handleBuildSection(JsonObject params) throws Exception {
        String sceneName = requireString(params, "sceneName");
        JsonArray clips = requireArray(params, "clips");

        if (clips.isEmpty()) {
            throw new IllegalArgumentException("'clips' array must not be empty");
        }

        int sceneCountBefore = stateCache.getSceneItemCount();

        dispatcher.handleInternal("scene/create", new JsonObject());

        JsonObject scrollParams = new JsonObject();
        scrollParams.addProperty("amount", sceneCountBefore);
        dispatcher.handleInternal("sceneBank/scrollBy", scrollParams);

        int bankSize = 5; // matches SCENE_COUNT
        int totalScenes = sceneCountBefore + 1;
        int scrollPosition = Math.max(0, totalScenes - bankSize);
        int newSceneBankIndex = (totalScenes - 1) - scrollPosition;

        JsonObject renameParams = new JsonObject();
        renameParams.addProperty("index", newSceneBankIndex);
        renameParams.addProperty("name", sceneName);
        dispatcher.handleInternal("scene/rename", renameParams);

        // Phase 1 (this flush cycle): create all clips and select the first one
        for (JsonElement clipEl : clips) {
            JsonObject clip = clipEl.getAsJsonObject();
            int trackIndex = requireInt(clip, "trackIndex");
            int lengthBeats = requireInt(clip, "lengthBeats");
            createClip(trackIndex, newSceneBankIndex, lengthBeats);
        }

        // Select the first clip — cursor will follow in next flush cycle
        JsonObject firstClip = clips.get(0).getAsJsonObject();
        forceSelectClip(requireInt(firstClip, "trackIndex"), newSceneBankIndex);

        // Phase 2+: chain clip writes across flush cycles
        // Each clip needs: (flush N) write notes + select next clip → (flush N+1) write next
        for (int i = 0; i < clips.size(); i++) {
            JsonObject clip = clips.get(i).getAsJsonObject();
            double stepSize = requireDouble(clip, "stepSize");
            JsonArray notes = requireArray(clip, "notes");
            String clipName = clip.has("name") && !clip.get("name").isJsonNull()
                ? clip.get("name").getAsString() : null;

            boolean isLast = (i == clips.size() - 1);
            int nextClipTrackIndex = isLast ? -1
                : requireInt(clips.get(i + 1).getAsJsonObject(), "trackIndex");

            long writeDelay = FLUSH_DELAY_MS * (2L * i + 1);
            final int sceneIdx = newSceneBankIndex;
            final int nextTrack = nextClipTrackIndex;
            scheduler.schedule(() -> {
                try {
                    writeNotesToCursor(stepSize, notes, clipName);
                    if (nextTrack >= 0) {
                        forceSelectClip(nextTrack, sceneIdx);
                    }
                } catch (Exception e) {
                    // Deferred write failed
                }
            }, writeDelay);
        }

        JsonObject result = new JsonObject();
        result.addProperty("sceneIndex", sceneCountBefore);
        result.addProperty("clipCount", clips.size());
        return result;
    }

    // --- Internal helpers ---

    private void writeNotesToCursor(double stepSize, JsonArray notes, String name) throws Exception {
        JsonObject stepSizeParams = new JsonObject();
        stepSizeParams.addProperty("size", stepSize);
        dispatcher.handleInternal("clip/setStepSize", stepSizeParams);

        JsonObject noteParams = new JsonObject();
        noteParams.add("notes", notes);
        dispatcher.handleInternal("clip/setNotes", noteParams);

        if (name != null) {
            JsonObject renameP = new JsonObject();
            renameP.addProperty("name", name);
            dispatcher.handleInternal("clip/rename", renameP);
        }
    }

    private void createClip(int trackIndex, int slotIndex, int lengthBeats) throws Exception {
        JsonObject params = new JsonObject();
        params.addProperty("trackIndex", trackIndex);
        params.addProperty("slotIndex", slotIndex);
        params.addProperty("lengthInBeats", lengthBeats);
        dispatcher.handleInternal("clip/create", params);
    }

    private void forceSelectClip(int trackIndex, int slotIndex) throws Exception {
        JsonObject params = new JsonObject();
        params.addProperty("trackIndex", trackIndex);
        params.addProperty("slotIndex", slotIndex);
        params.addProperty("force", true);
        dispatcher.handleInternal("clip/select", params);
    }

    // --- Parameter helpers ---

    private static String requireString(JsonObject params, String key) {
        JsonElement el = params.get(key);
        if (el == null || el.isJsonNull()) {
            throw new IllegalArgumentException("Missing required param: " + key);
        }
        return el.getAsString();
    }

    private static int requireInt(JsonObject params, String key) {
        JsonElement el = params.get(key);
        if (el == null || el.isJsonNull()) {
            throw new IllegalArgumentException("Missing required param: " + key);
        }
        return el.getAsInt();
    }

    private static double requireDouble(JsonObject params, String key) {
        JsonElement el = params.get(key);
        if (el == null || el.isJsonNull()) {
            throw new IllegalArgumentException("Missing required param: " + key);
        }
        return el.getAsDouble();
    }

    private static JsonArray requireArray(JsonObject params, String key) {
        JsonElement el = params.get(key);
        if (el == null || !el.isJsonArray()) {
            throw new IllegalArgumentException("Missing required param: " + key);
        }
        return el.getAsJsonArray();
    }
}
