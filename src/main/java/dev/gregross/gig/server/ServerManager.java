package dev.gregross.gig.server;

import java.io.IOException;
import java.util.function.Function;

public class ServerManager {

    private HttpRpcServer httpServer;
    private WsRpcServer wsServer;

    public void start(int httpPort, Function<String, String> requestHandler) throws IOException {
        httpServer = new HttpRpcServer(httpPort, requestHandler);
        httpServer.start();

        int wsPort = httpPort + 1;
        wsServer = new WsRpcServer(wsPort, requestHandler);
        wsServer.start();
    }

    public void stop() {
        if (httpServer != null) {
            httpServer.stop();
            httpServer = null;
        }
        if (wsServer != null) {
            try {
                wsServer.stop(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            wsServer = null;
        }
    }

    public void broadcast(String json) {
        if (wsServer != null) {
            wsServer.broadcast(json);
        }
    }

    public int getWsClientCount() {
        return wsServer != null ? wsServer.getClientCount() : 0;
    }
}
