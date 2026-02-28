package dev.gregross.gig.handlers;

import com.bitwig.extension.controller.api.Transport;
import com.google.gson.JsonPrimitive;
import dev.gregross.gig.rpc.JsonRpcDispatcher;

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
    }
}
