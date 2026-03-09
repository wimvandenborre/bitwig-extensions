package dev.gregross.gig.rpc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

@FunctionalInterface
public interface MethodHandler {
    JsonElement handle(JsonObject params) throws Exception;
}
