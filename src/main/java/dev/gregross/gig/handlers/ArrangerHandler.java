package dev.gregross.gig.handlers;

import com.bitwig.extension.controller.api.Arranger;
import com.bitwig.extension.controller.api.CueMarker;
import com.bitwig.extension.controller.api.CueMarkerBank;
import com.bitwig.extension.controller.api.Transport;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.gregross.gig.rpc.JsonRpcDispatcher;

public class ArrangerHandler {

    private static final int CUE_MARKER_COUNT = 16;

    private final Arranger arranger;
    private final Transport transport;
    private final CueMarkerBank cueMarkerBank;

    public ArrangerHandler(Arranger arranger, Transport transport, CueMarkerBank cueMarkerBank) {
        this.arranger = arranger;
        this.transport = transport;
        this.cueMarkerBank = cueMarkerBank;
    }

    public void register(JsonRpcDispatcher dispatcher) {
        // --- Arranger visibility toggles ---

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

        // --- Cue marker operations ---

        dispatcher.register("cueMarker/addAtPlayhead", params -> {
            transport.addCueMarkerAtPlaybackPosition();
            return ok();
        });

        dispatcher.register("cueMarker/list", params -> {
            JsonArray markers = new JsonArray();
            for (int i = 0; i < CUE_MARKER_COUNT; i++) {
                CueMarker marker = (CueMarker) cueMarkerBank.getItemAt(i);
                if (!marker.exists().get()) continue;
                JsonObject obj = new JsonObject();
                obj.addProperty("index", i);
                obj.addProperty("name", marker.name().get());
                obj.addProperty("position", marker.position().get());
                // Color as [r, g, b]
                JsonArray color = new JsonArray();
                color.add(marker.getColor().red());
                color.add(marker.getColor().green());
                color.add(marker.getColor().blue());
                obj.add("color", color);
                markers.add(obj);
            }
            return markers;
        });

        dispatcher.register("cueMarker/launch", params -> {
            if (!params.has("index")) {
                throw new IllegalArgumentException("missing 'index' parameter");
            }
            int index = params.get("index").getAsInt();
            if (index < 0 || index >= CUE_MARKER_COUNT) {
                throw new IllegalArgumentException("index must be 0-" + (CUE_MARKER_COUNT - 1));
            }
            boolean quantized = params.has("quantized") && params.get("quantized").getAsBoolean();
            CueMarker marker = (CueMarker) cueMarkerBank.getItemAt(index);
            marker.launch(quantized);
            return ok();
        });

        dispatcher.register("cueMarker/delete", params -> {
            if (!params.has("index")) {
                throw new IllegalArgumentException("missing 'index' parameter");
            }
            int index = params.get("index").getAsInt();
            if (index < 0 || index >= CUE_MARKER_COUNT) {
                throw new IllegalArgumentException("index must be 0-" + (CUE_MARKER_COUNT - 1));
            }
            CueMarker marker = (CueMarker) cueMarkerBank.getItemAt(index);
            marker.deleteObject();
            return ok();
        });
    }

    private JsonObject ok() {
        JsonObject result = new JsonObject();
        result.addProperty("ok", true);
        return result;
    }
}
