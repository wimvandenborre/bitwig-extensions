package dev.gregross.gig.handlers;

import com.bitwig.extension.controller.api.Transport;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.gregross.gig.rpc.JsonRpcDispatcher;

import java.util.Set;

public class TransportHandler {

    private final Transport transport;

    public TransportHandler(Transport transport) {
        this.transport = transport;
    }

    public void register(JsonRpcDispatcher dispatcher) {
        dispatcher.register("transport/play", params -> {
            transport.play();
            return new JsonPrimitive("ok");
        });

        dispatcher.register("transport/stop", params -> {
            transport.stop();
            return new JsonPrimitive("ok");
        });

        dispatcher.register("transport/record", params -> {
            transport.record();
            return new JsonPrimitive("ok");
        });

        dispatcher.register("transport/togglePlay", params -> {
            transport.togglePlay();
            return new JsonPrimitive("ok");
        });

        dispatcher.register("transport/rewind", params -> {
            transport.rewind();
            return new JsonPrimitive("ok");
        });

        dispatcher.register("transport/fastForward", params -> {
            transport.fastForward();
            return new JsonPrimitive("ok");
        });

        dispatcher.register("transport/tapTempo", params -> {
            transport.tapTempo();
            return new JsonPrimitive("ok");
        });

        dispatcher.register("transport/setTempo", params -> {
            if (!params.has("tempo")) {
                throw new IllegalArgumentException("missing 'tempo' parameter");
            }
            double tempo = params.get("tempo").getAsDouble();
            transport.tempo().value().setRaw(tempo);
            return new JsonPrimitive("ok");
        });

        dispatcher.register("transport/setPosition", params -> {
            if (!params.has("beats")) {
                throw new IllegalArgumentException("missing 'beats' parameter");
            }
            double beats = params.get("beats").getAsDouble();
            transport.getPosition().set(beats);
            return new JsonPrimitive("ok");
        });

        dispatcher.register("transport/setLoop", params -> {
            if (!params.has("enabled")) {
                throw new IllegalArgumentException("missing 'enabled' parameter");
            }
            boolean enabled = params.get("enabled").getAsBoolean();
            transport.isArrangerLoopEnabled().set(enabled);
            return new JsonPrimitive("ok");
        });

        dispatcher.register("transport/setMetronome", params -> {
            if (!params.has("enabled")) {
                throw new IllegalArgumentException("missing 'enabled' parameter");
            }
            boolean enabled = params.get("enabled").getAsBoolean();
            transport.isMetronomeEnabled().set(enabled);
            return new JsonPrimitive("ok");
        });

        // --- Loop range ---

        dispatcher.register("transport/setLoopRange", params -> {
            if (!params.has("start")) {
                throw new IllegalArgumentException("missing 'start' parameter");
            }
            if (!params.has("duration")) {
                throw new IllegalArgumentException("missing 'duration' parameter");
            }
            if (!params.has("enabled")) {
                throw new IllegalArgumentException("missing 'enabled' parameter");
            }
            transport.arrangerLoopStart().set(params.get("start").getAsDouble());
            transport.arrangerLoopDuration().set(params.get("duration").getAsDouble());
            transport.isArrangerLoopEnabled().set(params.get("enabled").getAsBoolean());
            return ok();
        });

        dispatcher.register("transport/getLoopRange", params -> {
            JsonObject result = new JsonObject();
            result.addProperty("loopStart", transport.arrangerLoopStart().get());
            result.addProperty("loopDuration", transport.arrangerLoopDuration().get());
            result.addProperty("loopEnabled", transport.isArrangerLoopEnabled().get());
            result.addProperty("punchInPosition", transport.getInPosition().get());
            result.addProperty("punchInEnabled", transport.isPunchInEnabled().get());
            result.addProperty("punchOutPosition", transport.getOutPosition().get());
            result.addProperty("punchOutEnabled", transport.isPunchOutEnabled().get());
            return result;
        });

        // --- Punch in/out ---

        dispatcher.register("transport/setPunchIn", params -> {
            if (!params.has("position")) {
                throw new IllegalArgumentException("missing 'position' parameter");
            }
            if (!params.has("enabled")) {
                throw new IllegalArgumentException("missing 'enabled' parameter");
            }
            transport.getInPosition().set(params.get("position").getAsDouble());
            transport.isPunchInEnabled().set(params.get("enabled").getAsBoolean());
            return ok();
        });

        dispatcher.register("transport/setPunchOut", params -> {
            if (!params.has("position")) {
                throw new IllegalArgumentException("missing 'position' parameter");
            }
            if (!params.has("enabled")) {
                throw new IllegalArgumentException("missing 'enabled' parameter");
            }
            transport.getOutPosition().set(params.get("position").getAsDouble());
            transport.isPunchOutEnabled().set(params.get("enabled").getAsBoolean());
            return ok();
        });

        // --- Automation ---

        dispatcher.register("transport/setAutomationWriteMode", params -> {
            if (!params.has("mode")) {
                throw new IllegalArgumentException("missing 'mode' parameter");
            }
            String mode = params.get("mode").getAsString();
            if (!AUTOMATION_MODES.contains(mode)) {
                throw new IllegalArgumentException("invalid mode '" + mode + "', expected: latch, touch, write");
            }
            transport.automationWriteMode().set(mode);
            return ok();
        });

        dispatcher.register("transport/setArrangerAutomationWrite", params -> {
            if (!params.has("enabled")) {
                throw new IllegalArgumentException("missing 'enabled' parameter");
            }
            transport.isArrangerAutomationWriteEnabled().set(params.get("enabled").getAsBoolean());
            return ok();
        });

        dispatcher.register("transport/setClipLauncherAutomationWrite", params -> {
            if (!params.has("enabled")) {
                throw new IllegalArgumentException("missing 'enabled' parameter");
            }
            transport.isClipLauncherAutomationWriteEnabled().set(params.get("enabled").getAsBoolean());
            return ok();
        });

        dispatcher.register("transport/resetAutomationOverrides", params -> {
            transport.resetAutomationOverrides();
            return ok();
        });
    }

    private static final Set<String> AUTOMATION_MODES = Set.of("latch", "touch", "write");

    private JsonObject ok() {
        JsonObject result = new JsonObject();
        result.addProperty("ok", true);
        return result;
    }
}
