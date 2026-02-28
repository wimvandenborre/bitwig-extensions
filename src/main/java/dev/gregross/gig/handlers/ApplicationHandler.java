package dev.gregross.gig.handlers;

import com.bitwig.extension.controller.api.Application;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.gregross.gig.rpc.JsonRpcDispatcher;

public class ApplicationHandler {

    private final Application application;

    public ApplicationHandler(Application application) {
        this.application = application;
    }

    public void register(JsonRpcDispatcher dispatcher) {
        dispatcher.register("app/undo", params -> {
            application.undo();
            return new JsonPrimitive("ok");
        });

        dispatcher.register("app/redo", params -> {
            application.redo();
            return new JsonPrimitive("ok");
        });

        dispatcher.register("app/getState", params -> {
            JsonObject state = new JsonObject();
            state.addProperty("projectName", application.projectName().get());
            state.addProperty("canUndo", application.canUndo().get());
            state.addProperty("canRedo", application.canRedo().get());
            state.addProperty("hasActiveEngine", application.hasActiveEngine().get());
            return state;
        });
    }
}
