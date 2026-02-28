package dev.gregross.gig.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class HttpRpcServer {

    private final HttpServer server;

    public HttpRpcServer(int port, Function<String, String> requestHandler) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(4));

        server.createContext("/rpc", exchange -> handleRpc(exchange, requestHandler));
        server.createContext("/health", this::handleHealth);
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(1);
    }

    private void handleRpc(HttpExchange exchange, Function<String, String> requestHandler) throws IOException {
        addCorsHeaders(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        if (body.isBlank()) {
            sendResponse(exchange, 400, "{\"error\":\"Empty body\"}");
            return;
        }

        String response = requestHandler.apply(body);
        if (response == null) {
            // Notification — no response per JSON-RPC spec
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        sendResponse(exchange, 200, response);
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        sendResponse(exchange, 200, "{\"status\":\"ok\",\"version\":\"0.1.0\"}");
    }

    private void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }
}
