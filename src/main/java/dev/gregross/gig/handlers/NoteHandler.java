package dev.gregross.gig.handlers;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.NoteStep;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.gregross.gig.rpc.JsonRpcDispatcher;

public class NoteHandler {

    private static final int GRID_WIDTH = 64;
    private static final int GRID_HEIGHT = 128;

    private final Clip cursorClip;

    public NoteHandler(Clip cursorClip) {
        this.cursorClip = cursorClip;
    }

    public void register(JsonRpcDispatcher dispatcher) {

        dispatcher.register("clip/setNotes", params -> {
            JsonArray notes = requireArray(params, "notes");
            int count = 0;
            for (JsonElement el : notes) {
                JsonObject note = el.getAsJsonObject();
                int x = note.get("x").getAsInt();
                int y = note.get("y").getAsInt();
                int velocity = note.has("velocity")
                    ? (int) (note.get("velocity").getAsDouble() * 127)
                    : 100;
                double duration = note.has("duration")
                    ? note.get("duration").getAsDouble()
                    : 0.25;
                cursorClip.setStep(0, x, y, velocity, duration);
                count++;
            }
            JsonObject result = new JsonObject();
            result.addProperty("count", count);
            return result;
        });

        dispatcher.register("clip/clearNote", params -> {
            int x = requireInt(params, "x");
            int y = requireInt(params, "y");
            cursorClip.clearStep(0, x, y);
            return new JsonPrimitive("ok");
        });

        dispatcher.register("clip/clearAllNotes", params -> {
            cursorClip.clearSteps();
            return new JsonPrimitive("ok");
        });

        dispatcher.register("clip/getNotes", params -> {
            JsonArray notes = new JsonArray();
            for (int x = 0; x < GRID_WIDTH; x++) {
                for (int y = 0; y < GRID_HEIGHT; y++) {
                    NoteStep step = cursorClip.getStep(0, x, y);
                    if (step.state().name().equals("NoteOn")) {
                        JsonObject note = new JsonObject();
                        note.addProperty("x", x);
                        note.addProperty("y", y);
                        note.addProperty("velocity", step.velocity());
                        note.addProperty("duration", step.duration());
                        notes.add(note);
                    }
                }
            }
            return notes;
        });

        dispatcher.register("clip/setStepSize", params -> {
            double size = requireDouble(params, "size");
            cursorClip.setStepSize(size);
            return new JsonPrimitive("ok");
        });

        dispatcher.register("clip/scrollSteps", params -> {
            int offset = requireInt(params, "offset");
            cursorClip.scrollToStep(offset);
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

    private JsonArray requireArray(JsonObject params, String key) {
        JsonElement el = params.get(key);
        if (el == null) {
            throw new IllegalArgumentException("missing '" + key + "' parameter");
        }
        return el.getAsJsonArray();
    }
}
