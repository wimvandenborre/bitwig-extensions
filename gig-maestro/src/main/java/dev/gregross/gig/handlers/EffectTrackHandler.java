package dev.gregross.gig.handlers;

import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
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

/** Exact, bulk-safe operations over the project's effect tracks. */
public class EffectTrackHandler {

    private final TrackBank effectTrackBank;
    private final List<DeviceBank> deviceBanks;

    public EffectTrackHandler(TrackBank effectTrackBank, List<DeviceBank> deviceBanks) {
        this.effectTrackBank = effectTrackBank;
        this.deviceBanks = deviceBanks;
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
}
