package dev.gregross.gig.handlers;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.InsertionPoint;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.Transport;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.gregross.gig.rpc.JsonRpcDispatcher;

import static dev.gregross.gig.rpc.JsonParamValidator.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class DeviceHandler {

    private static final int PARAM_COUNT = 8;
    static final Set<String> VALID_PAGE_TAGS = Set.of(
        "env", "eq", "filter", "fx", "lfo", "mixer", "osc", "perf"
    );

    private final CursorTrack cursorTrack;
    private final CursorDevice cursorDevice;
    private final CursorRemoteControlsPage remoteControlsPage;
    private final DrumPadBank drumPadBank;
    private final DeviceLibrary deviceLibrary;
    private final Transport transport;
    private final ControllerHost host;

    public DeviceHandler(CursorTrack cursorTrack, CursorDevice cursorDevice,
                         CursorRemoteControlsPage remoteControlsPage,
                         DrumPadBank drumPadBank,
                         DeviceLibrary deviceLibrary, Transport transport,
                         ControllerHost host) {
        this.cursorTrack = cursorTrack;
        this.cursorDevice = cursorDevice;
        this.remoteControlsPage = remoteControlsPage;
        this.drumPadBank = drumPadBank;
        this.deviceLibrary = deviceLibrary;
        this.transport = transport;
        this.host = host;
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

        // Envelope writing
        dispatcher.register("device/writeEnvelope", params -> {
            int index = requireInt(params, "index");
            if (index < 0 || index >= PARAM_COUNT) {
                throw new IllegalArgumentException("parameter index out of range: " + index);
            }

            // Precondition: arranger automation write must be enabled
            if (!transport.isArrangerAutomationWriteEnabled().get()) {
                throw new IllegalStateException("Arranger automation write must be enabled");
            }

            // Parse and validate points
            JsonArray pointsArr = requireArray(params, "points");
            if (pointsArr.isEmpty()) {
                throw new IllegalArgumentException("points array must not be empty");
            }

            List<double[]> points = new ArrayList<>();
            for (JsonElement el : pointsArr) {
                JsonObject pt = el.getAsJsonObject();
                if (!pt.has("position") || !pt.has("value")) {
                    throw new IllegalArgumentException("each point must have 'position' and 'value'");
                }
                double position = pt.get("position").getAsDouble();
                double value = pt.get("value").getAsDouble();
                if (position < 0) {
                    throw new IllegalArgumentException("position must be >= 0, got: " + position);
                }
                // Clamp value to [0, 1]
                value = Math.max(0.0, Math.min(1.0, value));
                points.add(new double[]{position, value});
            }

            // Sort by position ascending
            points.sort(Comparator.comparingDouble(a -> a[0]));

            // Deduplicate: last-wins for same position
            List<double[]> deduped = new ArrayList<>();
            for (int i = 0; i < points.size(); i++) {
                if (i == points.size() - 1 || points.get(i)[0] != points.get(i + 1)[0]) {
                    deduped.add(points.get(i));
                }
            }

            int pointCount = deduped.size();
            RemoteControl param = remoteControlsPage.getParameter(index);

            // Save state
            double savedPosition = transport.getPosition().get();
            boolean wasPlaying = transport.isPlaying().get();

            // Stop if playing, then start fresh playback
            // (Bitwig requires active playback for touch automation recording — D-9.2a)
            if (wasPlaying) {
                transport.stop();
            }
            transport.play();

            // Schedule point-writing as chained tasks across flush cycles.
            // Each point needs its own flush cycle for the engine to process
            // the position jump before recording the touch + value.
            long delay = 100; // initial delay for playback to engage
            for (int i = 0; i < deduped.size(); i++) {
                final double[] pt = deduped.get(i);
                final boolean isLast = (i == deduped.size() - 1);
                host.scheduleTask(() -> {
                    transport.getPosition().set(pt[0]);
                    param.touch(true);
                    param.value().setImmediately(pt[1]);
                    param.touch(false);

                    if (isLast) {
                        // Final point: schedule cleanup
                        host.scheduleTask(() -> {
                            param.touch(false); // ensure untouched
                            transport.stop();
                            transport.getPosition().set(savedPosition);
                            if (wasPlaying) {
                                transport.play();
                            }
                        }, 50);
                    }
                }, delay);
                delay += 100; // 100ms between points
            }

            JsonObject result = new JsonObject();
            result.addProperty("ok", true);
            result.addProperty("pointsWritten", pointCount);
            return result;
        });

        // Drum pad inspection
        dispatcher.register("device/getDrumPads", params -> {
            if (!cursorDevice.hasDrumPads().get()) {
                throw new IllegalStateException("Current device has no drum pads");
            }
            JsonArray pads = new JsonArray();
            for (int i = 0; i < 128; i++) {
                com.bitwig.extension.controller.api.DrumPad pad =
                    (com.bitwig.extension.controller.api.DrumPad) drumPadBank.getItemAt(i);
                if (pad.exists().get()) {
                    String name = pad.name().get();
                    if (name != null && !name.isEmpty()) {
                        JsonObject padObj = new JsonObject();
                        padObj.addProperty("note", i);
                        padObj.addProperty("name", name);
                        pads.add(padObj);
                    }
                }
            }
            return pads;
        });

        // Nested device chain navigation
        dispatcher.register("device/enterSlot", params -> {
            String name = requireString(params, "name");
            cursorDevice.selectFirstInSlot(name);
            return new JsonPrimitive("ok");
        });

        dispatcher.register("device/exitToParent", params -> {
            cursorDevice.selectParent();
            return new JsonPrimitive("ok");
        });

        // Layer and drum pad navigation
        dispatcher.register("device/enterLayer", params -> {
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

        dispatcher.register("device/enterKeyPad", params -> {
            int key = requireInt(params, "key");
            if (key < 0 || key > 127) {
                throw new IllegalArgumentException("key must be 0-127, got " + key);
            }
            cursorDevice.selectFirstInKeyPad(key);
            return new JsonPrimitive("ok");
        });

        // Parameter page tag filtering
        dispatcher.register("device/selectPageByTag", params -> {
            String tag = requireString(params, "tag").toLowerCase();
            validatePageTag(tag);
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

    private JsonArray requireArray(JsonObject params, String key) {
        JsonElement el = params.get(key);
        if (el == null) {
            throw new IllegalArgumentException("missing '" + key + "' parameter");
        }
        if (!el.isJsonArray()) {
            throw new IllegalArgumentException("'" + key + "' must be an array");
        }
        return el.getAsJsonArray();
    }

    static void validatePageTag(String tag) {
        if (!VALID_PAGE_TAGS.contains(tag)) {
            throw new IllegalArgumentException(
                "Invalid page tag '" + tag + "'. Valid tags: " + VALID_PAGE_TAGS);
        }
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
