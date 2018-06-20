package com.daubajee.dheba.peer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.daubajee.dheba.Topic;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class PeerManagerVerticle extends AbstractVerticle {

    private EventBus eventBus;

    private final Logger LOGGER = LoggerFactory.getLogger(PeerManagerVerticle.class);

    private Map<String, String> deployedVerticles = new ConcurrentHashMap<>();

    @Override
    public void start() throws Exception {

        eventBus = vertx.eventBus();

        eventBus.consumer(Topic.REMOTE_PEER_EVENTS, this::onRemotePeerEvent);

        eventBus.consumer(Topic.REMOTE_PEER_INBOX, this::onRemotePeerMessage);

    }

    private void onRemotePeerEvent(Message<JsonObject> msg) {
        JsonObject body = msg.body();

        RemotePeerEvent remotePeerEvent = RemotePeerEvent.from(body);

        if (!remotePeerEvent.isValid()) {
            LOGGER.warn("Invalid {} event : {}", Topic.REMOTE_PEER_EVENTS, remotePeerEvent);
            return;
        }

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
            vertx.timerStream(1000).handler(handler -> onRemotePeerMessage(msg));
        }

        eventBus.send(remotePeerId + "-INBOX", msg.body());
    }

    private void deployPeerIncomingChannel(RemotePeerEvent remotePeerEvent) {

        PeerIncomingChannel incomingChannel = new PeerIncomingChannel(remotePeerEvent.getRemoteHostAddress(), remotePeerEvent.getRemoteHostPort());

        deployVerticle(incomingChannel, remotePeerId(remotePeerEvent));
    }

    private void deployPeerOutgoingChannel(RemotePeerEvent remotePeerEvent) {
        PeerOutgoingChannel outgoing = new PeerOutgoingChannel(remotePeerEvent.getRemoteHostAddress(),
                remotePeerEvent.getRemoteHostPort());

        deployVerticle(outgoing, remotePeerId(remotePeerEvent));
    }

    private void deployVerticle(Verticle incomingChannel, String remotePeerId) {
        vertx.deployVerticle(incomingChannel, handler -> {
            if (handler.failed()) {
                LOGGER.error("Deployment of a Verticle {} failed", incomingChannel.getClass().getSimpleName());
            }
            String id = handler.result();
            deployedVerticles.put(remotePeerId, id);
        });
    }

    private void undeployVerticle(RemotePeerEvent remotePeerEvent) {
        String remotePeerId = remotePeerId(remotePeerEvent);
        if (deployedVerticles.containsKey(remotePeerId)) {
            String id = deployedVerticles.remove(remotePeerId);
            vertx.undeploy(id);
        }
    }

    private static String remotePeerId(RemotePeerEvent remotePeerEvent) {
        return remotePeerEvent.getRemoteHostAddress() + ":" + remotePeerEvent.getRemoteHostPort();
    }

    private static String remotePeerId(RemotePeerPacket remotePeerPacket) {
        return remotePeerPacket.getRemoteHostAddress() + ":" + remotePeerPacket.getRemoteHostPort();
    }

}
