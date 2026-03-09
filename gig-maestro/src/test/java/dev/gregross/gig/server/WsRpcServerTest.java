package dev.gregross.gig.server;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class WsRpcServerTest {

    private static final int TEST_PORT = 19788;
    private WsRpcServer server;

    @BeforeEach
    void setUp() throws Exception {
        server = new WsRpcServer(TEST_PORT, body -> {
            if (body.contains("\"echo\"")) {
                return CompletableFuture.completedFuture(
                    "{\"jsonrpc\":\"2.0\",\"result\":\"pong\",\"id\":1}");
            }
            return CompletableFuture.completedFuture(null); // notification
        });
        server.start();
        Thread.sleep(200); // let server bind
    }

    @AfterEach
    void tearDown() throws Exception {
        server.stop(1000);
    }

    @Test
    void clientReceivesResponse() throws Exception {
        CompletableFuture<String> responseFuture = new CompletableFuture<>();

        WebSocketClient client = new WebSocketClient(URI.create("ws://localhost:" + TEST_PORT)) {
            @Override public void onOpen(ServerHandshake handshake) {}
            @Override public void onMessage(String message) { responseFuture.complete(message); }
            @Override public void onClose(int code, String reason, boolean remote) {}
            @Override public void onError(Exception ex) { responseFuture.completeExceptionally(ex); }
        };

        client.connectBlocking(2, TimeUnit.SECONDS);
        client.send("{\"jsonrpc\":\"2.0\",\"method\":\"echo\",\"id\":1}");

        String response = responseFuture.get(2, TimeUnit.SECONDS);
        assertTrue(response.contains("\"pong\""));
        client.closeBlocking();
    }

    @Test
    void notificationSendsNoResponse() throws Exception {
        CompletableFuture<String> responseFuture = new CompletableFuture<>();

        WebSocketClient client = new WebSocketClient(URI.create("ws://localhost:" + TEST_PORT)) {
            @Override public void onOpen(ServerHandshake handshake) {}
            @Override public void onMessage(String message) { responseFuture.complete(message); }
            @Override public void onClose(int code, String reason, boolean remote) {}
            @Override public void onError(Exception ex) {}
        };

        client.connectBlocking(2, TimeUnit.SECONDS);
        client.send("{\"jsonrpc\":\"2.0\",\"method\":\"notify\"}");

        Thread.sleep(300);
        assertFalse(responseFuture.isDone());
        client.closeBlocking();
    }

    @Test
    void broadcastReachesClients() throws Exception {
        CompletableFuture<String> broadcastFuture = new CompletableFuture<>();

        WebSocketClient client = new WebSocketClient(URI.create("ws://localhost:" + TEST_PORT)) {
            @Override public void onOpen(ServerHandshake handshake) {}
            @Override public void onMessage(String message) { broadcastFuture.complete(message); }
            @Override public void onClose(int code, String reason, boolean remote) {}
            @Override public void onError(Exception ex) {}
        };

        client.connectBlocking(2, TimeUnit.SECONDS);
        Thread.sleep(100);

        server.broadcast("{\"jsonrpc\":\"2.0\",\"method\":\"state/changed\"}");

        String received = broadcastFuture.get(2, TimeUnit.SECONDS);
        assertTrue(received.contains("state/changed"));
        client.closeBlocking();
    }

    @Test
    void tracksClientCount() throws Exception {
        assertEquals(0, server.getClientCount());

        WebSocketClient client = new WebSocketClient(URI.create("ws://localhost:" + TEST_PORT)) {
            @Override public void onOpen(ServerHandshake handshake) {}
            @Override public void onMessage(String message) {}
            @Override public void onClose(int code, String reason, boolean remote) {}
            @Override public void onError(Exception ex) {}
        };

        client.connectBlocking(2, TimeUnit.SECONDS);
        Thread.sleep(100);
        assertEquals(1, server.getClientCount());

        client.closeBlocking();
        Thread.sleep(200);
        assertEquals(0, server.getClientCount());
    }
}
