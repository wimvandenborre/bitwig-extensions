package dev.gregross.gig.handlers;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.gregross.gig.extension.StateCache;
import dev.gregross.gig.rpc.JsonRpcDispatcher;

public class ClipHandler {

    private static final int SCENE_COUNT = 5;

    private final TrackBank trackBank;
    private final SceneBank sceneBank;
    private final Clip cursorClip;
    private final StateCache stateCache;

    public ClipHandler(TrackBank trackBank, SceneBank sceneBank, Clip cursorClip, StateCache stateCache) {
        this.trackBank = trackBank;
        this.sceneBank = sceneBank;
        this.cursorClip = cursorClip;
        this.stateCache = stateCache;
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
            if (!stateCache.clipHasContent(trackIndex, slotIndex)) {
                throw new IllegalArgumentException(
                    "slot is empty at track " + trackIndex + " slot " + slotIndex
                    + " — create a clip first with clip/create");
            }
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

        dispatcher.register("clip/rename", params -> {
            JsonElement nameEl = params.get("name");
            if (nameEl == null) {
                throw new IllegalArgumentException("missing 'name' parameter");
            }
            cursorClip.setName(nameEl.getAsString());
            return new JsonPrimitive("ok");
        });

        dispatcher.register("clip/duplicate", params -> {
            int trackIndex = requireInt(params, "trackIndex");
            int slotIndex = requireInt(params, "slotIndex");
            getSlotBank(trackIndex).duplicateClip(slotIndex);
            return new JsonPrimitive("ok");
        });

        dispatcher.register("clip/duplicateToSlot", params -> {
            int srcTrackIndex = requireInt(params, "srcTrackIndex");
            int srcSlotIndex = requireInt(params, "srcSlotIndex");
            int destTrackIndex = requireInt(params, "destTrackIndex");
            int destSlotIndex = requireInt(params, "destSlotIndex");
            ClipLauncherSlot sourceSlot = (ClipLauncherSlot) getSlotBank(srcTrackIndex).getItemAt(srcSlotIndex);
            ClipLauncherSlot destSlot = (ClipLauncherSlot) getSlotBank(destTrackIndex).getItemAt(destSlotIndex);
            destSlot.replaceInsertionPoint().copySlotsOrScenes(sourceSlot);
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
