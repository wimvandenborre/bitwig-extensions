package dev.gregross.gig.handlers;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.gregross.gig.rpc.JsonRpcDispatcher;

public class ClipHandler {

    private static final int SCENE_COUNT = 8;

    private final TrackBank trackBank;
    private final SceneBank sceneBank;

    public ClipHandler(TrackBank trackBank, SceneBank sceneBank) {
        this.trackBank = trackBank;
        this.sceneBank = sceneBank;
    }

    public void register(JsonRpcDispatcher dispatcher) {
        dispatcher.register("clip/launch", params -> {
            int trackIndex = requireInt(params, "trackIndex");
            int slotIndex = requireInt(params, "slotIndex");
            getSlotBank(trackIndex).launch(slotIndex);
            return new JsonPrimitive("ok");
        });

        dispatcher.register("clip/stop", params -> {
            int trackIndex = requireInt(params, "trackIndex");
            getSlotBank(trackIndex).stop();
            return new JsonPrimitive("ok");
        });

        dispatcher.register("clip/record", params -> {
            int trackIndex = requireInt(params, "trackIndex");
            int slotIndex = requireInt(params, "slotIndex");
            getSlotBank(trackIndex).record(slotIndex);
            return new JsonPrimitive("ok");
        });

        dispatcher.register("clip/create", params -> {
            int trackIndex = requireInt(params, "trackIndex");
            int slotIndex = requireInt(params, "slotIndex");
            int lengthInBeats = requireInt(params, "lengthInBeats");
            getSlotBank(trackIndex).createEmptyClip(slotIndex, lengthInBeats);
            return new JsonPrimitive("ok");
        });

        dispatcher.register("clip/select", params -> {
            int trackIndex = requireInt(params, "trackIndex");
            int slotIndex = requireInt(params, "slotIndex");
            ClipLauncherSlotBank slotBank = getSlotBank(trackIndex);
            ClipLauncherSlot slot = (ClipLauncherSlot) slotBank.getItemAt(slotIndex);
            slot.select();
            return new JsonPrimitive("ok");
        });

        dispatcher.register("clip/delete", params -> {
            int trackIndex = requireInt(params, "trackIndex");
            int slotIndex = requireInt(params, "slotIndex");
            ClipLauncherSlotBank slotBank = getSlotBank(trackIndex);
            ClipLauncherSlot slot = (ClipLauncherSlot) slotBank.getItemAt(slotIndex);
            slot.deleteObject();
            return new JsonPrimitive("ok");
        });

        dispatcher.register("scene/launch", params -> {
            int index = requireInt(params, "index");
            if (index < 0 || index >= SCENE_COUNT) {
                throw new IllegalArgumentException("scene index out of range: " + index);
            }
            sceneBank.launchScene(index);
            return new JsonPrimitive("ok");
        });
    }

    private ClipLauncherSlotBank getSlotBank(int trackIndex) {
        if (trackIndex < 0 || trackIndex >= trackBank.getSizeOfBank()) {
            throw new IllegalArgumentException("track index out of range: " + trackIndex);
        }
        Track track = (Track) trackBank.getItemAt(trackIndex);
        return track.clipLauncherSlotBank();
    }

    private int requireInt(JsonObject params, String key) {
        JsonElement el = params.get(key);
        if (el == null) {
            throw new IllegalArgumentException("missing '" + key + "' parameter");
        }
        return el.getAsInt();
    }
}
