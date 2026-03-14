package dev.gregross.gig.extension;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.gregross.gig.handlers.ApplicationHandler;
import dev.gregross.gig.handlers.ArrangerHandler;
import dev.gregross.gig.handlers.BrowserHandler;
import dev.gregross.gig.handlers.ClipHandler;
import dev.gregross.gig.handlers.DeviceHandler;
import dev.gregross.gig.handlers.DeviceLibrary;
import dev.gregross.gig.handlers.GrooveHandler;
import dev.gregross.gig.handlers.MacroHandler;
import dev.gregross.gig.handlers.MasterDeviceHandler;
import dev.gregross.gig.handlers.MasterHandler;
import dev.gregross.gig.handlers.NoteHandler;
import dev.gregross.gig.handlers.NoteInputHandler;
import dev.gregross.gig.handlers.ProjectHandler;
import dev.gregross.gig.handlers.SceneHandler;
import dev.gregross.gig.handlers.SendHandler;
import dev.gregross.gig.handlers.TrackBankManager;
import dev.gregross.gig.handlers.TrackHandler;
import dev.gregross.gig.handlers.TransactionHandler;
import dev.gregross.gig.handlers.TransportHandler;
import dev.gregross.gig.rpc.CommandQueue;
import dev.gregross.gig.rpc.JsonRpcDispatcher;
import dev.gregross.gig.server.ServerManager;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

public class GigMaestroExtension extends ControllerExtension {

    private static final int DEFAULT_PORT = 8787;
    private static final int TRACK_COUNT = 8;
    private static final int SEND_COUNT = 4;
    private static final int SCENE_COUNT = 5;
    private static final int CLIP_GRID_WIDTH = 256;
    private static final int CLIP_GRID_HEIGHT = 128;
    private static final String BITWIG_DEVICE_LIBRARY =
        "/Applications/Bitwig Studio.app/Contents/Resources/Library/devices";

    private final ControllerHost host;
    private JsonRpcDispatcher dispatcher;
    private CommandQueue commandQueue;
    private ServerManager serverManager;
    private StateCache stateCache;

    protected GigMaestroExtension(GigMaestroDefinition definition, ControllerHost host) {
        super(definition, host);
        this.host = host;
    }

