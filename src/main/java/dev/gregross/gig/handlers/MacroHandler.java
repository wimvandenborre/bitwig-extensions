package dev.gregross.gig.handlers;

import com.google.gson.*;
import dev.gregross.gig.extension.StateCache;
import dev.gregross.gig.rpc.JsonRpcDispatcher;

public class MacroHandler {

    private final JsonRpcDispatcher dispatcher;
    private final StateCache stateCache;

    public MacroHandler(JsonRpcDispatcher dispatcher, StateCache stateCache) {
        this.dispatcher = dispatcher;
        this.stateCache = stateCache;
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

        createClip(trackIndex, sceneIndex, lengthBeats);
        forceSelectClip(trackIndex, sceneIndex);

        JsonObject stepSizeParams = new JsonObject();
        stepSizeParams.addProperty("size", stepSize);
        dispatcher.handleInternal("clip/setStepSize", stepSizeParams);

        JsonObject noteParams = new JsonObject();
        noteParams.add("notes", notes);
        dispatcher.handleInternal("clip/setNotes", noteParams);

        if (params.has("name") && !params.get("name").isJsonNull()) {
            JsonObject renameParams = new JsonObject();
            renameParams.addProperty("name", params.get("name").getAsString());
            dispatcher.handleInternal("clip/rename", renameParams);
        }

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

        // Scroll scene bank to the end so the new scene is visible.
        // Use scrollBy with a large positive amount — Bitwig clamps to valid range.
        // We can't use scrollTo because the itemCount observer hasn't updated yet.
        JsonObject scrollParams = new JsonObject();
        scrollParams.addProperty("amount", sceneCountBefore);
        dispatcher.handleInternal("sceneBank/scrollBy", scrollParams);

        // The new scene is the last one. After scrolling to the end,
        // its bank-relative index depends on how many scenes fit in the window.
        // With bankSize=5 and N+1 total scenes, the last scene is at offset (N+1)-1 - scrollPosition.
        // Since scrollBy clamps, we calculate the bank-relative index from the scene count.
        int bankSize = 5; // matches SCENE_COUNT
        int totalScenes = sceneCountBefore + 1;
        int scrollPosition = Math.max(0, totalScenes - bankSize);
        int newSceneBankIndex = (totalScenes - 1) - scrollPosition;

        JsonObject renameParams = new JsonObject();
        renameParams.addProperty("index", newSceneBankIndex);
        renameParams.addProperty("name", sceneName);
        dispatcher.handleInternal("scene/rename", renameParams);

        int clipCount = 0;
        for (JsonElement clipEl : clips) {
            JsonObject clip = clipEl.getAsJsonObject();
            int trackIndex = requireInt(clip, "trackIndex");
            int lengthBeats = requireInt(clip, "lengthBeats");
            double stepSize = requireDouble(clip, "stepSize");
            JsonArray notes = requireArray(clip, "notes");

            createClip(trackIndex, newSceneBankIndex, lengthBeats);
            forceSelectClip(trackIndex, newSceneBankIndex);

            JsonObject stepSizeParams = new JsonObject();
            stepSizeParams.addProperty("size", stepSize);
            dispatcher.handleInternal("clip/setStepSize", stepSizeParams);

            JsonObject noteParams = new JsonObject();
            noteParams.add("notes", notes);
            dispatcher.handleInternal("clip/setNotes", noteParams);

            if (clip.has("name") && !clip.get("name").isJsonNull()) {
                JsonObject clipRenameParams = new JsonObject();
                clipRenameParams.addProperty("name", clip.get("name").getAsString());
                dispatcher.handleInternal("clip/rename", clipRenameParams);
            }

            clipCount++;
        }

        JsonObject result = new JsonObject();
        result.addProperty("sceneIndex", sceneCountBefore);
        result.addProperty("clipCount", clipCount);
        return result;
    }

    // --- Internal helpers ---

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
