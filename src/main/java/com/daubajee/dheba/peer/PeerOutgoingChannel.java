package com.daubajee.dheba.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.daubajee.dheba.Topic;
import com.daubajee.dheba.peer.msg.HandShake;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;

public class PeerOutgoingChannel extends AbstractVerticle {

    private final String remoteHostAddress;

    private final int remoteHostPort;

    private EventBus eventBus;

    private final Logger LOGGER;

    private MessageConsumer<JsonObject> consumer;

    private State currentState = State.INIT;
    
    public PeerOutgoingChannel(String remoteHostAddress, int remoteHostPort) {
        this.remoteHostAddress = remoteHostAddress;
        this.remoteHostPort = remoteHostPort;
        LOGGER = LoggerFactory
                .getLogger(PeerOutgoingChannel.class.getSimpleName() + "-" + remoteHostAddress + ":" + remoteHostPort);
    }

    @Override
    public void start() throws Exception {

        eventBus = vertx.eventBus();

        consumer = eventBus.consumer(getRemotePeerInboxTopic(), this::onInboxMessage);

        vertx.timerStream(1000).handler(handler -> {
            JsonObject handshakeMsgContent = createHandShakeMessage(remoteHostAddress, remoteHostPort, "localhost", 8080);
            PeerMessage handshakeMsg = new PeerMessage(PeerMessage.HANDSHAKE, handshakeMsgContent);
            RemotePeerPacket packet = new RemotePeerPacket(remoteHostAddress, remoteHostPort, handshakeMsg.toJson());
            eventBus.send(Topic.REMOTE_PEER_OUTBOX, packet.toJson());
            LOGGER.info("Handshake Msg send to {}:{}", remoteHostAddress, remoteHostPort);
            currentState = State.WAIT_HANDSHAKE_ACK;
        });
    }

    @Override
    public void stop() throws Exception {
        consumer.unregister();
    }

    private String getRemotePeerInboxTopic() {
        return String.format("%s:%d-INBOX", remoteHostAddress, remoteHostPort);
    }

    private void onInboxMessage(Message<JsonObject> msg) {
    	JsonObject body = msg.body();
        PeerMessage peerMsg = PeerMessage.from(body);
        String type = peerMsg.getType();
        
        LOGGER.info("Message of type {} received on {}", type, getRemotePeerInboxTopic());
        switch (type) {
			case PeerMessage.HANDSHAKE_ACK:
				onHandShakeAck(peerMsg.getContent());
				break;
	
			default:
				LOGGER.warn("Unrecognized type {}", peerMsg.toJson());
				break;
		}
    }

    private void onHandShakeAck(JsonObject content) {
		if (currentState == State.WAIT_HANDSHAKE_ACK) {
			currentState = State.READY;
			
			RemotePeerEvent event = new RemotePeerEvent(remoteHostAddress, remoteHostPort, RemotePeerEvent.HANDSHAKED);
			eventBus.publish(Topic.REMOTE_PEER_EVENTS, event.toJson());
		}
		else {
			LOGGER.warn(PeerMessage.HANDSHAKE_ACK + " already received");
		}
	}

	private JsonObject createHandShakeMessage(String remoteHost, Integer remotePort, String selfHost,
            Integer selfPort) {
        HandShake handShake = new HandShake();
        handShake.setAddrMe(selfHost + ":" + selfPort);
        handShake.setAddrYou(remoteHost + ":" + remotePort);
        handShake.setAgent("dheba 0.1");
        handShake.setBestHeight(1);
        handShake.setServices("NODE BETA ALPHA");
        handShake.setTimestamp(System.currentTimeMillis());
        handShake.setVersion("0.1");
        return handShake.toJson();
    }
    
	private static enum State {
    	
    	INIT,
    	
    	WAIT_HANDSHAKE_ACK,
    	
    	READY
    	
    }
}
