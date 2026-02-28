package dev.gregross.gig.handlers;

import com.bitwig.extension.controller.api.Arranger;
import com.google.gson.JsonObject;
import dev.gregross.gig.rpc.JsonRpcDispatcher;

public class ArrangerHandler {

    private final Arranger arranger;

    public ArrangerHandler(Arranger arranger) {
        this.arranger = arranger;
    }

    public void register(JsonRpcDispatcher dispatcher) {
        dispatcher.register("arranger/setPlaybackFollow", params -> {
            if (!params.has("enabled")) {
                throw new IllegalArgumentException("missing 'enabled' parameter");
            }
            arranger.isPlaybackFollowEnabled().set(params.get("enabled").getAsBoolean());
            return ok();
        });

        dispatcher.register("arranger/setClipLauncherVisible", params -> {
            if (!params.has("enabled")) {
                throw new IllegalArgumentException("missing 'enabled' parameter");
            }
            arranger.isClipLauncherVisible().set(params.get("enabled").getAsBoolean());
            return ok();
        });

        dispatcher.register("arranger/setTimelineVisible", params -> {
            if (!params.has("enabled")) {
                throw new IllegalArgumentException("missing 'enabled' parameter");
            }
            arranger.isTimelineVisible().set(params.get("enabled").getAsBoolean());
            return ok();
        });

        dispatcher.register("arranger/setCueMarkersVisible", params -> {
            if (!params.has("enabled")) {
                throw new IllegalArgumentException("missing 'enabled' parameter");
            }
            arranger.areCueMarkersVisible().set(params.get("enabled").getAsBoolean());
            return ok();
        });

        dispatcher.register("arranger/setEffectTracksVisible", params -> {
            if (!params.has("enabled")) {
                throw new IllegalArgumentException("missing 'enabled' parameter");
            }
            arranger.areEffectTracksVisible().set(params.get("enabled").getAsBoolean());
            return ok();
        });

        dispatcher.register("arranger/setIoSectionVisible", params -> {
            if (!params.has("enabled")) {
                throw new IllegalArgumentException("missing 'enabled' parameter");
            }
            arranger.isIoSectionVisible().set(params.get("enabled").getAsBoolean());
            return ok();
        });

        dispatcher.register("arranger/setDoubleRowTrackHeight", params -> {
            if (!params.has("enabled")) {
                throw new IllegalArgumentException("missing 'enabled' parameter");
            }
            arranger.hasDoubleRowTrackHeight().set(params.get("enabled").getAsBoolean());
            return ok();
        });
    }

    private JsonObject ok() {
        JsonObject result = new JsonObject();
        result.addProperty("ok", true);
        return result;
    }
}
