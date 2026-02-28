package dev.gregross.gig.handlers;

import com.bitwig.extension.controller.api.MasterTrack;
import com.google.gson.JsonPrimitive;
import dev.gregross.gig.rpc.JsonRpcDispatcher;

public class MasterHandler {

    private final MasterTrack masterTrack;

    public MasterHandler(MasterTrack masterTrack) {
        this.masterTrack = masterTrack;
    }

    public void register(JsonRpcDispatcher dispatcher) {
        dispatcher.register("master/setVolume", params -> {
            if (!params.has("value")) {
                throw new IllegalArgumentException("missing 'value' parameter");
            }
            double value = params.get("value").getAsDouble();
            masterTrack.volume().value().setImmediately(value);
            return new JsonPrimitive("ok");
        });

        dispatcher.register("master/setPan", params -> {
            if (!params.has("value")) {
                throw new IllegalArgumentException("missing 'value' parameter");
            }
            double value = params.get("value").getAsDouble();
            masterTrack.pan().value().setImmediately(value);
            return new JsonPrimitive("ok");
        });
    }
}
