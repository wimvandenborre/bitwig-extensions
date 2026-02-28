package dev.gregross.gig.handlers;

import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.gregross.gig.rpc.JsonRpcDispatcher;

public class TrackHandler {

    private final TrackBank trackBank;
    private final Application application;
    private final CursorTrack cursorTrack;
    private final TrackBankManager trackBankManager;

    public TrackHandler(TrackBank trackBank, Application application,
                        CursorTrack cursorTrack, TrackBankManager trackBankManager) {
        this.trackBank = trackBank;
        this.application = application;
        this.cursorTrack = cursorTrack;
        this.trackBankManager = trackBankManager;
    }

    public void register(JsonRpcDispatcher dispatcher) {
        dispatcher.register("track/setVolume", params -> {
            Track track = getTrack(params.get("index").getAsInt());
            double value = params.get("value").getAsDouble();
            track.volume().value().setImmediately(value);
            return new JsonPrimitive("ok");
        });

        dispatcher.register("track/setPan", params -> {
            Track track = getTrack(params.get("index").getAsInt());
            double value = params.get("value").getAsDouble();
            track.pan().value().setImmediately(value);
            return new JsonPrimitive("ok");
        });

        dispatcher.register("track/setMute", params -> {
            Track track = getTrack(params.get("index").getAsInt());
            boolean muted = params.get("muted").getAsBoolean();
            track.mute().set(muted);
            return new JsonPrimitive("ok");
        });

        dispatcher.register("track/setSolo", params -> {
            Track track = getTrack(params.get("index").getAsInt());
            boolean soloed = params.get("soloed").getAsBoolean();
            track.solo().set(soloed);
            return new JsonPrimitive("ok");
        });

        dispatcher.register("track/setArm", params -> {
            Track track = getTrack(params.get("index").getAsInt());
            boolean armed = params.get("armed").getAsBoolean();
            track.arm().set(armed);
            return new JsonPrimitive("ok");
        });

        // --- Phase 6: Track management methods ---

        dispatcher.register("track/createAudio", params -> {
            int position = optionalInt(params, "position", -1);
            application.createAudioTrack(position);
            return cursorResponse();
        });

        dispatcher.register("track/createInstrument", params -> {
            int position = optionalInt(params, "position", -1);
            application.createInstrumentTrack(position);
            return cursorResponse();
        });

        dispatcher.register("track/createEffect", params -> {
            int position = optionalInt(params, "position", -1);
            application.createEffectTrack(position);
            return cursorResponse();
        });

        dispatcher.register("track/select", params -> {
            int index = requireInt(params, "index");
            trackBankManager.selectByIndex(index);
            return cursorResponse();
        });

        dispatcher.register("track/rename", params -> {
            String name = requireString(params, "name");
            cursorTrack.name().set(name);
            return cursorResponse();
        });

        dispatcher.register("track/deleteSelected", params -> {
            cursorTrack.deleteObject();
            JsonObject result = new JsonObject();
            result.addProperty("ok", true);
            return result;
        });

        dispatcher.register("track/duplicate", params -> {
            cursorTrack.duplicate();
            return cursorResponse();
        });
    }

    private Track getTrack(int index) {
        if (index < 0 || index >= trackBank.getSizeOfBank()) {
            throw new IllegalArgumentException("track index out of range: " + index);
        }
        return (Track) trackBank.getItemAt(index);
    }

    private JsonObject cursorResponse() {
        JsonObject result = new JsonObject();
        result.addProperty("ok", true);
        result.addProperty("cursorTrackName", cursorTrack.name().get());
        return result;
    }

    private int requireInt(JsonObject params, String key) {
        JsonElement el = params.get(key);
        if (el == null) {
            throw new IllegalArgumentException("missing '" + key + "' parameter");
        }
        return el.getAsInt();
    }

    private String requireString(JsonObject params, String key) {
        JsonElement el = params.get(key);
        if (el == null) {
            throw new IllegalArgumentException("missing '" + key + "' parameter");
        }
        return el.getAsString();
    }

    private int optionalInt(JsonObject params, String key, int defaultValue) {
        JsonElement el = params.get(key);
        if (el == null || el.isJsonNull()) {
            return defaultValue;
        }
        return el.getAsInt();
    }
}
