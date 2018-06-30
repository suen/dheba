package com.daubajee.dheba.peer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.daubajee.dheba.Topic;
import com.daubajee.dheba.peer.msg.HandShake;
import com.daubajee.dheba.peer.msg.PeerMessage;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class PeerManagerVerticle extends AbstractVerticle {

    private EventBus eventBus;

    private final Logger LOGGER = LoggerFactory.getLogger(PeerManagerVerticle.class);

    private Map<String, String> deployedVerticles = new ConcurrentHashMap<>();
    
    private Map<String, Peer> peers = new ConcurrentHashMap<>();

    private final int MAX_OUTGOING_CONNECTION = 5;

    @Override
    public void start() throws Exception {

        eventBus = vertx.eventBus();

        eventBus.consumer(Topic.REMOTE_PEER_EVENTS, this::onRemotePeerEvent);

        eventBus.consumer(Topic.REMOTE_PEER_INBOX, this::onRemotePeerMessage);

        vertx.setPeriodic(1000, this::onPeriodStream);
    }

    private void onPeriodStream(Long tick) {

    }

    private void onRemotePeerEvent(Message<JsonObject> msg) {
        JsonObject body = msg.body();

        RemotePeerEvent remotePeerEvent = RemotePeerEvent.from(body);

        if (!remotePeerEvent.isValid()) {
            LOGGER.warn("Invalid {} event : {}", Topic.REMOTE_PEER_EVENTS, remotePeerEvent);
            return;
        }
        LOGGER.info("Message of type {} received on {}", remotePeerEvent.getType(), Topic.REMOTE_PEER_EVENTS);
        
        String type = remotePeerEvent.getType();
        switch (type) {
            case RemotePeerEvent.CONNECTED :
                deployPeerIncomingChannel(remotePeerEvent);
                break;
            case RemotePeerEvent.DISCONNECTED :
                undeployVerticle(remotePeerEvent);
                break;

            case RemotePeerEvent.NEW_PEER :
                deployPeerOutgoingChannel(remotePeerEvent);
                break;

            case RemotePeerEvent.HANDSHAKED :
            	addToReadyPeers(remotePeerEvent);
            	break;
            	
            default :
                LOGGER.warn("{} Event type unrecognized : {}", Topic.REMOTE_PEER_EVENTS, type);
                break;
        }

    }


	private void onRemotePeerMessage(Message<JsonObject> msg) {

        RemotePeerPacket packet = RemotePeerPacket.from(msg.body());

        if (!packet.isValid()) {
            LOGGER.warn("Invalid {} packet : {}", Topic.REMOTE_PEER_INBOX, packet);
        }
        
        String remotePeerId = remotePeerId(packet);

        if (!deployedVerticles.containsKey(remotePeerId)) {
            LOGGER.info("No verticle deployed for {}, waiting..", remotePeerId);
            vertx.setTimer(1000, handler -> onRemotePeerMessage(msg));
        }

        eventBus.send(remotePeerId + "-INBOX", packet.getContent());
    }

    private void deployPeerIncomingChannel(RemotePeerEvent remotePeerEvent) {

        String remoteHostAddress = remotePeerEvent.getRemoteHostAddress();
        int remoteHostPort = remotePeerEvent.getRemoteHostPort();
        PeerIncomingChannel incomingChannel = new PeerIncomingChannel(remoteHostAddress, remoteHostPort);

        Peer peer = peers.computeIfAbsent(remoteHostAddress, ra -> new Peer(ra));
        
        if (peer.getIncomingPort() != 0) {
            LOGGER.warn("An incoming connection already exists for {}", remoteHostAddress);
            return;
        }
        peer.setIncomingPort(remoteHostPort);

        String remotePeerId = remotePeerId(remotePeerEvent);
        deployVerticle(incomingChannel, remotePeerId);
        LOGGER.info("IncomingVerticle deployed for {}", remotePeerId);
    }

    private void deployPeerOutgoingChannel(RemotePeerEvent remotePeerEvent) {
        String remoteHostAddress = remotePeerEvent.getRemoteHostAddress();
        int remoteHostPort = remotePeerEvent.getRemoteHostPort();
        PeerOutgoingChannel outgoing = new PeerOutgoingChannel(remoteHostAddress, remoteHostPort);
        Peer peer = peers.computeIfAbsent(remoteHostAddress, ra -> new Peer(ra));
        peer.setOutgoingPort(remoteHostPort);

        String remotePeerId = remotePeerId(remotePeerEvent);
        deployVerticle(outgoing, remotePeerId);
        LOGGER.info("OutgoingVerticle deployed for {}", remotePeerId);
    }
    
    private void addToReadyPeers(RemotePeerEvent remotePeerEvent) {
        JsonObject content = remotePeerEvent.getContent();

        String remoteHostAddress = remotePeerEvent.getRemoteHostAddress();
        Peer connection = peers.get(remoteHostAddress);
        if (connection == null) {
            LOGGER.error("No PeerConnection found for {}", remoteHostAddress);
            return;
        }

        HandShake handshake = HandShake.fromJson(content);
        // No handshake content for type HANDSHAKE_ACK
        if (handshake.isValid()) {
            // change the remote hostname in the Handshake with the hostname in
            // the
            // RemotePeerEvent object as this is the address this node sees the
            // remote Peer
            Integer port = getPortFromAddress(handshake.getAddrMe());
            handshake.setAddrMe(remotePeerEvent.getRemoteHostAddress() + ":" + port);
            connection.setHandshake(handshake);
        } else {
            LOGGER.warn("Content of {} invalid, content : {}", PeerMessage.HANDSHAKE, content);
        }

	}

    private void deployVerticle(Verticle incomingChannel, String remotePeerId) {
        if (deployedVerticles.containsKey(remotePeerId)) {
            LOGGER.error("A verticle is already deployed for {}", remotePeerId);
            return;
        }

        vertx.deployVerticle(incomingChannel, handler -> {
            if (handler.failed()) {
                LOGGER.error("Deployment of a Verticle {} failed", incomingChannel.getClass().getSimpleName());
                LOGGER.error("Throwable", handler.cause());
                return;
            }
            String id = handler.result();
            deployedVerticles.put(remotePeerId, id);
        });
    }

    private void undeployVerticle(RemotePeerEvent remotePeerEvent) {
        String remotePeerId = remotePeerId(remotePeerEvent);
        if (deployedVerticles.containsKey(remotePeerId)) {
            String id = deployedVerticles.remove(remotePeerId);
            vertx.undeploy(id, handler -> {
                if (!handler.succeeded()) {
                    LOGGER.error("Undeployment of a Verticle for {} failed", remotePeerId);
                } else {
                    LOGGER.info("Verticle for {} undeployed", remotePeerId);
                }
                String previousId = deployedVerticles.get(remotePeerId);
                LOGGER.info("Verticle {} status {}", remotePeerId, previousId);
            });
        }
    }

    private static String getHostnameFromAddress(String address) {
        int indexOfColon = address.indexOf(":");
        return address.substring(0, indexOfColon);
    }

    private static Integer getPortFromAddress(String address) {
        int indexOfColon = address.indexOf(":");
        String portStr = address.substring(indexOfColon + 1);
        try {
            return Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static String remotePeerId(RemotePeerEvent remotePeerEvent) {
        return remotePeerEvent.getRemoteHostAddress() + ":" + remotePeerEvent.getRemoteHostPort();
    }

    private static String remotePeerId(RemotePeerPacket remotePeerPacket) {
        return remotePeerPacket.getRemoteHostAddress() + ":" + remotePeerPacket.getRemoteHostPort();
    }

}
