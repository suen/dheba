package com.daubajee.dheba.peer;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.daubajee.dheba.Config;
import com.daubajee.dheba.peer.msg.HandShake;

import io.reactivex.subjects.PublishSubject;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class PeerVerticle extends AbstractVerticle {

    public static final String NAME = PeerVerticle.class.getSimpleName();

    public static final String ADD_NEW_PEER = "ADD_NEW_PEER";

    public static final String NEW_PEER_CONNECTED = "NEW_PEER_CONNECTED";

    public static final String PEER_MSG = "PEER_MSG";

    public static final String GET_PEER_LIST = "GET_PEER_LIST";

    private static final UUID PEER_UUID = UUID.randomUUID();

    private EventBus eventBus;

    private static final Logger LOGGER = LoggerFactory.getLogger(PeerVerticle.class);

    private Config config = Config.instance();

    private Set<String> knownSelfAddresses = new ConcurrentHashSet<String>();

    private Map<String, RemotePeer> remotePeers = new ConcurrentHashMap<>();

    private Set<String> activePeers = new ConcurrentHashSet<>();

    PublishSubject<JsonObject> internalBus = PublishSubject.create();

    @Override
    public void start() throws Exception {
        vertx.deployVerticle(new MessengerVerticle());

        eventBus = vertx.eventBus();

        eventBus.consumer(ADD_NEW_PEER, this::onAddNewPeer);

        eventBus.consumer(GET_PEER_LIST, this::onGetPeerList);

        eventBus.consumer(NAME, this::onMessage);

        cycle();
    }

    private void onMessage(Message<JsonObject> msg) {
        JsonObject body = msg.body();
        String type = body.getString(S.TYPE, "");
        if (type.isEmpty()) {
            return;
        }
        switch (type) {
            case NEW_PEER_CONNECTED :
                onNewPeerConnected(msg);
                break;
            case PEER_MSG :
                onPeerMsg(msg);
                break;
            default :
                LOGGER.warn("Unknown message type: " + type);
                break;
        }
    }

    private void cycle() {

        config.getInitialPeerSeeds().forEach(seedAddr -> {
            LOGGER.info("Adding seed addr: " + seedAddr);
            eventBus.publish(ADD_NEW_PEER, seedAddr);
        });

        vertx.periodicStream(10000).handler(handler -> {
            LOGGER.info("Number of connected peers : " + activePeers.size());
            if (activePeers.size() != config.getMaxPeerConnections()) {
                remotePeers.values()
                    .stream()
                        .filter(peer -> !activePeers.contains(peer.identifier()))
                    .findFirst()
                    .ifPresent(peer -> {
                        LOGGER.info("Sending handshake to a unconnected peer");
                        sendHandshake(peer.getHostAddress(), peer.getPort());
                    });
            }
        });

    }

    private void onAddNewPeer(Message<Object> msg) {
        vertx.timerStream(3000)
            .handler(handler -> onAddNewPeerDelayed(msg));
    }

    private void onAddNewPeerDelayed(Message<Object> msg) {
        Object bodyObj = msg.body();
        JsonObject body;
        if (bodyObj instanceof String) {
            String[] parts = bodyObj.toString().split(":");
            body = new JsonObject()
                .put(S.REMOTE_HOST, parts[0])
                .put(S.REMOTE_PORT, Integer.parseInt(parts[1]));
        } else if (bodyObj instanceof JsonObject) {
            body = (JsonObject) bodyObj;
        } else {
            LOGGER.error("Bad request : ", bodyObj);
            return;
        }

        String remoteHost = body.getString(S.REMOTE_HOST, "");
        Integer remotePort = body.getInteger(S.REMOTE_PORT, 0);
        if (remoteHost.isEmpty() || remotePort == 0) {
            LOGGER.warn("Bad message ", msg);
            return;
        }

        RemotePeer remotePeer = new RemotePeer(remotePort, remoteHost);
        remotePeers.put(remotePeer.identifier(), remotePeer);

        sendHandshake(remotePeer.getHostAddress(), remotePeer.getPort());
    }

    private void sendHandshake(String remoteHost, Integer remotePort) {

        JsonObject handShakeMsg = createHandShakeMessage(remoteHost, remotePort, config.getHostname(),
                config.getP2PPort());
        JsonObject peerPacket = createRemotePeerPacket("HANDSHAKE", handShakeMsg);

        JsonObject verticleMsgPacket = createPeerSendVerticlePacket(remoteHost, remotePort, peerPacket);

        eventBus.send(MessengerVerticle.NAME, verticleMsgPacket, result -> {
            if (result.failed()) {
                LOGGER.warn("Connection to " + remoteHost + ":" + remotePort + " failed");
                return;
            }
            LOGGER.info("Connected to " + remoteHost + ":" + remotePort + ", awaiting HANDSHAKE_ACK");
            activePeers.add(remoteHost + ":" + remotePort);
        });
    }

    private void onNewPeerConnected(Message<JsonObject> msg) {
        JsonObject body = msg.body();
        String remoteHost = body.getString(S.REMOTE_HOST, "");
        Integer remotePort = body.getInteger(S.REMOTE_PORT, 0);
        if (remoteHost.isEmpty() || remotePort == 0) {
            LOGGER.warn("Bad message ", msg);
            return;
        }

        getPeerOrCreate(remotePort, remoteHost);

        JsonObject handShakeMsg = createHandShakeMessage(remoteHost, remotePort, config.getHostname(), config.getP2PPort());
        JsonObject peerPacket = createRemotePeerPacket("HANDSHAKE", handShakeMsg);
        JsonObject verticleMsgPacket = createPeerSendVerticlePacket(remoteHost, remotePort, peerPacket);
        eventBus.publish(MessengerVerticle.NAME, verticleMsgPacket);
    }

    private void onPeerMsg(Message<JsonObject> verticlePacket) {
        JsonObject body = verticlePacket.body();
        String remoteHost = body.getString(S.REMOTE_HOST, "");
        int remotePort = body.getInteger(S.REMOTE_PORT, 0);

        LOGGER.info("Received PEER_MSG : " + body.toString());

        JsonObject peerPacket = body.getJsonObject(S.MESSAGE, new JsonObject());


        String peerPacketType = peerPacket.getString(S.TYPE, "");
        if (!"MESSAGE".equals(peerPacketType)) {
            LOGGER.info("Unknown Peer packet type :" + peerPacketType);
            return;
        }

        updatePeerStatus(remoteHost, remotePort);

        JsonObject msgBody = peerPacket.getJsonObject("body", new JsonObject());
        if (msgBody.isEmpty()) {
            LOGGER.info("Peer MESSAGE packet is empty");
            return;
        }

        JsonObject content = msgBody.getJsonObject(S.CONTENT, new JsonObject());

        // The type of 'MESSAGE' packet
        String type = msgBody.getString(S.TYPE, "");

        switch (type) {
            case "HANDSHAKE" :
                if (content.isEmpty()) {
                    LOGGER.warn("HANDSHAKE message has empty content");
                } else {
                    updatePeerInfo(remoteHost, remotePort, content);
                    sendHandshakeAct(remoteHost, remotePort);
                }
                break;
            case "HANDSHAKE_ACK" :
                LOGGER.warn("HANDSHAKE_ACK message received");
                break;
            case "GET_PEER_LIST" :
                JsonObject peers = getRemotePeersJson();
                JsonObject peerListReply = createRemotePeerPacket("PEER_LIST", peers);
                JsonObject verticleMsgPacket = createPeerSendVerticlePacket(remoteHost, remotePort, peerListReply);
                eventBus.send(MessengerVerticle.NAME, verticleMsgPacket);
                break;
            case "PEER_LIST" :
                JsonArray newPeerListArray = content.getJsonArray("peers", new JsonArray());
                List<JsonObject> newPeerList = newPeerListArray.getList();
                onNewPeerList(newPeerList);
                break;
            default :
                LOGGER.info("Message type unrecognized");
                break;
        }
    }


    private void updatePeerInfo(String remoteHost, int remotePort, JsonObject handshakeMsg) {
        HandShake peerHandshake = HandShake.fromJson(handshakeMsg);
        String addrPeer = peerHandshake.getAddrMe();
        RemotePeer peer = new RemotePeer(addrPeer);
        peer.setActive(true);
        peer.setLastConnected(System.currentTimeMillis());
        peer.setAgent(peerHandshake.getAgent());
        peer.setVersion(peerHandshake.getVersion());
        peer.setBestHeight(peerHandshake.getBestHeight());
        peer.setServices(peerHandshake.getServices());

        String addrYou = peerHandshake.getAddrYou();
        Matcher matcher = Config.P2P_ADDRESS_PATTERN.matcher(addrYou);
        if (matcher.matches()) {
            String selfHostname = matcher.group(0);
            knownSelfAddresses.add(selfHostname);
        }

        remotePeers.put(peer.identifier(), peer);

        LOGGER.info("Peer updated : " + peer.toJson());
    }

    private void sendHandshakeAct(String remoteHost, int remotePort) {
        JsonObject handshakeReply = createRemotePeerPacket("HANDSHAKE_ACK", new JsonObject());
        JsonObject verticleMsgPacket = createPeerSendVerticlePacket(remoteHost, remotePort, handshakeReply);
        eventBus.send(MessengerVerticle.NAME, verticleMsgPacket);
        LOGGER.info("HANDSHAKE_ACK sent");
    }

    private void updatePeerStatus(String hostname, Integer port) {
        RemotePeer peer = getPeerOrCreate(port, hostname);

        peer.updateLastConnected(System.currentTimeMillis());
        peer.setActive(true);
    }

    private void onGetPeerList(Message<JsonObject> msg) {
        JsonObject result = getRemotePeersJson();
        msg.reply(result);
    }

    private JsonObject getRemotePeersJson() {
        List<JsonObject> peers = remotePeers.values().stream().map(p -> p.toJson()).collect(Collectors.toList());
        JsonObject result = new JsonObject().put("peers", new JsonArray(peers));
        return result;
    }

    private RemotePeer getPeerOrCreate(int port, String hostname) {
        RemotePeer peer = remotePeers.get(hostname + ":" + port);
        if (peer == null) {
            peer = new RemotePeer(port, hostname);
            remotePeers.put(peer.identifier(), peer);
        }
        return peer;
    }

    private void onNewPeerList(List<JsonObject> newPeerList) {
        newPeerList.stream().map(json -> RemotePeer.fromJson(json)).forEach(peer -> {
            String peerIdentity = peer.identifier();

            if (knownSelfAddresses.contains(peerIdentity)) {
                return;
            }

            RemotePeer existing = remotePeers.get(peerIdentity);
            if (existing == null) {
                peer.setActive(false);
                peer.setLastConnected(-1);
                LOGGER.info("New peer added " + peerIdentity);
                remotePeers.put(peerIdentity, peer);
            }
        });

    }
    
    private JsonObject createRemotePeerPacket(String type, JsonObject content) {
        return new JsonObject()
            .put(S.TYPE, type)
            .put(S.CONTENT, content);
    }

    private JsonObject createPeerSendVerticlePacket(String remoteHost, Integer remotePort, JsonObject handShakeMsg) {
        return new JsonObject()
                .put(S.REMOTE_HOST, remoteHost)
                .put(S.REMOTE_PORT, remotePort)
                .put(S.TYPE, MessengerVerticle.PEER_SEND)
                .put(S.MESSAGE, handShakeMsg);
    }

    private JsonObject createHandShakeMessage(String remoteHost, Integer remotePort, String selfHost, Integer selfPort) {
        HandShake handShake = new HandShake();
        handShake.setAddrMe(selfHost + ":" + selfPort);
        handShake.setAddrYou(remoteHost + ":" + remotePort);
        handShake.setAgent("dheba 0.1");
        handShake.setBestHeight(1);
        handShake.setServices("NODE BETA ALPHA");
        handShake.setTimestamp(System.currentTimeMillis());
        handShake.setVersion("0.1");
        handShake.setUuid(PEER_UUID);
        return handShake.toJson();
    }

}
