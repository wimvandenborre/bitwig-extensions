package dev.gregross.gig.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class HttpRpcServerTest {

    private static final int TEST_PORT = 19787;
    private static final String TEST_TOKEN = "0123456789abcdef0123456789abcdef";
    private HttpRpcServer server;
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void setUp() throws IOException {
        server = new HttpRpcServer(TEST_PORT, TEST_TOKEN, body -> {
            if (body.contains("\"echo\"")) {
                return CompletableFuture.completedFuture(
                    "{\"jsonrpc\":\"2.0\",\"result\":\"pong\",\"id\":1}");
            }
            return CompletableFuture.completedFuture(null); // notification
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void healthEndpointReturnsOk() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + TEST_PORT + "/health"))
            .header("Authorization", "Bearer " + TEST_TOKEN)
            .GET()
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"status\":\"ok\""));
        assertTrue(response.body().contains("\"version\":\"0.1.0\""));
    }

    @Test
    void rpcEndpointProcessesPost() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + TEST_PORT + "/rpc"))
            .POST(HttpRequest.BodyPublishers.ofString(
                "{\"jsonrpc\":\"2.0\",\"method\":\"echo\",\"id\":1}"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + TEST_TOKEN)
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"pong\""));
    }

    @Test
    void rpcNotificationReturns204() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + TEST_PORT + "/rpc"))
            .POST(HttpRequest.BodyPublishers.ofString(
                "{\"jsonrpc\":\"2.0\",\"method\":\"notify\"}"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + TEST_TOKEN)
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(204, response.statusCode());
    }

    @Test
    void rpcRejectsGetMethod() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + TEST_PORT + "/rpc"))
            .header("Authorization", "Bearer " + TEST_TOKEN)
            .GET()
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, response.statusCode());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + TEST_PORT + "/health"))
            .GET()
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("Unauthorized"));
    }

    @Test
    void serverBindsOnlyToLoopback() {
        assertTrue(server.getAddress().getAddress().isLoopbackAddress());
    }
}
