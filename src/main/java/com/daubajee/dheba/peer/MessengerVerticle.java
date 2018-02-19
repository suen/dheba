package com.daubajee.dheba.peer;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.daubajee.dheba.Config;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;

public class MessengerVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessengerVerticle.class);

    static final String NAME = MessengerVerticle.class.getSimpleName();

    static final String PEER_CONNECT = "PEER_CONNECT";
    static final String PEER_SEND = "PEER_SEND";

    private Map<String, NetSocket> remotes = new ConcurrentHashMap<>();

    private EventBus eventBus;

    private Config config;

    public MessengerVerticle() {
        config = Config.instance();
    }

    @Override
    public void start() throws Exception {

        eventBus = vertx.eventBus();

        eventBus.consumer(NAME, this::onMessage);

        NetServer server = vertx.createNetServer();

        server.connectHandler(this::onNewClientPeerConnect);

        int p2PPort = config.getP2PPort();
        server.listen(p2PPort, res -> {
            if (res.failed()) {
                LOGGER.error("Binding on port " + p2PPort + " failed", res.cause());
                return;
            }
            LOGGER.info("P2P listening on " + p2PPort);
        });
    }

    private void onMessage(Message<JsonObject> msg) {
        JsonObject body = msg.body();
        String type = body.getString(S.TYPE, "");
        if (type.isEmpty()) {
            return;
        }
        String remoteHost;
        Integer port;

        switch (type) {
            case PEER_SEND :
                remoteHost = body.getString(S.REMOTE_HOST, "");
                port = body.getInteger(S.REMOTE_PORT, 0);

                getorCreateRemoteSocket(remoteHost, port)
                    .thenAccept(socket -> {
                        JsonObject message = body.getJsonObject(S.MESSAGE);
                        
                        JsonObject peerSendMsg = PeerUtils.createPeerSendMsg(message);
                        
                        Buffer socketFrame = PeerUtils.toSocketFrame(peerSendMsg);
                        LOGGER.info("Send message to " + remoteHost + ":" + port + " of type : " + message.getString(S.TYPE));
                        socket.write(socketFrame);
                        msg.reply(new JsonObject());
                    });
                break;
            default :
                LOGGER.warn("Unknown message type " + type);
                break;
        }
    }

    private CompletableFuture<NetSocket> getorCreateRemoteSocket(String remoteHost, Integer port) {
        CompletableFuture<NetSocket> netSocketContainer = new CompletableFuture<>();

        String remoteAddress = remoteHost + ":" + port;
        NetSocket netSocket = remotes.get(remoteAddress);
        if (netSocket != null) {
            netSocketContainer.complete(netSocket);
            return netSocketContainer;
        }

        LOGGER.info("No active socket for remoteAddress " + remoteAddress + ", creating one..");
        NetClient client = vertx.createNetClient();
        client.connect(port, remoteHost, handler -> {

            String remote = remoteHost + ":" + port;
            if (handler.failed()) {
                LOGGER.info("Connection to remoteAddress '" + remote + "' failed");
                return;
            } else {
                LOGGER.info("Connected to remoteAddress: " + remote);
            }

            NetSocket socket = handler.result();

            socket.handler(buffer -> onPeerMessage(buffer, socket));

            socket.closeHandler(end -> onPeerClose(socket));

            remotes.put(remoteAddress, socket);
            netSocketContainer.complete(socket);
        });

        return netSocketContainer;
    }

    private void onNewClientPeerConnect(NetSocket socket) {

        socket.handler(buffer -> onPeerMessage(buffer, socket));

        socket.closeHandler(end -> onPeerClose(socket));

        SocketAddress remoteAddress = socket.remoteAddress();
        
        String remoteAddressStr = remoteAddress.toString();
        
        remotes.put(remoteAddressStr, socket);

        LOGGER.info("New Client peer connected : " + remoteAddressStr);
    }

    private void onPeerMessage(Buffer buffer, NetSocket socket) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(NAME + " received : ", buffer.toString());
        }

        JsonObject msg = PeerUtils.fromSocketFrame(buffer);

        JsonObject body = msg.getJsonObject(S.BODY, new JsonObject());
        String type = msg.getString(S.TYPE);
        String address = msg.getString(S.ADDRESS);

        if (type.isEmpty() || address.isEmpty() || body.isEmpty()) {
            LOGGER.info("Incoming message of bad format : ", msg);
            return;
        }
        SocketAddress remoteAddress = socket.remoteAddress();
        
        JsonObject peerMsg = new JsonObject()
                .put(S.REMOTE_HOST, remoteAddress.host())
                .put(S.REMOTE_PORT, remoteAddress.port())
                .put(S.TYPE, PeerVerticle.PEER_MSG)
                .put(S.MESSAGE, msg);

        eventBus.publish(PeerVerticle.NAME, peerMsg);
    }

    private void onPeerClose(NetSocket socket) {
        String remoteAddr = socket.remoteAddress().toString();
        LOGGER.info("Peer disconnected : " + remoteAddr);
        remotes.remove(remoteAddr);
    }

}
