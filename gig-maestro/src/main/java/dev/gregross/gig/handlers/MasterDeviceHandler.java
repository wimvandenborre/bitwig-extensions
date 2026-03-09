package dev.gregross.gig.handlers;

import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.InsertionPoint;
import com.bitwig.extension.controller.api.MasterTrack;
import com.bitwig.extension.controller.api.RemoteControl;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.gregross.gig.rpc.JsonRpcDispatcher;

import static dev.gregross.gig.rpc.JsonParamValidator.*;

import java.nio.file.Path;

public class MasterDeviceHandler {

    private static final int PARAM_COUNT = 8;

    private final MasterTrack masterTrack;
    private final CursorDevice cursorDevice;
    private final CursorRemoteControlsPage remoteControlsPage;
    private final DeviceLibrary deviceLibrary;

    public MasterDeviceHandler(MasterTrack masterTrack, CursorDevice cursorDevice,
                                CursorRemoteControlsPage remoteControlsPage,
                                DeviceLibrary deviceLibrary) {
        this.masterTrack = masterTrack;
        this.cursorDevice = cursorDevice;
        this.remoteControlsPage = remoteControlsPage;
        this.deviceLibrary = deviceLibrary;
    }

    public void register(JsonRpcDispatcher dispatcher) {
        // Device chain navigation
        dispatcher.register("masterDevice/selectNext", params -> {
            cursorDevice.selectNext();
            return new JsonPrimitive("ok");
        });

        dispatcher.register("masterDevice/selectPrevious", params -> {
            cursorDevice.selectPrevious();
            return new JsonPrimitive("ok");
        });

        // Device state
        dispatcher.register("masterDevice/setEnabled", params -> {
            boolean enabled = requireBoolean(params, "enabled");
            cursorDevice.isEnabled().set(enabled);
            return new JsonPrimitive("ok");
        });

        // Device insertion
        dispatcher.register("masterDevice/insertBitwigDevice", params -> {
            String name = requireString(params, "name");
            String position = optionalString(params, "position", "end");
            Path devicePath = deviceLibrary.resolve(name);
            getInsertionPoint(position).insertFile(devicePath.toString());
            return new JsonPrimitive("ok");
        });

        dispatcher.register("masterDevice/insertPluginDevice", params -> {
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

        // Device removal
        dispatcher.register("masterDevice/remove", params -> {
            cursorDevice.deleteObject();
            return new JsonPrimitive("ok");
        });

        // Parameter page navigation
        dispatcher.register("masterDevice/selectPage", params -> {
            int index = requireInt(params, "index");
            remoteControlsPage.selectedPageIndex().set(index);
            return new JsonPrimitive("ok");
        });

        dispatcher.register("masterDevice/nextPage", params -> {
            remoteControlsPage.selectNextPage(false);
            return new JsonPrimitive("ok");
        });

        dispatcher.register("masterDevice/previousPage", params -> {
            remoteControlsPage.selectPreviousPage(false);
            return new JsonPrimitive("ok");
        });

        // Nested device chain navigation
        dispatcher.register("masterDevice/enterSlot", params -> {
            String name = requireString(params, "name");
            cursorDevice.selectFirstInSlot(name);
            return new JsonPrimitive("ok");
        });

        dispatcher.register("masterDevice/exitToParent", params -> {
            cursorDevice.selectParent();
            return new JsonPrimitive("ok");
        });

        // Layer and drum pad navigation
        dispatcher.register("masterDevice/enterLayer", params -> {
            boolean hasIndex = params.has("index") && !params.get("index").isJsonNull();
            boolean hasName = params.has("name") && !params.get("name").isJsonNull();
            if (!hasIndex && !hasName) {
                throw new IllegalArgumentException("must provide 'index' or 'name' parameter");
            }
            if (hasIndex && hasName) {
                throw new IllegalArgumentException("'index' and 'name' are mutually exclusive — provide one, not both");
            }
            if (hasIndex) {
                cursorDevice.selectFirstInLayer(params.get("index").getAsInt());
            } else {
                cursorDevice.selectFirstInLayer(params.get("name").getAsString());
            }
            return new JsonPrimitive("ok");
        });

        dispatcher.register("masterDevice/enterKeyPad", params -> {
            int key = requireInt(params, "key");
            if (key < 0 || key > 127) {
                throw new IllegalArgumentException("key must be 0-127, got " + key);
            }
            cursorDevice.selectFirstInKeyPad(key);
            return new JsonPrimitive("ok");
        });

        // Parameter page tag filtering
        dispatcher.register("masterDevice/selectPageByTag", params -> {
            String tag = requireString(params, "tag").toLowerCase();
            DeviceHandler.validatePageTag(tag);
            String direction = optionalString(params, "direction", "next");
            boolean cycle = params.has("cycle") ? params.get("cycle").getAsBoolean() : true;
            if ("next".equals(direction)) {
                remoteControlsPage.selectNextPageMatching(tag, cycle);
            } else if ("previous".equals(direction)) {
                remoteControlsPage.selectPreviousPageMatching(tag, cycle);
            } else {
                throw new IllegalArgumentException("direction must be 'next' or 'previous', got: " + direction);
            }
            return new JsonPrimitive("ok");
        });

        // Parameter mutation
        dispatcher.register("masterDevice/setParameterValue", params -> {
            int index = requireInt(params, "index");
            double value = requireDouble(params, "value");
            if (index < 0 || index >= PARAM_COUNT) {
                throw new IllegalArgumentException("parameter index out of range: " + index);
            }
            RemoteControl param = remoteControlsPage.getParameter(index);
            param.value().setImmediately(value);
            return new JsonPrimitive("ok");
        });
    }

    private InsertionPoint getInsertionPoint(String position) {
        switch (position) {
            case "end":
                return masterTrack.endOfDeviceChainInsertionPoint();
            case "before":
                return cursorDevice.beforeDeviceInsertionPoint();
            case "after":
                return cursorDevice.afterDeviceInsertionPoint();
            default:
                throw new IllegalArgumentException("position must be 'end', 'before', or 'after', got: " + position);
        }
    }

}
