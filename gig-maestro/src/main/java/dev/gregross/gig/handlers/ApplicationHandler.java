package dev.gregross.gig.handlers;

import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.ControllerHost;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.gregross.gig.rpc.JsonRpcDispatcher;

import java.util.Set;

public class ApplicationHandler {

    private final Application application;
    private final ControllerHost host;

    public ApplicationHandler(Application application, ControllerHost host) {
        this.application = application;
        this.host = host;
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
            state.addProperty("panelLayout", application.panelLayout().get());
            return state;
        });

        dispatcher.register("app/activateEngine", params -> {
            application.activateEngine();
            return new JsonPrimitive("ok");
        });

        dispatcher.register("app/deactivateEngine", params -> {
            application.deactivateEngine();
            return new JsonPrimitive("ok");
        });

        dispatcher.register("app/showNotification", params -> {
            if (!params.has("text")) {
                throw new IllegalArgumentException("missing 'text' parameter");
            }
            host.showPopupNotification(params.get("text").getAsString());
            return new JsonPrimitive("ok");
        });

        dispatcher.register("app/setPanelLayout", params -> {
            if (!params.has("layout")) {
                throw new IllegalArgumentException("missing 'layout' parameter");
            }
            String layout = params.get("layout").getAsString();
            if (!PANEL_LAYOUTS.contains(layout)) {
                throw new IllegalArgumentException("invalid layout '" + layout + "', expected: ARRANGE, MIX, EDIT");
            }
            application.setPanelLayout(layout);
            return new JsonPrimitive("ok");
        });
    }

    private static final Set<String> PANEL_LAYOUTS = Set.of("ARRANGE", "MIX", "EDIT");
}