    @Override
    public void init() {
        // Create Bitwig API objects
        Transport transport = host.createTransport();
        TrackBank trackBank = host.createMainTrackBank(TRACK_COUNT, SEND_COUNT, SCENE_COUNT);
        trackBank.setShouldShowClipLauncherFeedback(true);
        trackBank.sceneBank().setIndication(true);
        MasterTrack masterTrack = host.createMasterTrack(0);
        Application application = host.createApplication();

        // Create cursor objects for device navigation
        CursorTrack cursorTrack = host.createCursorTrack("gig-cursor", "Gig Maestro", 0, SCENE_COUNT, true);
        CursorDevice cursorDevice = cursorTrack.createCursorDevice("gig-device", "Gig Device", 0,
            CursorDeviceFollowMode.FOLLOW_SELECTION);
        CursorRemoteControlsPage remoteControlsPage = cursorDevice.createCursorRemoteControlsPage(8);

        // Create drum pad bank for reading drum pad names (128 = full MIDI range)
        com.bitwig.extension.controller.api.DrumPadBank drumPadBank = cursorDevice.createDrumPadBank(128);
        for (int i = 0; i < 128; i++) {
            com.bitwig.extension.controller.api.DrumPad pad =
                (com.bitwig.extension.controller.api.DrumPad) drumPadBank.getItemAt(i);
            pad.name().markInterested();
            pad.exists().markInterested();
        }

        // Create master cursor device for master bus FX
        CursorDevice masterCursorDevice = masterTrack.createCursorDevice("gig-master-device", 0);
        CursorRemoteControlsPage masterRemoteControlsPage = masterCursorDevice.createCursorRemoteControlsPage(8);

        // Create cursor clip for note editing
        Clip cursorClip = cursorTrack.createLauncherCursorClip("gig-clip", "Gig Clip",
            CLIP_GRID_WIDTH, CLIP_GRID_HEIGHT);

        // Create project reference
        Project project = host.getProject();

        // Create note input for real-time MIDI injection
        NoteInput noteInput = host.getMidiInPort(0).createNoteInput("Gig Maestro");
        Arpeggiator arpeggiator = noteInput.arpeggiator();
        NoteLatch noteLatch = noteInput.noteLatch();

        // Create popup browser for preset/device/sample browsing
        PopupBrowser popupBrowser = host.createPopupBrowser();

        // Create groove
        Groove groove = host.createGroove();

        // Create arranger and cue marker bank
        Arranger arranger = host.createArranger();
        CueMarkerBank cueMarkerBank = arranger.createCueMarkerBank(16);

        // Create infrastructure
        dispatcher = new JsonRpcDispatcher();
        commandQueue = new CommandQueue();
        serverManager = new ServerManager();
        stateCache = new StateCache();

        // Register all observers into StateCache
        stateCache.registerObservers(transport, trackBank, masterTrack, application, project);
        stateCache.registerClipObservers(trackBank);
        stateCache.registerDeviceObservers(cursorTrack, cursorDevice, remoteControlsPage);
        stateCache.registerClipCursorObservers(cursorClip, cursorTrack);
        stateCache.registerArrangerObservers(arranger);
        stateCache.registerArrangementObservers(transport, cueMarkerBank);
        stateCache.registerSendObservers(trackBank, SEND_COUNT);
        stateCache.registerMixerObservers(trackBank);
        stateCache.registerGroupObservers(trackBank);
        stateCache.registerMasterDeviceObservers(masterCursorDevice, masterRemoteControlsPage);
        stateCache.registerBrowserObservers(popupBrowser);
        stateCache.registerFilterObservers(popupBrowser);
        stateCache.registerNoteInputObservers(arpeggiator, noteLatch);
        stateCache.registerGrooveObservers(groove);

        // Register session/snapshot handler
        dispatcher.register("session/snapshot", params -> stateCache.getSnapshot());

        // Register api/list handler
        dispatcher.register("api/list", params -> {
            JsonArray methods = new JsonArray();
            for (String method : dispatcher.getRegisteredMethods()) {
                methods.add(new JsonPrimitive(method));
            }
            return methods;
        });

        // Register handlers
        new ApplicationHandler(application, host).register(dispatcher);
        new TransportHandler(transport, stateCache).register(dispatcher);
        TrackBankManager trackBankManager = new TrackBankManager(trackBank, TRACK_COUNT);
        new TrackHandler(trackBank, application, cursorTrack, trackBankManager, stateCache, noteInput).register(dispatcher);
        new MasterHandler(masterTrack).register(dispatcher);
        new ClipHandler(trackBank, trackBank.sceneBank(), cursorClip, stateCache).register(dispatcher);
        DeviceLibrary deviceLibrary;
        try {
            deviceLibrary = new DeviceLibrary(Paths.get(BITWIG_DEVICE_LIBRARY));
            host.println("Device library loaded: " + deviceLibrary.size() + " devices");
        } catch (IOException e) {
            host.errorln("Failed to scan device library: " + e.getMessage());
            try {
                deviceLibrary = new DeviceLibrary(Paths.get(""));
            } catch (IOException e2) {
                throw new RuntimeException("Failed to create empty device library", e2);
            }
        }
        new DeviceHandler(cursorTrack, cursorDevice, remoteControlsPage, drumPadBank, deviceLibrary, transport, host, host::scheduleTask).register(dispatcher);
        new NoteHandler(cursorClip, stateCache).register(dispatcher);
        new SceneHandler(trackBank.sceneBank(), project, stateCache).register(dispatcher);
        new ArrangerHandler(arranger, transport, cueMarkerBank, stateCache).register(dispatcher);
        new MasterDeviceHandler(masterTrack, masterCursorDevice, masterRemoteControlsPage, deviceLibrary, host::scheduleTask).register(dispatcher);
        new SendHandler(trackBank, SEND_COUNT).register(dispatcher);
        new ProjectHandler(project, stateCache).register(dispatcher);
        new TransactionHandler(dispatcher, stateCache).register(dispatcher);
        new BrowserHandler(popupBrowser, cursorDevice, stateCache).register(dispatcher);
        new NoteInputHandler(noteInput, arpeggiator, noteLatch, stateCache).register(dispatcher);
        new GrooveHandler(groove).register(dispatcher);
        new MacroHandler(dispatcher, stateCache, host::scheduleTask).register(dispatcher);

        // Start servers
        try {
            serverManager.start(DEFAULT_PORT, this::handleRequest);
            host.println("Gig Maestro started — HTTP on port " + DEFAULT_PORT
                + ", WebSocket on port " + (DEFAULT_PORT + 1));
        } catch (IOException e) {
            host.errorln("Failed to start Gig Maestro servers: " + e.getMessage());
        }
    }

    @Override
    public void flush() {
        commandQueue.drainAndExecute(dispatcher);

        // Broadcast state change notifications with delta data to WebSocket clients
        if (serverManager.getWsClientCount() > 0) {
            JsonObject delta = stateCache.getDelta();
            if (delta != null) {
                JsonObject notification = new JsonObject();
                notification.addProperty("jsonrpc", "2.0");
                notification.addProperty("method", "state/changed");
                notification.add("params", delta);
                serverManager.broadcast(notification.toString());
            }
        }
    }

    @Override
    public void exit() {
        if (serverManager != null) {
            serverManager.stop();
        }
        host.println("Gig Maestro stopped");
    }

    private CompletableFuture<String> handleRequest(String requestJson) {
        CompletableFuture<String> future = commandQueue.enqueue(requestJson);
        host.requestFlush();
        return future;
    }
}
