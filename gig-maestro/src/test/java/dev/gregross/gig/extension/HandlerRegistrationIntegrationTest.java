package dev.gregross.gig.extension;

import com.bitwig.extension.controller.api.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import dev.gregross.gig.handlers.*;
import dev.gregross.gig.rpc.JsonRpcDispatcher;
import dev.gregross.gig.rpc.TaskScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HandlerRegistrationIntegrationTest {

    // Bitwig API mocks — one per unique type needed by handler constructors
    @Mock private Application mockApplication;
    @Mock private ControllerHost mockHost;
    @Mock private Transport mockTransport;
    @Mock private TrackBank mockTrackBank;
    @Mock private SceneBank mockSceneBank;
    @Mock private CursorTrack mockCursorTrack;
    @Mock private CursorDevice mockCursorDevice;
    @Mock private CursorDevice mockMasterCursorDevice;
    @Mock private CursorRemoteControlsPage mockRemoteControlsPage;
    @Mock private CursorRemoteControlsPage mockMasterRemoteControlsPage;
    @Mock private DrumPadBank mockDrumPadBank;
    @Mock private MasterTrack mockMasterTrack;
    @Mock private Clip mockCursorClip;
    @Mock private Project mockProject;
    @Mock private NoteInput mockNoteInput;
    @Mock private Arpeggiator mockArpeggiator;
    @Mock private NoteLatch mockNoteLatch;
    @Mock private PopupBrowser mockPopupBrowser;
    @Mock private Arranger mockArranger;
    @Mock private CueMarkerBank mockCueMarkerBank;
    @Mock private SettableBooleanValue mockIsPinned;
    @Mock private StringValue mockCursorTrackType;

    @TempDir
    Path tempDir;

    private JsonRpcDispatcher dispatcher;
    private static final TaskScheduler IMMEDIATE_SCHEDULER = (task, delayMs) -> task.run();

    @BeforeEach
    void setUp() throws IOException {
        // TrackBank.sceneBank() needed by ClipHandler and SceneHandler
        when(mockTrackBank.sceneBank()).thenReturn(mockSceneBank);

        // CursorTrack properties needed by TrackHandler constructor markInterested() calls
        when(mockCursorTrack.isPinned()).thenReturn(mockIsPinned);
        when(mockCursorTrack.trackType()).thenReturn(mockCursorTrackType);

        dispatcher = new JsonRpcDispatcher();

        // Register built-in methods (mirrors GigMaestroExtension.init)
        StateCache stateCache = new StateCache();
        dispatcher.register("session/snapshot", params -> stateCache.getSnapshot());
        dispatcher.register("api/list", params -> {
            JsonArray methods = new JsonArray();
            for (String method : dispatcher.getRegisteredMethods()) {
                methods.add(new JsonPrimitive(method));
            }
            return methods;
        });

        // Register all 16 handlers in the same order as GigMaestroExtension.init()
        DeviceLibrary deviceLibrary = new DeviceLibrary(tempDir);

        new ApplicationHandler(mockApplication, mockHost).register(dispatcher);
        new TransportHandler(mockTransport, stateCache).register(dispatcher);
        TrackBankManager trackBankManager = new TrackBankManager(mockTrackBank, 8);
        new TrackHandler(mockTrackBank, mockApplication, mockCursorTrack, trackBankManager, stateCache, mockNoteInput).register(dispatcher);
        new MasterHandler(mockMasterTrack).register(dispatcher);
        new ClipHandler(mockTrackBank, mockSceneBank, mockCursorClip, stateCache).register(dispatcher);
        new DeviceHandler(mockCursorTrack, mockCursorDevice, mockRemoteControlsPage, mockDrumPadBank, deviceLibrary, mockTransport, mockHost, (task, delay) -> task.run()).register(dispatcher);
        new NoteHandler(mockCursorClip, stateCache).register(dispatcher);
        new SceneHandler(mockSceneBank, mockProject, stateCache).register(dispatcher);
        new ArrangerHandler(mockArranger, mockTransport, mockCueMarkerBank, stateCache).register(dispatcher);
        new MasterDeviceHandler(mockMasterTrack, mockMasterCursorDevice, mockMasterRemoteControlsPage, deviceLibrary, (task, delay) -> task.run()).register(dispatcher);
        new SendHandler(mockTrackBank, 4).register(dispatcher);
        new ProjectHandler(mockProject, stateCache).register(dispatcher);
        new TransactionHandler(dispatcher, stateCache).register(dispatcher);
        new BrowserHandler(mockPopupBrowser, mockCursorDevice, stateCache).register(dispatcher);
        new NoteInputHandler(mockNoteInput, mockArpeggiator, mockNoteLatch, stateCache).register(dispatcher);
        new MacroHandler(dispatcher, stateCache, IMMEDIATE_SCHEDULER).register(dispatcher);
    }

    // --- Wiring verification ---

    @Test
    void allHandlersRegisterSuccessfully() {
        Set<String> methods = dispatcher.getRegisteredMethods();
        // 16 handlers + 2 built-in (session/snapshot, api/list)
        // Exact count verified by running the test
        assertTrue(methods.size() > 200, "Expected 200+ methods, got " + methods.size());
    }

    @Test
    void apiListReturnsAllMethods() {
        String response = rpc("api/list", "{}");
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        assertTrue(json.has("result"));
        JsonArray methods = json.getAsJsonArray("result");
        assertEquals(dispatcher.getRegisteredMethods().size(), methods.size());
    }

    // --- Namespace presence ---

    @Test
    void applicationNamespaceRegistered() {
        assertNamespacePresent("app/");
    }

    @Test
    void transportNamespaceRegistered() {
        assertNamespacePresent("transport/");
    }

    @Test
    void trackNamespaceRegistered() {
        assertNamespacePresent("track/");
    }

    @Test
    void clipNamespaceRegistered() {
        assertNamespacePresent("clip/");
    }

    @Test
    void deviceNamespaceRegistered() {
        assertNamespacePresent("device/");
    }

    @Test
    void masterDeviceNamespaceRegistered() {
        assertNamespacePresent("masterDevice/");
    }

    @Test
    void macroNamespaceRegistered() {
        assertNamespacePresent("macro/");
    }

    @Test
    void sessionNamespaceRegistered() {
        assertNamespacePresent("session/");
    }

    // --- End-to-end pipeline ---

    @Test
    void sampleRpcCallReturnsValidJsonRpcResponse() {
        String response = rpc("api/list", "{}");
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        assertEquals("2.0", json.get("jsonrpc").getAsString());
        assertTrue(json.has("result"));
        assertEquals(1, json.get("id").getAsInt());
    }

    @Test
    void unknownMethodReturnsError() {
        String response = rpc("nonexistent/method", "{}");
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        assertTrue(json.has("error"));
        assertEquals(-32601, json.getAsJsonObject("error").get("code").getAsInt());
    }

    // --- Helpers ---

    private String rpc(String method, String params) {
        return dispatcher.handle(
            "{\"jsonrpc\":\"2.0\",\"method\":\"" + method + "\",\"params\":" + params + ",\"id\":1}");
    }

    private void assertNamespacePresent(String prefix) {
        boolean found = dispatcher.getRegisteredMethods().stream()
            .anyMatch(m -> m.startsWith(prefix));
        assertTrue(found, "No methods found with prefix '" + prefix + "'");
    }
}
