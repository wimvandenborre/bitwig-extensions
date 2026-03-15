package dev.gregross.gig.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class HttpRpcServer {

    private final HttpServer server;
    private static final long TIMEOUT_MS = 5000;

    public HttpRpcServer(int port, Function<String, CompletableFuture<String>> requestHandler) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(4));

        server.createContext("/rpc", exchange -> handleRpc(exchange, requestHandler));
        server.createContext("/health", this::handleHealth);
        server.createContext("/docs", this::handleDocs);
        server.createContext("/openapi.json", this::handleOpenApiSpec);
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(1);
    }

    private void handleRpc(HttpExchange exchange, Function<String, CompletableFuture<String>> requestHandler) throws IOException {
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

        try {
            CompletableFuture<String> future = requestHandler.apply(body);
            String response = future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);

            if (response == null) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
            } else {
                sendResponse(exchange, 200, response);
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\":\"Internal server error\"}");
        }
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        sendResponse(exchange, 200, "{\"status\":\"ok\",\"version\":\"0.1.0\"}");
    }

    private void handleDocs(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        serveClasspathResource(exchange, "/docs/api.html", "text/html");
    }

    private void handleOpenApiSpec(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        serveClasspathResource(exchange, "/docs/openapi.json", "application/json");
    }

    private void serveClasspathResource(HttpExchange exchange, String path, String contentType) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                sendResponse(exchange, 404, "{\"error\":\"Resource not found\"}");
                return;
            }
            byte[] bytes = is.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
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
