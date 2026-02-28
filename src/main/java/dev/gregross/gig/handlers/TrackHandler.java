package dev.gregross.gig.handlers;

import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.google.gson.JsonPrimitive;
import dev.gregross.gig.rpc.JsonRpcDispatcher;

public class TrackHandler {

    private final TrackBank trackBank;

    public TrackHandler(TrackBank trackBank) {
        this.trackBank = trackBank;
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
    }

    private Track getTrack(int index) {
        if (index < 0 || index >= trackBank.getSizeOfBank()) {
            throw new IllegalArgumentException("track index out of range: " + index);
        }
        return (Track) trackBank.getItemAt(index);
    }
}
