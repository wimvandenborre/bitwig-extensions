package dev.gregross.gig.extension;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import dev.gregross.gig.handlers.ApplicationHandler;
import dev.gregross.gig.handlers.ClipHandler;
import dev.gregross.gig.handlers.DeviceHandler;
import dev.gregross.gig.handlers.MasterHandler;
import dev.gregross.gig.handlers.TrackHandler;
import dev.gregross.gig.handlers.TransportHandler;
import dev.gregross.gig.rpc.CommandQueue;
import dev.gregross.gig.rpc.JsonRpcDispatcher;
import dev.gregross.gig.server.ServerManager;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class GigMaestroExtension extends ControllerExtension {

    private static final int DEFAULT_PORT = 8787;
    private static final int TRACK_COUNT = 64;
    private static final int SCENE_COUNT = 8;

    private final ControllerHost host;
    private JsonRpcDispatcher dispatcher;
    private CommandQueue commandQueue;
    private ServerManager serverManager;

    protected GigMaestroExtension(GigMaestroDefinition definition, ControllerHost host) {
        super(definition, host);
        this.host = host;
    }

    @Override
    public void init() {
        // Create Bitwig API objects
        Transport transport = host.createTransport();
        TrackBank trackBank = host.createMainTrackBank(TRACK_COUNT, 0, SCENE_COUNT);
        MasterTrack masterTrack = host.createMasterTrack(0);
        Application application = host.createApplication();

        // Create cursor objects for device navigation
        CursorTrack cursorTrack = host.createCursorTrack("gig-cursor", "Gig Maestro", 0, SCENE_COUNT, true);
        CursorDevice cursorDevice = cursorTrack.createCursorDevice("gig-device", "Gig Device", 0,
            CursorDeviceFollowMode.FOLLOW_SELECTION);
        CursorRemoteControlsPage remoteControlsPage = cursorDevice.createCursorRemoteControlsPage(8);

        // Create infrastructure
        dispatcher = new JsonRpcDispatcher();
        commandQueue = new CommandQueue();
        serverManager = new ServerManager();
        StateCache stateCache = new StateCache();

        // Register all observers into StateCache
        stateCache.registerObservers(transport, trackBank, masterTrack, application);
        stateCache.registerClipObservers(trackBank);
        stateCache.registerDeviceObservers(cursorTrack, cursorDevice, remoteControlsPage);

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
        new ApplicationHandler(application).register(dispatcher);
        new TransportHandler(transport).register(dispatcher);
        new TrackHandler(trackBank).register(dispatcher);
        new MasterHandler(masterTrack).register(dispatcher);
        new ClipHandler(trackBank, trackBank.sceneBank()).register(dispatcher);
        new DeviceHandler(cursorTrack, cursorDevice, remoteControlsPage).register(dispatcher);

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
