package dev.gregross.gig.handlers;

import com.google.gson.*;
import dev.gregross.gig.rpc.JsonRpcDispatcher;

public class MacroHandler {

    private final JsonRpcDispatcher dispatcher;

    public MacroHandler(JsonRpcDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public void register(JsonRpcDispatcher dispatcher) {
        dispatcher.register("macro/createTrack", this::handleCreateTrack);
        dispatcher.register("macro/createClip", this::handleCreateClip);
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
}
