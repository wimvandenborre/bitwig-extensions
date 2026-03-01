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

        // Determine the track creation method
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

        // Build create params
        JsonObject createParams = new JsonObject();
        if (params.has("position")) {
            createParams.addProperty("position", params.get("position").getAsInt());
        }

        // Create the track
        dispatcher.handleInternal(createMethod, createParams);

        // Rename if name provided
        if (params.has("name") && !params.get("name").isJsonNull()) {
            JsonObject renameParams = new JsonObject();
            renameParams.addProperty("name", params.get("name").getAsString());
            dispatcher.handleInternal("track/rename", renameParams);
        }

        // Insert device if provided
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

        // Create the clip
        JsonObject createParams = new JsonObject();
        createParams.addProperty("trackIndex", trackIndex);
        createParams.addProperty("slotIndex", sceneIndex);
        createParams.addProperty("lengthInBeats", lengthBeats);
        dispatcher.handleInternal("clip/create", createParams);

        // Select the clip (cursor clip ready for note writing)
        JsonObject selectParams = new JsonObject();
        selectParams.addProperty("trackIndex", trackIndex);
        selectParams.addProperty("slotIndex", sceneIndex);
        dispatcher.handleInternal("clip/select", selectParams);

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

        // Create the clip
        JsonObject createParams = new JsonObject();
        createParams.addProperty("trackIndex", trackIndex);
        createParams.addProperty("slotIndex", sceneIndex);
        createParams.addProperty("lengthInBeats", lengthBeats);
        dispatcher.handleInternal("clip/create", createParams);

        // Select the clip
        JsonObject selectParams = new JsonObject();
        selectParams.addProperty("trackIndex", trackIndex);
        selectParams.addProperty("slotIndex", sceneIndex);
        dispatcher.handleInternal("clip/select", selectParams);

        // Set step size
        JsonObject stepSizeParams = new JsonObject();
        stepSizeParams.addProperty("size", stepSize);
        dispatcher.handleInternal("clip/setStepSize", stepSizeParams);

        // Write notes
        JsonObject noteParams = new JsonObject();
        noteParams.add("notes", notes);
        dispatcher.handleInternal("clip/setNotes", noteParams);

        // Rename if provided
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

        // Record scene count before creation to find the new scene's absolute index
        int sceneCountBefore = stateCache.getSceneItemCount();

        // Create a new scene
        dispatcher.handleInternal("scene/create", new JsonObject());

        // Scroll scene bank so the new scene is visible
        // New scene is at absolute index = sceneCountBefore (0-based)
        JsonObject scrollParams = new JsonObject();
        scrollParams.addProperty("position", sceneCountBefore);
        dispatcher.handleInternal("sceneBank/scrollTo", scrollParams);

        // The new scene's bank-relative index is 0 (first in the scrolled window)
        int newSceneBankIndex = 0;

        // Rename the scene
        JsonObject renameParams = new JsonObject();
        renameParams.addProperty("index", newSceneBankIndex);
        renameParams.addProperty("name", sceneName);
        dispatcher.handleInternal("scene/rename", renameParams);

        // Write clips into the new scene
        int clipCount = 0;
        for (JsonElement clipEl : clips) {
            JsonObject clip = clipEl.getAsJsonObject();
            int trackIndex = requireInt(clip, "trackIndex");
            int lengthBeats = requireInt(clip, "lengthBeats");
            double stepSize = requireDouble(clip, "stepSize");
            JsonArray notes = requireArray(clip, "notes");

            // Create clip in this track at the new scene's slot
            JsonObject createParams = new JsonObject();
            createParams.addProperty("trackIndex", trackIndex);
            createParams.addProperty("slotIndex", newSceneBankIndex);
            createParams.addProperty("lengthInBeats", lengthBeats);
            dispatcher.handleInternal("clip/create", createParams);

            // Select clip
            JsonObject selectParams = new JsonObject();
            selectParams.addProperty("trackIndex", trackIndex);
            selectParams.addProperty("slotIndex", newSceneBankIndex);
            dispatcher.handleInternal("clip/select", selectParams);

            // Set step size
            JsonObject stepSizeParams = new JsonObject();
            stepSizeParams.addProperty("size", stepSize);
            dispatcher.handleInternal("clip/setStepSize", stepSizeParams);

            // Write notes
            JsonObject noteParams = new JsonObject();
            noteParams.add("notes", notes);
            dispatcher.handleInternal("clip/setNotes", noteParams);

            // Rename clip if provided
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
