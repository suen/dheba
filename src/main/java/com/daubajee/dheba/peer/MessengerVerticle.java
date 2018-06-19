package com.daubajee.dheba.peer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.daubajee.dheba.Config;
import com.daubajee.dheba.Topic;

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

	private static AtomicInteger instanceCount = new AtomicInteger(0);
    
	private final Logger LOGGER = LoggerFactory.getLogger(MessengerVerticle.class + "-" +instanceCount.incrementAndGet());
    
    static final String NAME = MessengerVerticle.class.getSimpleName();

    private Map<String, NetSocket> remotes = new ConcurrentHashMap<>();

    private EventBus eventBus;

    private Config config;

	private int p2pPort = 0;

    public MessengerVerticle(Config config) {
        this.config = config;
    }

    @Override
    public void start() throws Exception {

        eventBus = vertx.eventBus();
        
        NetServer server = vertx.createNetServer();

        server.connectHandler(this::onNewClientPeerConnect);

        p2pPort = config.getP2PPort();
        server.listen(p2pPort, res -> {
            if (res.failed()) {
                LOGGER.error("Binding on port " + p2pPort + " failed", res.cause());
                return;
            }
            LOGGER.info("P2P listening on " + p2pPort);
        });

        eventBus.consumer(Topic.REMOTE_PEER_OUTBOX, this::onRemotePeerOutbox);
    }

    private void onRemotePeerOutbox(Message<JsonObject> msg) {
        JsonObject body = msg.body();
        RemotePeerPacket packet = RemotePeerPacket.from(body);
        if (!packet.isValid()) {
            LOGGER.warn("Remote Packet Invalid " + body);
            return;
        }

        String hostAddress = packet.getRemoteHostAddress();
        Integer port = packet.getRemoteHostPort();
        JsonObject content = packet.getContent();

        boolean selfAddressed = checkifSelfAddressed(hostAddress, port);
        if (selfAddressed) {
        	LOGGER.info("Self addressed " + body);
        	return;
        }
        
        getorCreateRemoteSocket(hostAddress, port).thenAccept(socket -> {
            Buffer socketFrame = PeerUtils.toSocketFrame(content);
            socket.write(socketFrame);
            LOGGER.info("Message sent to " + hostAddress + ":" + port);
            msg.reply(new JsonObject());
        });
    }

    private boolean checkifSelfAddressed(String hostAddress, Integer port) {
		try {
			InetAddress address = InetAddress.getByName(hostAddress);
			String ip = address.getHostAddress();
			return ip.startsWith("127.") && port == p2pPort;
		} catch (UnknownHostException e) {
		}
    	
		return false;
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

        RemotePeerEvent event = new RemotePeerEvent(remoteAddress.host(), remoteAddress.port(), RemotePeerEvent.CONNECTED);
        eventBus.publish(Topic.REMOTE_PEER_EVENTS, event.toJson());
    }

    private void onPeerMessage(Buffer buffer, NetSocket socket) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Packet received : ", buffer.toString());
        }

        JsonObject msg = PeerUtils.fromSocketFrame(buffer);

        SocketAddress remoteAddress = socket.remoteAddress();
        
        RemotePeerPacket packet = new RemotePeerPacket(remoteAddress.host(), remoteAddress.port(), msg);

        eventBus.publish(Topic.REMOTE_PEER_INBOX, packet.toJson());
    }

    private void onPeerClose(NetSocket socket) {
        SocketAddress remoteAddress = socket.remoteAddress();
        
        String remoteAddr = remoteAddress.toString();
        remotes.remove(remoteAddr);

        LOGGER.info("Peer disconnected : " + remoteAddr);
        RemotePeerEvent event = new RemotePeerEvent(remoteAddress.host(), remoteAddress.port(),
                RemotePeerEvent.DISCONNECTED);

        eventBus.publish(Topic.REMOTE_PEER_EVENTS, event.toJson());
    }

}
