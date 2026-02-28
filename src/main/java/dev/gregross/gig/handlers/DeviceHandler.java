package dev.gregross.gig.handlers;

import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.InsertionPoint;
import com.bitwig.extension.controller.api.RemoteControl;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.gregross.gig.rpc.JsonRpcDispatcher;

import java.nio.file.Path;

public class DeviceHandler {

    private static final int PARAM_COUNT = 8;

    private final CursorTrack cursorTrack;
    private final CursorDevice cursorDevice;
    private final CursorRemoteControlsPage remoteControlsPage;
    private final DeviceLibrary deviceLibrary;

    public DeviceHandler(CursorTrack cursorTrack, CursorDevice cursorDevice,
                         CursorRemoteControlsPage remoteControlsPage,
                         DeviceLibrary deviceLibrary) {
        this.cursorTrack = cursorTrack;
        this.cursorDevice = cursorDevice;
        this.remoteControlsPage = remoteControlsPage;
        this.deviceLibrary = deviceLibrary;
    }

    public void register(JsonRpcDispatcher dispatcher) {
        // Device chain navigation
        dispatcher.register("device/selectNext", params -> {
            cursorDevice.selectNext();
            return new JsonPrimitive("ok");
        });

        dispatcher.register("device/selectPrevious", params -> {
            cursorDevice.selectPrevious();
            return new JsonPrimitive("ok");
        });

        // Device state
        dispatcher.register("device/setEnabled", params -> {
            boolean enabled = requireBoolean(params, "enabled");
            cursorDevice.isEnabled().set(enabled);
            return new JsonPrimitive("ok");
        });

        // Parameter page navigation
        dispatcher.register("device/selectPage", params -> {
            int index = requireInt(params, "index");
            remoteControlsPage.selectedPageIndex().set(index);
            return new JsonPrimitive("ok");
        });

        dispatcher.register("device/nextPage", params -> {
            remoteControlsPage.selectNextPage(false);
            return new JsonPrimitive("ok");
        });

        dispatcher.register("device/previousPage", params -> {
            remoteControlsPage.selectPreviousPage(false);
            return new JsonPrimitive("ok");
        });

        // Parameter mutation
        dispatcher.register("device/setParameterValue", params -> {
            int index = requireInt(params, "index");
            double value = requireDouble(params, "value");
            if (index < 0 || index >= PARAM_COUNT) {
                throw new IllegalArgumentException("parameter index out of range: " + index);
            }
            RemoteControl param = remoteControlsPage.getParameter(index);
            param.value().setImmediately(value);
            return new JsonPrimitive("ok");
        });

        // Device insertion
        dispatcher.register("device/insertBitwigDevice", params -> {
            String name = requireString(params, "name");
            String position = optionalString(params, "position", "end");
            Path devicePath = deviceLibrary.resolve(name);
            getInsertionPoint(position).insertFile(devicePath.toString());
            return new JsonPrimitive("ok");
        });

        dispatcher.register("device/insertPluginDevice", params -> {
            String type = requireString(params, "type");
            String id = requireString(params, "id");
            String position = optionalString(params, "position", "end");
            InsertionPoint ip = getInsertionPoint(position);
            switch (type) {
                case "vst2":
                    ip.insertVST2Device(Integer.parseInt(id));
                    break;
                case "vst3":
                    ip.insertVST3Device(id);
                    break;
                case "clap":
                    ip.insertCLAPDevice(id);
                    break;
                default:
                    throw new IllegalArgumentException("type must be 'vst2', 'vst3', or 'clap', got: " + type);
            }
            return new JsonPrimitive("ok");
        });

        dispatcher.register("device/listBitwigDevices", params -> {
            JsonArray arr = new JsonArray();
            for (String name : deviceLibrary.listDevices()) {
                arr.add(name);
            }
            return arr;
        });

        dispatcher.register("device/remove", params -> {
            cursorDevice.deleteObject();
            return new JsonPrimitive("ok");
        });

        // Per-parameter automation methods
        dispatcher.register("device/hasAutomation", params -> {
            int index = requireInt(params, "index");
            if (index < 0 || index >= PARAM_COUNT) {
                throw new IllegalArgumentException("parameter index out of range: " + index);
            }
            RemoteControl param = remoteControlsPage.getParameter(index);
            JsonObject result = new JsonObject();
            result.addProperty("hasAutomation", param.hasAutomation().get());
            return result;
        });

        dispatcher.register("device/deleteAllAutomation", params -> {
            int index = requireInt(params, "index");
            if (index < 0 || index >= PARAM_COUNT) {
                throw new IllegalArgumentException("parameter index out of range: " + index);
            }
            RemoteControl param = remoteControlsPage.getParameter(index);
            param.deleteAllAutomation();
            JsonObject result = new JsonObject();
            result.addProperty("ok", true);
            return result;
        });

        dispatcher.register("device/restoreAutomationControl", params -> {
            int index = requireInt(params, "index");
            if (index < 0 || index >= PARAM_COUNT) {
                throw new IllegalArgumentException("parameter index out of range: " + index);
            }
            RemoteControl param = remoteControlsPage.getParameter(index);
            param.restoreAutomationControl();
            JsonObject result = new JsonObject();
            result.addProperty("ok", true);
            return result;
        });

        dispatcher.register("device/touch", params -> {
            int index = requireInt(params, "index");
            boolean touched = requireBoolean(params, "touched");
            if (index < 0 || index >= PARAM_COUNT) {
                throw new IllegalArgumentException("parameter index out of range: " + index);
            }
            RemoteControl param = remoteControlsPage.getParameter(index);
            param.touch(touched);
            JsonObject result = new JsonObject();
            result.addProperty("ok", true);
            return result;
        });

        // Cursor track navigation
        dispatcher.register("cursor/selectTrack", params -> {
            String direction = requireString(params, "direction");
            switch (direction) {
                case "next":
                    cursorTrack.selectNext();
                    break;
                case "previous":
                    cursorTrack.selectPrevious();
                    break;
                default:
                    throw new IllegalArgumentException("direction must be 'next' or 'previous', got: " + direction);
            }
            return new JsonPrimitive("ok");
        });
    }

