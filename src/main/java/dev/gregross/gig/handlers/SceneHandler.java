package dev.gregross.gig.handlers;

import com.bitwig.extension.controller.api.Project;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.gregross.gig.extension.StateCache;
import dev.gregross.gig.rpc.JsonRpcDispatcher;
import dev.gregross.gig.rpc.RpcException;

public class SceneHandler {

    private static final int SCENE_COUNT = 5;

    private final SceneBank sceneBank;
    private final Project project;
    private final StateCache stateCache;

    public SceneHandler(SceneBank sceneBank, Project project, StateCache stateCache) {
        this.sceneBank = sceneBank;
        this.project = project;
        this.stateCache = stateCache;
    }

    public void register(JsonRpcDispatcher dispatcher) {
        dispatcher.register("scene/create", params -> {
            project.createScene();
            return ok();
        });

        dispatcher.register("scene/createFromPlaying", params -> {
            project.createSceneFromPlayingLauncherClips();
            return ok();
        });

        dispatcher.register("scene/duplicate", params -> {
            int index = requireInt(params, "index");
            validateIndex(index);
            Scene scene = sceneBank.getScene(index);
            scene.duplicateObject();
            return ok();
        });

        dispatcher.register("scene/rename", params -> {
            int index = requireInt(params, "index");
            validateIndex(index);
            String name = requireString(params, "name");
            Scene scene = sceneBank.getScene(index);
            scene.name().set(name);
            return ok();
        });

        dispatcher.register("scene/delete", params -> {
            int index = requireInt(params, "index");
            validateIndex(index);
            Scene scene = sceneBank.getScene(index);
            scene.deleteObject();
            return ok();
        });

        // --- Scene bank scroll methods ---

        dispatcher.register("sceneBank/scrollTo", params -> {
            int position = requireInt(params, "position");
            int itemCount = stateCache.getSceneItemCount();
            if (position < 0 || position >= itemCount) {
                JsonObject error = new JsonObject();
                error.addProperty("itemCount", itemCount);
                error.addProperty("requestedPosition", position);
                throw new RpcException(-32001, "POSITION_OUT_OF_RANGE", error);
            }
            sceneBank.scrollPosition().set(position);
            return ok();
        });

        dispatcher.register("sceneBank/scrollBy", params -> {
            int amount = requireInt(params, "amount");
            sceneBank.scrollBy(amount);
            return ok();
        });

        dispatcher.register("sceneBank/getScrollInfo", params -> {
            return stateCache.getSceneBankScrollInfo();
        });
    }

    private void validateIndex(int index) {
        if (index < 0 || index >= SCENE_COUNT) {
            throw new IllegalArgumentException("scene index out of range: " + index);
        }
    }

    private JsonObject ok() {
        JsonObject result = new JsonObject();
        result.addProperty("ok", true);
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
}
