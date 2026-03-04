package dev.gregross.gig.handlers;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.NoteStep;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.gregross.gig.extension.StateCache;
import dev.gregross.gig.rpc.JsonRpcDispatcher;

public class NoteHandler {

    private static final int GRID_WIDTH = 256;
    private static final int GRID_HEIGHT = 128;
    // Step size that makes 256 grid steps cover 1024 beats (256 bars in 4/4)
    private static final double WIDE_STEP_SIZE = 4.0;

    private final Clip cursorClip;
    private final StateCache stateCache;

    public NoteHandler(Clip cursorClip, StateCache stateCache) {
        this.cursorClip = cursorClip;
        this.stateCache = stateCache;
    }

    public void register(JsonRpcDispatcher dispatcher) {

        dispatcher.register("clip/setNotes", params -> {
            JsonArray notes = requireArray(params, "notes");
            int count = 0;
            for (JsonElement el : notes) {
                JsonObject note = el.getAsJsonObject();
                int x = note.get("x").getAsInt();
                int y = note.get("y").getAsInt();
                if (x < 0 || x >= GRID_WIDTH) {
                    throw new IllegalArgumentException(
                        "note x=" + x + " is out of range (0-" + (GRID_WIDTH - 1)
                        + "). Grid covers " + GRID_WIDTH + " steps at current stepSize.");
                }
                if (y < 0 || y >= GRID_HEIGHT) {
                    throw new IllegalArgumentException(
                        "note y=" + y + " is out of range (0-" + (GRID_HEIGHT - 1) + ").");
                }
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
            double savedStepSize = stateCache.getClipStepSize();
            // Widen viewport: 64 steps × 4.0 = 256 beats (64 bars)
            cursorClip.setStepSize(WIDE_STEP_SIZE);
            cursorClip.scrollToStep(0);
            cursorClip.clearSteps();
            // Restore original step size and scroll position
            cursorClip.setStepSize(savedStepSize);
            cursorClip.scrollToStep(0);
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
                        if (step.isChanceEnabled()) {
                            note.addProperty("chance", step.chance());
                        }
                        // Expressive properties — only include non-default values
                        double pan = step.pan();
                        if (pan != 0.0) note.addProperty("pan", pan);
                        double timbre = step.timbre();
                        if (timbre != 0.0) note.addProperty("timbre", timbre);
                        double pressure = step.pressure();
                        if (pressure != 0.0) note.addProperty("pressure", pressure);
                        double gain = step.gain();
                        if (gain != 0.5) note.addProperty("gain", gain);
                        double transpose = step.transpose();
                        if (transpose != 0.0) note.addProperty("transpose", transpose);
                        double releaseVelocity = step.releaseVelocity();
                        if (releaseVelocity != 0.0) note.addProperty("releaseVelocity", releaseVelocity);
                        double velocitySpread = step.velocitySpread();
                        if (velocitySpread != 0.0) note.addProperty("velocitySpread", velocitySpread);
                        if (step.isMuted()) note.addProperty("mute", true);
                        // Occurrence
                        if (step.isOccurrenceEnabled()) {
                            note.addProperty("occurrence", step.occurrence().name());
                        }
                        // Recurrence
                        if (step.isRecurrenceEnabled()) {
                            JsonObject rec = new JsonObject();
                            rec.addProperty("length", step.recurrenceLength());
                            rec.addProperty("mask", step.recurrenceMask());
                            note.add("recurrence", rec);
                        }
                        // Repeat
                        if (step.isRepeatEnabled()) {
                            JsonObject rep = new JsonObject();
                            rep.addProperty("count", step.repeatCount());
                            rep.addProperty("curve", step.repeatCurve());
                            rep.addProperty("velocityEnd", step.repeatVelocityEnd());
                            rep.addProperty("velocityCurve", step.repeatVelocityCurve());
                            note.add("repeat", rep);
                        }
                        notes.add(note);
                    }
                }
            }
            return notes;
        });

        dispatcher.register("clip/setChance", params -> {
            JsonArray notes = requireArray(params, "notes");
            int count = 0;
            for (JsonElement el : notes) {
                JsonObject note = el.getAsJsonObject();
                int x = note.get("x").getAsInt();
                int y = note.get("y").getAsInt();
                double chance = note.get("chance").getAsDouble();
                if (chance < 0.0 || chance > 1.0) {
                    throw new IllegalArgumentException("chance must be between 0.0 and 1.0");
                }
                NoteStep step = cursorClip.getStep(0, x, y);
                step.setChance(chance);
                step.setIsChanceEnabled(true);
                count++;
            }
            JsonObject result = new JsonObject();
            result.addProperty("count", count);
            return result;
        });

        dispatcher.register("clip/setNoteExpressions", params -> {
            JsonArray notes = requireArray(params, "notes");
            int count = 0;
            for (JsonElement el : notes) {
                JsonObject note = el.getAsJsonObject();
                int x = note.get("x").getAsInt();
                int y = note.get("y").getAsInt();
                String property = note.get("property").getAsString();
                double value = note.get("value").getAsDouble();
                NoteStep step = cursorClip.getStep(0, x, y);
                switch (property) {
                    case "pan":
                        if (value < -1.0 || value > 1.0)
                            throw new IllegalArgumentException("pan must be between -1.0 and 1.0");
                        step.setPan(value);
                        break;
                    case "timbre":
                        if (value < -1.0 || value > 1.0)
                            throw new IllegalArgumentException("timbre must be between -1.0 and 1.0");
                        step.setTimbre(value);
                        break;
                    case "pressure":
                        if (value < 0.0 || value > 1.0)
                            throw new IllegalArgumentException("pressure must be between 0.0 and 1.0");
                        step.setPressure(value);
                        break;
                    case "gain":
                        if (value < 0.0 || value > 1.0)
                            throw new IllegalArgumentException("gain must be between 0.0 and 1.0 (0.5 = 0dB)");
                        step.setGain(value);
                        break;
                    case "transpose":
                        if (value < -96.0 || value > 96.0)
                            throw new IllegalArgumentException("transpose must be between -96 and +96 semitones");
                        step.setTranspose(value);
                        break;
                    case "releaseVelocity":
                        if (value < 0.0 || value > 1.0)
                            throw new IllegalArgumentException("releaseVelocity must be between 0.0 and 1.0");
                        step.setReleaseVelocity(value);
                        break;
                    case "velocitySpread":
                        if (value < 0.0 || value > 1.0)
                            throw new IllegalArgumentException("velocitySpread must be between 0.0 and 1.0");
                        step.setVelocitySpread(value);
                        break;
                    case "mute":
                        step.setIsMuted(value != 0.0);
                        break;
                    default:
                        throw new IllegalArgumentException(
                            "unknown property '" + property + "'. Valid: pan, timbre, pressure, gain, transpose, releaseVelocity, velocitySpread, mute");
                }
                count++;
            }
            JsonObject result = new JsonObject();
            result.addProperty("count", count);
            return result;
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
