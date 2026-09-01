package dev.gregross.gig.handlers;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.InsertionPoint;
import com.bitwig.extension.controller.api.IntegerValue;
import com.bitwig.extension.controller.api.SettableStringValue;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import dev.gregross.gig.rpc.JsonRpcDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EffectTrackHandlerTest {

    @Mock private TrackBank effectTrackBank;
    @Mock private Track sourceTrack;
    @Mock private Track destinationTrack;
    @Mock private DeviceBank sourceDeviceBank;
    @Mock private DeviceBank destinationDeviceBank;
    @Mock private Device sourceDevice;
    @Mock private Device destinationPlaceholder;
    @Mock private InsertionPoint destinationInsertionPoint;
    @Mock private DeviceLibrary deviceLibrary;

    private JsonRpcDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new JsonRpcDispatcher();
        new EffectTrackHandler(effectTrackBank, List.of(sourceDeviceBank, destinationDeviceBank), deviceLibrary)
            .register(dispatcher);

        when(effectTrackBank.getSizeOfBank()).thenReturn(2);
        when(effectTrackBank.getItemAt(0)).thenReturn(sourceTrack);
        when(effectTrackBank.getItemAt(1)).thenReturn(destinationTrack);
        stubTrack(sourceTrack, "source-id", "Kick");
        stubTrack(destinationTrack, "destination-id", "Clap");

        when(sourceDeviceBank.getSizeOfBank()).thenReturn(1);
        when(sourceDeviceBank.getItemAt(0)).thenReturn(sourceDevice);
        stubDevice(sourceDevice, true, "ROOMORBIT");

        when(destinationDeviceBank.getSizeOfBank()).thenReturn(1);
        when(destinationDeviceBank.getItemAt(0)).thenReturn(destinationPlaceholder);
        stubDevice(destinationPlaceholder, false, "");
        IntegerValue destinationDeviceCount = mock(IntegerValue.class);
        when(destinationDeviceCount.get()).thenReturn(0);
        when(destinationDeviceBank.itemCount()).thenReturn(destinationDeviceCount);
        when(destinationTrack.endOfDeviceChainInsertionPoint()).thenReturn(destinationInsertionPoint);
    }

    @Test
    void registersEffectTrackMethods() {
        assertTrue(dispatcher.getRegisteredMethods().contains("effect/getTracks"));
        assertTrue(dispatcher.getRegisteredMethods().contains("effect/copyDeviceToTracks"));
        assertTrue(dispatcher.getRegisteredMethods().contains("effect/insertDeviceOnTracks"));
        assertTrue(dispatcher.getRegisteredMethods().contains("effect/renameTracks"));
    }

    @Test
    void copiesExactSourceDeviceToValidatedDestination() {
        String response = dispatcher.handle(rpc("effect/copyDeviceToTracks",
            "{\"sourceTrack\":{\"channelId\":\"source-id\",\"expectedName\":\"Kick\"},"
                + "\"sourceDevice\":{\"position\":0,\"expectedName\":\"ROOMORBIT\"},"
                + "\"destinationTracks\":[{\"channelId\":\"destination-id\","
                + "\"expectedName\":\"Clap\"}]}"));

        assertContains(response, "\"ok\":true");
        assertContains(response, "\"copiedCount\":1");
        verify(destinationInsertionPoint).copyDevices(sourceDevice);
    }

    @Test
    void insertsPluginOnceOnValidatedDestinations() {
        String response = dispatcher.handle(rpc("effect/insertDeviceOnTracks",
            "{\"device\":{\"kind\":\"plugin\",\"type\":\"vst3\","
                + "\"id\":\"com.example.room\",\"expectedName\":\"RoomOrbit\"},"
                + "\"position\":\"end\",\"destinationTracks\":[{"
                + "\"channelId\":\"destination-id\",\"expectedName\":\"Clap\"}]}"));

        assertContains(response, "\"ok\":true");
        assertContains(response, "\"insertedCount\":1");
        verify(destinationInsertionPoint).insertVST3Device("com.example.room");
    }

    @Test
    void renamesValidatedTracksInOneRequest() {
        String response = dispatcher.handle(rpc("effect/renameTracks",
            "{\"renames\":[{\"channelId\":\"source-id\",\"expectedName\":\"Kick\","
                + "\"newName\":\"Kick_FX\"},{\"channelId\":\"destination-id\","
                + "\"expectedName\":\"Clap\",\"newName\":\"Clap_FX\"}]}"));

        assertContains(response, "\"ok\":true");
        assertContains(response, "\"renameCount\":2");
        verify(sourceTrack.name()).set("Kick_FX");
        verify(destinationTrack.name()).set("Clap_FX");
    }

    private void stubTrack(Track track, String id, String name) {
        BooleanValue exists = mock(BooleanValue.class);
        StringValue channelId = mock(StringValue.class);
        SettableStringValue trackName = mock(SettableStringValue.class);
        when(exists.get()).thenReturn(true);
        when(channelId.get()).thenReturn(id);
        when(trackName.get()).thenReturn(name);
        when(track.exists()).thenReturn(exists);
        when(track.channelId()).thenReturn(channelId);
        when(track.name()).thenReturn(trackName);
    }

    private void stubDevice(Device device, boolean existsValue, String name) {
        BooleanValue exists = mock(BooleanValue.class);
        SettableStringValue deviceName = mock(SettableStringValue.class);
        when(exists.get()).thenReturn(existsValue);
        when(deviceName.get()).thenReturn(name);
        when(device.exists()).thenReturn(exists);
        when(device.name()).thenReturn(deviceName);
    }

    private String rpc(String method, String params) {
        return "{\"jsonrpc\":\"2.0\",\"method\":\"" + method
            + "\",\"params\":" + params + ",\"id\":1}";
    }

    private void assertContains(String actual, String expected) {
        assertTrue(actual.contains(expected), "Expected '" + expected + "' in: " + actual);
    }
}
