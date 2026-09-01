package dev.gregross.gig.handlers;

import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.InsertionPoint;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.gregross.gig.rpc.JsonRpcDispatcher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static dev.gregross.gig.rpc.JsonParamValidator.requireArray;
import static dev.gregross.gig.rpc.JsonParamValidator.requireInt;
import static dev.gregross.gig.rpc.JsonParamValidator.requireString;
import static dev.gregross.gig.rpc.JsonParamValidator.optionalString;

/** Exact, bulk-safe operations over the project's effect tracks. */
public class EffectTrackHandler {

    private final TrackBank effectTrackBank;
    private final List<DeviceBank> deviceBanks;
    private final DeviceLibrary deviceLibrary;

    public EffectTrackHandler(
        TrackBank effectTrackBank,
        List<DeviceBank> deviceBanks,
        DeviceLibrary deviceLibrary
    ) {
        this.effectTrackBank = effectTrackBank;
        this.deviceBanks = deviceBanks;
        this.deviceLibrary = deviceLibrary;
    }

    public void register(JsonRpcDispatcher dispatcher) {
        dispatcher.register("effect/getTracks", params -> getTracks());

        dispatcher.register("effect/copyDeviceToTracks", params -> {
            EffectTrack sourceTrack = resolveTrack(requireObject(params, "sourceTrack"));
            JsonObject sourceDeviceRef = requireObject(params, "sourceDevice");
            int sourcePosition = requireInt(sourceDeviceRef, "position");
            String expectedDeviceName = requireString(sourceDeviceRef, "expectedName");
            Device sourceDevice = resolveDevice(sourceTrack, sourcePosition, expectedDeviceName);

            JsonArray destinationRefs = requireArray(params, "destinationTracks");
            if (destinationRefs.isEmpty()) {
                throw new IllegalArgumentException("destinationTracks array must not be empty");
            }
            if (destinationRefs.size() > effectTrackBank.getSizeOfBank() - 1) {
                throw new IllegalArgumentException("too many destination tracks");
            }

            // Resolve every target and inspect its device chain before copying anything.
            List<EffectTrack> toCopy = new ArrayList<>();
            JsonArray alreadyPresent = new JsonArray();
            Set<String> destinationIds = new HashSet<>();
            for (JsonElement element : destinationRefs) {
                if (!element.isJsonObject()) {
                    throw new IllegalArgumentException("each destination track reference must be an object");
                }
                EffectTrack destination = resolveTrack(element.getAsJsonObject());
                if (destination.channelId().equals(sourceTrack.channelId())) {
                    throw new IllegalArgumentException("source track cannot also be a destination");
                }
                if (!destinationIds.add(destination.channelId())) {
                    throw new IllegalArgumentException("duplicate destination track: " + destination.name());
                }

                int matches = countDevicesNamed(destination.deviceBank(), expectedDeviceName);
                if (matches > 1) {
                    throw new IllegalStateException("ambiguous existing device '" + expectedDeviceName
                        + "' on effect track '" + destination.name() + "'");
                }
                if (matches == 1) {
                    alreadyPresent.add(trackState(destination));
                } else {
                    if (destination.deviceBank().itemCount().get() >= destination.deviceBank().getSizeOfBank()) {
                        throw new IllegalStateException("device observation capacity reached on effect track '"
                            + destination.name() + "'");
                    }
                    toCopy.add(destination);
                }
            }

            for (EffectTrack destination : toCopy) {
                destination.track().endOfDeviceChainInsertionPoint().copyDevices(sourceDevice);
            }

            JsonArray copied = new JsonArray();
            for (EffectTrack destination : toCopy) {
                copied.add(trackState(destination));
            }

            JsonObject result = new JsonObject();
            result.addProperty("ok", true);
            result.add("sourceTrack", trackState(sourceTrack));
            result.addProperty("sourceDeviceName", expectedDeviceName);
            result.addProperty("sourceDevicePosition", sourcePosition);
            result.addProperty("destinationCount", destinationRefs.size());
            result.addProperty("copiedCount", copied.size());
            result.addProperty("alreadyPresentCount", alreadyPresent.size());
            result.add("copied", copied);
            result.add("alreadyPresent", alreadyPresent);
            return result;
        });

        dispatcher.register("effect/insertDeviceOnTracks", params -> {
            DeviceInsertion device = resolveDeviceInsertion(requireObject(params, "device"));
            String position = optionalString(params, "position", "end").toLowerCase();
            if (!position.equals("start") && !position.equals("end")) {
                throw new IllegalArgumentException("position must be 'start' or 'end'");
            }

            JsonArray destinationRefs = requireArray(params, "destinationTracks");
            if (destinationRefs.isEmpty()) {
                throw new IllegalArgumentException("destinationTracks array must not be empty");
            }
            if (destinationRefs.size() > effectTrackBank.getSizeOfBank()) {
                throw new IllegalArgumentException("too many destination tracks");
            }

            // Validate the complete destination set and idempotence checks before inserting anything.
            List<EffectTrack> toInsert = new ArrayList<>();
            JsonArray alreadyPresent = new JsonArray();
            Set<String> destinationIds = new HashSet<>();
            for (JsonElement element : destinationRefs) {
                if (!element.isJsonObject()) {
                    throw new IllegalArgumentException("each destination track reference must be an object");
                }
                EffectTrack destination = resolveTrack(element.getAsJsonObject());
                if (!destinationIds.add(destination.channelId())) {
                    throw new IllegalArgumentException("duplicate destination track: " + destination.name());
                }

                int matches = countDevicesNamed(destination.deviceBank(), device.expectedName());
                if (matches > 1) {
                    throw new IllegalStateException("ambiguous existing device '" + device.expectedName()
                        + "' on effect track '" + destination.name() + "'");
                }
                if (matches == 1) {
                    alreadyPresent.add(trackState(destination));
                } else {
                    if (destination.deviceBank().itemCount().get() >= destination.deviceBank().getSizeOfBank()) {
                        throw new IllegalStateException("device observation capacity reached on effect track '"
                            + destination.name() + "'");
                    }
                    toInsert.add(destination);
                }
            }

            for (EffectTrack destination : toInsert) {
                insertDevice(destination, device, position);
            }

            JsonArray inserted = new JsonArray();
            for (EffectTrack destination : toInsert) {
                inserted.add(trackState(destination));
            }

            JsonObject result = new JsonObject();
            result.addProperty("ok", true);
            result.addProperty("deviceKind", device.kind());
            result.addProperty("expectedDeviceName", device.expectedName());
            result.addProperty("position", position);
            result.addProperty("destinationCount", destinationRefs.size());
            result.addProperty("insertedCount", inserted.size());
            result.addProperty("alreadyPresentCount", alreadyPresent.size());
            result.add("inserted", inserted);
            result.add("alreadyPresent", alreadyPresent);
            return result;
        });
    }

