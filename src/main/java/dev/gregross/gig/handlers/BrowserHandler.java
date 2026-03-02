package dev.gregross.gig.handlers;

import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.PopupBrowser;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.gregross.gig.extension.StateCache;
import dev.gregross.gig.rpc.JsonRpcDispatcher;

public class BrowserHandler {

    private final PopupBrowser popupBrowser;
    private final CursorDevice cursorDevice;
    private final StateCache stateCache;

    public BrowserHandler(PopupBrowser popupBrowser, CursorDevice cursorDevice,
                           StateCache stateCache) {
        this.popupBrowser = popupBrowser;
        this.cursorDevice = cursorDevice;
        this.stateCache = stateCache;
    }

    public void register(JsonRpcDispatcher dispatcher) {
        // Browser opening — must use InsertionPoint.browse() to open
        dispatcher.register("browser/browsePresets", params -> {
            cursorDevice.replaceDeviceInsertionPoint().browse();
            return new JsonPrimitive("ok");
        });

        dispatcher.register("browser/browseInsertDevice", params -> {
            cursorDevice.afterDeviceInsertionPoint().browse();
            return new JsonPrimitive("ok");
        });

        // Result navigation
        dispatcher.register("browser/selectNextFile", params -> {
            popupBrowser.selectNextFile();
            return new JsonPrimitive("ok");
        });

        dispatcher.register("browser/selectPreviousFile", params -> {
            popupBrowser.selectPreviousFile();
            return new JsonPrimitive("ok");
        });

        dispatcher.register("browser/selectFirstFile", params -> {
            popupBrowser.selectFirstFile();
            return new JsonPrimitive("ok");
        });

        dispatcher.register("browser/selectLastFile", params -> {
            popupBrowser.selectLastFile();
            return new JsonPrimitive("ok");
        });

        // Commit / cancel
        dispatcher.register("browser/commit", params -> {
            popupBrowser.commit();
            return new JsonPrimitive("ok");
        });

        dispatcher.register("browser/cancel", params -> {
            popupBrowser.cancel();
            return new JsonPrimitive("ok");
        });

        // Content type switching
        dispatcher.register("browser/setContentType", params -> {
            int index = requireInt(params, "index");
            popupBrowser.selectedContentTypeIndex().set(index);
            return new JsonPrimitive("ok");
        });

        // Audition toggle
        dispatcher.register("browser/setShouldAudition", params -> {
            boolean enabled = requireBoolean(params, "enabled");
            popupBrowser.shouldAudition().set(enabled);
            return new JsonPrimitive("ok");
        });

        // State query
        dispatcher.register("browser/getState", params -> stateCache.getBrowserState());
    }

    private static int requireInt(JsonObject params, String key) {
        JsonElement el = params.get(key);
        if (el == null || el.isJsonNull()) {
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }
        return el.getAsInt();
    }

    private static boolean requireBoolean(JsonObject params, String key) {
        JsonElement el = params.get(key);
        if (el == null || el.isJsonNull()) {
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }
        return el.getAsBoolean();
    }
}
