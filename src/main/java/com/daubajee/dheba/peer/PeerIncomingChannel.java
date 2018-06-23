package com.daubajee.dheba.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.daubajee.dheba.Config;
import com.daubajee.dheba.Topic;
import com.daubajee.dheba.peer.msg.HandShake;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;

public class PeerIncomingChannel extends AbstractVerticle {

    private final String remoteHostAddress;

    private final int remoteHostPort;

    private EventBus eventBus;

    private final Logger LOGGER;

    private MessageConsumer<JsonObject> consumer;

    private State currentState = State.INIT; 
    
    public PeerIncomingChannel(String remoteHostAddress, int remoteHostPort) {
        this.remoteHostAddress = remoteHostAddress;
        this.remoteHostPort = remoteHostPort;
        LOGGER = LoggerFactory
                .getLogger(PeerIncomingChannel.class.getSimpleName() + "-" + remoteHostAddress + ":" + remoteHostPort);
    }

    @Override
    public void start() throws Exception {

        eventBus = vertx.eventBus();

        consumer = eventBus.consumer(getRemotePeerInboxTopic(), this::onInboxMessage);
        
        currentState = State.WAIT_HANDSHAKE; 
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
        LOGGER.info("Message content : {}", peerMsg.getContent());
		switch(type) {
			case PeerMessage.HANDSHAKE:
				onHandshake(peerMsg.getContent());
				break;
			default:
				LOGGER.info("Unrecognized Type {}", type);
				break;
		}
        
    }
    
    private void onHandshake(JsonObject content) {
		if (currentState.equals(State.WAIT_HANDSHAKE)) {
			currentState = State.READY;
        } else {
            LOGGER.info("Handshake message already processed");
            return;
		}
		
		if (!HandShake.fromJson(content).isValid()) {
		    LOGGER.warn("Invalid {} message received : {}", PeerMessage.HANDSHAKE, content);
            return;
		}
		
        Config config = new Config(vertx);
        String selfname = config.getHostname();
        int selfport = config.getP2PPort();		
        JsonObject handshakeMsgContent = PeerUtils.createHandShakeMessage(remoteHostAddress, remoteHostPort, selfname, selfport);

        PeerMessage handshakeAckMsg = new PeerMessage(PeerMessage.HANDSHAKE, handshakeMsgContent);
		RemotePeerPacket handshakeActPacket = new RemotePeerPacket(remoteHostAddress, remoteHostPort, handshakeAckMsg.toJson());

		eventBus.publish(Topic.REMOTE_PEER_OUTBOX, handshakeActPacket.toJson());
		
		RemotePeerEvent event = new RemotePeerEvent(remoteHostAddress, remoteHostPort, RemotePeerEvent.HANDSHAKED, content);
		eventBus.publish(Topic.REMOTE_PEER_EVENTS, event.toJson());
	}

	private static enum State {
    	
    	INIT,
    	
    	WAIT_HANDSHAKE,
    	
    	READY
    	
    }

}