    private JsonObject getTracks() {
        JsonArray tracks = new JsonArray();
        for (int i = 0; i < effectTrackBank.getSizeOfBank(); i++) {
            Track track = (Track) effectTrackBank.getItemAt(i);
            if (!track.exists().get()) {
                continue;
            }
            String name = track.name().get();
            String channelId = track.channelId().get();
            if (name == null || name.isEmpty() || channelId == null || channelId.isEmpty()) {
                continue;
            }
            EffectTrack resolved = new EffectTrack(i, channelId, name, track, deviceBanks.get(i));
            JsonObject state = trackState(resolved);
            JsonArray devices = new JsonArray();
            DeviceBank bank = resolved.deviceBank();
            for (int deviceIndex = 0; deviceIndex < bank.getSizeOfBank(); deviceIndex++) {
                Device device = (Device) bank.getItemAt(deviceIndex);
                if (!device.exists().get()) {
                    continue;
                }
                String deviceName = device.name().get();
                if (deviceName != null && !deviceName.isEmpty()) {
                    JsonObject deviceState = new JsonObject();
                    deviceState.addProperty("position", deviceIndex);
                    deviceState.addProperty("name", deviceName);
                    devices.add(deviceState);
                }
            }
            state.add("devices", devices);
            tracks.add(state);
        }

        JsonObject result = new JsonObject();
        result.addProperty("itemCount", effectTrackBank.itemCount().get());
        result.addProperty("observedCapacity", effectTrackBank.getSizeOfBank());
        result.add("tracks", tracks);
        return result;
    }

