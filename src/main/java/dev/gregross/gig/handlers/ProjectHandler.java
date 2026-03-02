package dev.gregross.gig.handlers;

import com.bitwig.extension.controller.api.Project;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.gregross.gig.extension.StateCache;
import dev.gregross.gig.rpc.JsonRpcDispatcher;

public class ProjectHandler {

    private final Project project;
    private final StateCache stateCache;

    public ProjectHandler(Project project, StateCache stateCache) {
        this.project = project;
        this.stateCache = stateCache;
    }

    public void register(JsonRpcDispatcher dispatcher) {
        dispatcher.register("project/unsoloAll", params -> {
            project.unsoloAll();
            return new JsonPrimitive("ok");
        });

        dispatcher.register("project/unmuteAll", params -> {
            project.unmuteAll();
            return new JsonPrimitive("ok");
        });

        dispatcher.register("project/unarmAll", params -> {
            project.unarmAll();
            return new JsonPrimitive("ok");
        });

        dispatcher.register("project/getState", params -> {
            JsonObject state = new JsonObject();
            state.addProperty("hasSoloedTracks", stateCache.hasSoloedTracks());
            state.addProperty("hasMutedTracks", stateCache.hasMutedTracks());
            state.addProperty("hasArmedTracks", stateCache.hasArmedTracks());
            state.addProperty("isModified", stateCache.isModified());
            return state;
        });
    }
}