    private int requireInt(JsonObject params, String key) {
        JsonElement el = params.get(key);
        if (el == null) {
            throw new IllegalArgumentException("missing '" + key + "' parameter");
        }
        return el.getAsInt();
    }

    private double requireDouble(JsonObject params, String key) {
        JsonElement el = params.get(key);
        if (el == null) {
            throw new IllegalArgumentException("missing '" + key + "' parameter");
        }
        return el.getAsDouble();
    }

    private boolean requireBoolean(JsonObject params, String key) {
        JsonElement el = params.get(key);
        if (el == null) {
            throw new IllegalArgumentException("missing '" + key + "' parameter");
        }
        return el.getAsBoolean();
    }

    private String requireString(JsonObject params, String key) {
        JsonElement el = params.get(key);
        if (el == null) {
            throw new IllegalArgumentException("missing '" + key + "' parameter");
        }
        return el.getAsString();
    }

    private String optionalString(JsonObject params, String key, String defaultValue) {
        JsonElement el = params.get(key);
        if (el == null || el.isJsonNull()) {
            return defaultValue;
        }
        return el.getAsString();
    }

    private InsertionPoint getInsertionPoint(String position) {
        switch (position) {
            case "end":
                return cursorTrack.endOfDeviceChainInsertionPoint();
            case "before":
                return cursorDevice.beforeDeviceInsertionPoint();
            case "after":
                return cursorDevice.afterDeviceInsertionPoint();
            default:
                throw new IllegalArgumentException("position must be 'end', 'before', or 'after', got: " + position);
        }
    }
}
