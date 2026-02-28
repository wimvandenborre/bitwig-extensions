package dev.gregross.gig.server;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;

public class WsRpcServer extends WebSocketServer {

    private final Function<String, CompletableFuture<String>> requestHandler;
    private final Set<WebSocket> clients = new CopyOnWriteArraySet<>();

    public WsRpcServer(int port, Function<String, CompletableFuture<String>> requestHandler) {
        super(new InetSocketAddress(port));
        this.requestHandler = requestHandler;
        setReuseAddr(true);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        clients.add(conn);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        clients.remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        CompletableFuture<String> future = requestHandler.apply(message);
        future.thenAccept(response -> {
            if (response != null && conn.isOpen()) {
                conn.send(response);
            }
        });
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (conn != null) {
            clients.remove(conn);
        }
    }

    @Override
    public void onStart() {
        // Server started
    }

    public void broadcast(String json) {
        for (WebSocket client : clients) {
            if (client.isOpen()) {
                client.send(json);
            }
        }
    }

    public int getClientCount() {
        return clients.size();
    }
}