    private EffectTrack resolveTrack(JsonObject ref) {
        String channelId = requireString(ref, "channelId");
        String expectedName = requireString(ref, "expectedName");
        for (int i = 0; i < effectTrackBank.getSizeOfBank(); i++) {
            Track track = (Track) effectTrackBank.getItemAt(i);
            if (!track.exists().get() || !channelId.equals(track.channelId().get())) {
                continue;
            }
            String actualName = track.name().get();
            if (!expectedName.equals(actualName)) {
                throw new IllegalStateException("effect track identity mismatch for " + channelId
                    + ": expected '" + expectedName + "', found '" + actualName + "'");
            }
            return new EffectTrack(i, channelId, actualName, track, deviceBanks.get(i));
        }
        throw new IllegalArgumentException("effect track not found: " + channelId);
    }

    private Device resolveDevice(EffectTrack sourceTrack, int position, String expectedName) {
        DeviceBank bank = sourceTrack.deviceBank();
        if (position < 0 || position >= bank.getSizeOfBank()) {
            throw new IllegalArgumentException("source device position out of range: " + position);
        }
        Device device = (Device) bank.getItemAt(position);
        if (!device.exists().get()) {
            throw new IllegalArgumentException("source device does not exist at position " + position);
        }
        String actualName = device.name().get();
        if (!expectedName.equals(actualName)) {
            throw new IllegalStateException("source device identity mismatch at position " + position
                + ": expected '" + expectedName + "', found '" + actualName + "'");
        }
        return device;
    }

    private int countDevicesNamed(DeviceBank bank, String expectedName) {
        int matches = 0;
        for (int i = 0; i < bank.getSizeOfBank(); i++) {
            Device device = (Device) bank.getItemAt(i);
            if (device.exists().get() && expectedName.equals(device.name().get())) {
                matches++;
            }
        }
        return matches;
    }

    private DeviceInsertion resolveDeviceInsertion(JsonObject device) {
        String kind = requireString(device, "kind");
        String expectedName = requireString(device, "expectedName");
        if (kind.equals("bitwig_library")) {
            String libraryName = requireString(device, "libraryName");
            String libraryPath = deviceLibrary.resolve(libraryName).toString();
            return new DeviceInsertion(kind, expectedName, libraryPath, null, null);
        }
        if (kind.equals("plugin")) {
            String type = requireString(device, "type").toLowerCase();
            if (!type.equals("clap") && !type.equals("vst3") && !type.equals("vst2")) {
                throw new IllegalArgumentException("plugin type must be 'clap', 'vst3', or 'vst2'");
            }
            String id = requireString(device, "id");
            if (type.equals("vst2")) {
                try {
                    Integer.parseInt(id);
                } catch (NumberFormatException error) {
                    throw new IllegalArgumentException("VST2 id must be an integer: " + id);
                }
            }
            return new DeviceInsertion(kind, expectedName, null, type, id);
        }
        throw new IllegalArgumentException("device kind must be 'bitwig_library' or 'plugin'");
    }

    private void insertDevice(EffectTrack destination, DeviceInsertion device, String position) {
        InsertionPoint insertionPoint = position.equals("start")
            ? destination.track().startOfDeviceChainInsertionPoint()
            : destination.track().endOfDeviceChainInsertionPoint();
        if (device.kind().equals("bitwig_library")) {
            insertionPoint.insertFile(device.libraryPath());
            return;
        }
        switch (device.pluginType()) {
            case "clap" -> insertionPoint.insertCLAPDevice(device.pluginId());
            case "vst3" -> insertionPoint.insertVST3Device(device.pluginId());
            case "vst2" -> insertionPoint.insertVST2Device(Integer.parseInt(device.pluginId()));
            default -> throw new IllegalStateException("unsupported plugin type: " + device.pluginType());
        }
    }

    private JsonObject trackState(EffectTrack track) {
        JsonObject state = new JsonObject();
        state.addProperty("index", track.index());
        state.addProperty("channelId", track.channelId());
        state.addProperty("name", track.name());
        return state;
    }

    private JsonObject requireObject(JsonObject params, String key) {
        JsonElement element = params.get(key);
        if (element == null || !element.isJsonObject()) {
            throw new IllegalArgumentException("missing '" + key + "' object");
        }
        return element.getAsJsonObject();
    }

    private record EffectTrack(
        int index,
        String channelId,
        String name,
        Track track,
        DeviceBank deviceBank
    ) {}

    private record DeviceInsertion(
        String kind,
        String expectedName,
        String libraryPath,
        String pluginType,
        String pluginId
    ) {}
}
