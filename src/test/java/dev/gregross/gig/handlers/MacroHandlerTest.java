package dev.gregross.gig.handlers;

import com.google.gson.*;
import dev.gregross.gig.extension.StateCache;
import dev.gregross.gig.rpc.JsonRpcDispatcher;
import dev.gregross.gig.rpc.TaskScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MacroHandlerTest {

    private JsonRpcDispatcher dispatcher;
    private List<String> callLog;

    /** Runs scheduled tasks immediately — simulates instant flush cycles for testing. */
    private static final TaskScheduler IMMEDIATE_SCHEDULER = (task, delayMs) -> task.run();

    @BeforeEach
    void setUp() {
        dispatcher = new JsonRpcDispatcher();
        callLog = new ArrayList<>();

        // Register stub handlers that log calls
        dispatcher.register("track/createAudio", params -> {
            callLog.add("track/createAudio");
            return new JsonPrimitive("ok");
        });
        dispatcher.register("track/createInstrument", params -> {
            callLog.add("track/createInstrument");
            return new JsonPrimitive("ok");
        });
        dispatcher.register("track/createEffect", params -> {
            callLog.add("track/createEffect");
            return new JsonPrimitive("ok");
        });
        dispatcher.register("track/rename", params -> {
            callLog.add("track/rename:" + params.get("name").getAsString());
            return new JsonPrimitive("ok");
        });
        dispatcher.register("device/insertBitwigDevice", params -> {
            callLog.add("device/insertBitwigDevice:" + params.get("name").getAsString());
            return new JsonPrimitive("ok");
        });
        dispatcher.register("clip/create", params -> {
            callLog.add("clip/create:t" + params.get("trackIndex").getAsInt()
                + "s" + params.get("slotIndex").getAsInt()
                + "l" + params.get("lengthInBeats").getAsInt());
            return new JsonPrimitive("ok");
        });
        dispatcher.register("clip/select", params -> {
            callLog.add("clip/select:t" + params.get("trackIndex").getAsInt()
                + "s" + params.get("slotIndex").getAsInt());
            return new JsonPrimitive("ok");
        });
        dispatcher.register("clip/setStepSize", params -> {
            callLog.add("clip/setStepSize:" + params.get("size").getAsDouble());
            return new JsonPrimitive("ok");
        });
        dispatcher.register("clip/setNotes", params -> {
            int count = params.getAsJsonArray("notes").size();
            callLog.add("clip/setNotes:" + count);
            return new JsonPrimitive(count);
        });
        dispatcher.register("clip/rename", params -> {
            callLog.add("clip/rename:" + params.get("name").getAsString());
            return new JsonPrimitive("ok");
        });
        dispatcher.register("scene/create", params -> {
            callLog.add("scene/create");
            return new JsonPrimitive("ok");
        });
        dispatcher.register("scene/rename", params -> {
            callLog.add("scene/rename:" + params.get("name").getAsString());
            return new JsonPrimitive("ok");
        });
        dispatcher.register("sceneBank/scrollBy", params -> {
            callLog.add("sceneBank/scrollBy:" + params.get("amount").getAsInt());
            return new JsonPrimitive("ok");
        });

        new MacroHandler(dispatcher, new StateCache(), IMMEDIATE_SCHEDULER).register(dispatcher);
    }

    // --- Registration ---

    @Test
    void registersFourMacroMethods() {
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("macro/createTrack"));
        assertTrue(methods.contains("macro/createClip"));
        assertTrue(methods.contains("macro/writeClip"));
        assertTrue(methods.contains("macro/buildSection"));
    }

    // --- macro/createTrack ---

    @Test
    void createTrack_audioOnly() {
        handle("macro/createTrack", """
            {"type":"audio"}""");
        assertEquals(List.of("track/createAudio"), callLog);
    }

    @Test
    void createTrack_instrumentWithName() {
        handle("macro/createTrack", """
            {"type":"instrument","name":"Synth"}""");
        assertEquals(List.of("track/createInstrument", "track/rename:Synth"), callLog);
    }

    @Test
    void createTrack_effectWithNameAndDevice() {
        handle("macro/createTrack", """
            {"type":"effect","name":"FX Bus","device":"Delay-2"}""");
        assertEquals(List.of(
            "track/createEffect",
            "track/rename:FX Bus",
            "device/insertBitwigDevice:Delay-2"
        ), callLog);
    }

    @Test
    void createTrack_invalidType() {
        String response = handle("macro/createTrack", """
            {"type":"midi"}""");
        assertTrue(response.contains("error"));
        assertTrue(response.contains("Invalid type"));
    }

    @Test
    void createTrack_missingType() {
        String response = handle("macro/createTrack", "{}");
        assertTrue(response.contains("error"));
        assertTrue(response.contains("Missing required param: type"));
    }

    // --- macro/createClip ---

    @Test
    void createClip_createsAndSelects() {
        handle("macro/createClip", """
            {"trackIndex":2,"sceneIndex":1,"lengthBeats":16}""");
        assertEquals(List.of("clip/create:t2s1l16", "clip/select:t2s1"), callLog);
    }

    @Test
    void createClip_missingTrackIndex() {
        String response = handle("macro/createClip", """
            {"sceneIndex":0,"lengthBeats":8}""");
        assertTrue(response.contains("error"));
    }

    // --- macro/writeClip ---

    @Test
    void writeClip_fullSequence() {
        handle("macro/writeClip", """
            {"trackIndex":0,"sceneIndex":1,"lengthBeats":8,"stepSize":0.25,
             "notes":[{"x":0,"y":60,"velocity":100,"duration":1},
                      {"x":4,"y":64,"velocity":80,"duration":1}]}""");
        // Phase 1: create + select; Phase 2 (deferred): setStepSize + setNotes
        assertEquals(List.of(
            "clip/create:t0s1l8",
            "clip/select:t0s1",
            "clip/setStepSize:0.25",
            "clip/setNotes:2"
        ), callLog);
    }

    @Test
    void writeClip_withName() {
        handle("macro/writeClip", """
            {"trackIndex":0,"sceneIndex":0,"lengthBeats":4,"stepSize":0.5,
             "notes":[{"x":0,"y":48,"velocity":100,"duration":1}],
             "name":"Bass Line"}""");
        assertEquals(List.of(
            "clip/create:t0s0l4",
            "clip/select:t0s0",
            "clip/setStepSize:0.5",
            "clip/setNotes:1",
            "clip/rename:Bass Line"
        ), callLog);
    }

    @Test
    void writeClip_returnsCount() {
        String response = handle("macro/writeClip", """
            {"trackIndex":0,"sceneIndex":0,"lengthBeats":4,"stepSize":0.25,
             "notes":[{"x":0,"y":60,"velocity":100,"duration":1},
                      {"x":1,"y":62,"velocity":90,"duration":1},
                      {"x":2,"y":64,"velocity":80,"duration":1}]}""");
        JsonObject result = parseResult(response);
        assertEquals(3, result.get("count").getAsInt());
    }

    @Test
    void writeClip_missingNotes() {
        String response = handle("macro/writeClip", """
            {"trackIndex":0,"sceneIndex":0,"lengthBeats":4,"stepSize":0.25}""");
        assertTrue(response.contains("error"));
    }

    // --- macro/buildSection ---

    @Test
    void buildSection_createsSceneAndWritesClips() {
        handle("macro/buildSection", """
            {"sceneName":"Verse 1","clips":[
                {"trackIndex":0,"lengthBeats":16,"stepSize":0.25,
                 "notes":[{"x":0,"y":60,"velocity":100,"duration":1}],"name":"Lead"},
                {"trackIndex":1,"lengthBeats":16,"stepSize":0.25,
                 "notes":[{"x":0,"y":48,"velocity":80,"duration":2}],"name":"Bass"}
            ]}""");
        // Phase 1: scene create + scroll + rename + create all clips + select first
        // Phase 2+ (deferred): write notes per clip with select-next chaining
        assertEquals(List.of(
            "scene/create",
            "sceneBank/scrollBy:0",
            "scene/rename:Verse 1",
            "clip/create:t0s0l16",
            "clip/create:t1s0l16",
            "clip/select:t0s0",
            "clip/setStepSize:0.25",
            "clip/setNotes:1",
            "clip/rename:Lead",
            "clip/select:t1s0",
            "clip/setStepSize:0.25",
            "clip/setNotes:1",
            "clip/rename:Bass"
        ), callLog);
    }

    @Test
    void buildSection_returnsSceneIndexAndClipCount() {
        String response = handle("macro/buildSection", """
            {"sceneName":"Chorus","clips":[
                {"trackIndex":0,"lengthBeats":8,"stepSize":0.5,
                 "notes":[{"x":0,"y":60,"velocity":100,"duration":1}]},
                {"trackIndex":1,"lengthBeats":8,"stepSize":0.5,
                 "notes":[{"x":0,"y":48,"velocity":80,"duration":1}]},
                {"trackIndex":2,"lengthBeats":8,"stepSize":0.5,
                 "notes":[{"x":0,"y":36,"velocity":90,"duration":1}]}
            ]}""");
        JsonObject result = parseResult(response);
        assertEquals(0, result.get("sceneIndex").getAsInt());
        assertEquals(3, result.get("clipCount").getAsInt());
    }

    @Test
    void buildSection_emptyClips() {
        String response = handle("macro/buildSection", """
            {"sceneName":"Empty","clips":[]}""");
        assertTrue(response.contains("error"));
        assertTrue(response.contains("must not be empty"));
    }

    @Test
    void buildSection_missingSceneName() {
        String response = handle("macro/buildSection", """
            {"clips":[{"trackIndex":0,"lengthBeats":8,"stepSize":0.25,
             "notes":[{"x":0,"y":60,"velocity":100,"duration":1}]}]}""");
        assertTrue(response.contains("error"));
    }

    @Test
    void buildSection_withExplicitSceneIndex() {
        handle("macro/buildSection", """
            {"sceneName":"Verse 1","sceneIndex":2,"clips":[
                {"trackIndex":0,"lengthBeats":16,"stepSize":0.25,
                 "notes":[{"x":0,"y":60,"velocity":100,"duration":1}],"name":"Lead"},
                {"trackIndex":1,"lengthBeats":16,"stepSize":0.25,
                 "notes":[{"x":0,"y":48,"velocity":80,"duration":2}],"name":"Bass"}
            ]}""");
        // No scene/create or sceneBank/scrollBy — uses sceneIndex directly
        assertEquals(List.of(
            "scene/rename:Verse 1",
            "clip/create:t0s2l16",
            "clip/create:t1s2l16",
            "clip/select:t0s2",
            "clip/setStepSize:0.25",
            "clip/setNotes:1",
            "clip/rename:Lead",
            "clip/select:t1s2",
            "clip/setStepSize:0.25",
            "clip/setNotes:1",
            "clip/rename:Bass"
        ), callLog);
    }

    @Test
    void buildSection_withExplicitSceneIndex_returnsIndex() {
        String response = handle("macro/buildSection", """
            {"sceneName":"Chorus","sceneIndex":3,"clips":[
                {"trackIndex":0,"lengthBeats":8,"stepSize":0.5,
                 "notes":[{"x":0,"y":60,"velocity":100,"duration":1}]}
            ]}""");
        JsonObject result = parseResult(response);
        assertEquals(3, result.get("sceneIndex").getAsInt());
        assertEquals(1, result.get("clipCount").getAsInt());
    }

    // --- Helpers ---

    private String handle(String method, String params) {
        return dispatcher.handle(
            "{\"jsonrpc\":\"2.0\",\"method\":\"" + method + "\",\"params\":" + params + ",\"id\":1}");
    }

    private JsonObject parseResult(String response) {
        return JsonParser.parseString(response).getAsJsonObject().getAsJsonObject("result");
    }
}
