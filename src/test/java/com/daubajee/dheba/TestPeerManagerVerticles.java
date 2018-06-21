package com.daubajee.dheba;

import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeUnit;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.daubajee.dheba.peer.PeerManagerVerticle;
import com.daubajee.dheba.peer.PeerMessage;
import com.daubajee.dheba.peer.RemotePeerEvent;
import com.daubajee.dheba.peer.RemotePeerPacket;
import com.daubajee.dheba.peer.msg.HandShake;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class TestPeerManagerVerticles {

    @BeforeEach
    public void setup(Vertx vertx, VertxTestContext testContext) throws Throwable {

        Checkpoint verticleCcheck = testContext.checkpoint();
        
        PeerManagerVerticle peerManagerVerticle = new PeerManagerVerticle();
        
        vertx.deployVerticle(peerManagerVerticle, testContext.succeeding(h -> {
        	verticleCcheck.flag();
        }));

        testContext.awaitCompletion(5, TimeUnit.SECONDS);
        if (testContext.failed()) {
            throw testContext.causeOfFailure();
        }
    }

    @Test
    public void testOutgoingHandshake(Vertx vertx, VertxTestContext testContext) throws Throwable {
        Checkpoint strictCheckpoint = testContext.strictCheckpoint();

        EventBus eventBus = vertx.eventBus();

        RemotePeerEvent event = new RemotePeerEvent("localhost", 42042, RemotePeerEvent.NEW_PEER);

        eventBus.consumer(Topic.REMOTE_PEER_OUTBOX, handler -> {

    		JsonObject body = (JsonObject)handler.body();
    		JsonObject packetContent = body.getJsonObject("content", new JsonObject());
    		String msgType = packetContent.getString("type", "");

    		assertThat("A " + PeerMessage.HANDSHAKE + " was expected", msgType, CoreMatchers.equalTo(PeerMessage.HANDSHAKE));
        	
        	PeerMessage handshakeAckMsg = new PeerMessage(PeerMessage.HANDSHAKE_ACK, new JsonObject());
        	RemotePeerPacket handshakeAckPacket = new RemotePeerPacket("localhost", 42042, handshakeAckMsg.toJson());
        	eventBus.publish(Topic.REMOTE_PEER_INBOX, handshakeAckPacket.toJson());
       	
        	strictCheckpoint.flag();
        });
        
        eventBus.publish(Topic.REMOTE_PEER_EVENTS, event.toJson());
    }
    
    @Test
    public void testIncomingHandshake(Vertx vertx, VertxTestContext testContext) throws Throwable {
    	Checkpoint strictCheckpoint = testContext.strictCheckpoint();
    	
    	EventBus eventBus = vertx.eventBus();
    	
    	String remoteHostAddress = "localhost";
		int remoteHostPort = 42042;
		
    	
    	eventBus.consumer(Topic.REMOTE_PEER_OUTBOX, handler -> {
    		
    		JsonObject body = (JsonObject)handler.body();
    		JsonObject packetContent = body.getJsonObject("content", new JsonObject());
    		String msgType = packetContent.getString("type", "");
    		assertThat("A " + PeerMessage.HANDSHAKE_ACK + " was expected", msgType, CoreMatchers.equalTo(PeerMessage.HANDSHAKE_ACK));
    		strictCheckpoint.flag();
    	});
    	
    	RemotePeerEvent event = new RemotePeerEvent(remoteHostAddress, remoteHostPort, RemotePeerEvent.CONNECTED);
    	eventBus.publish(Topic.REMOTE_PEER_EVENTS, event.toJson());
    	
    	JsonObject handshakeContent = new HandShake().toJson();
    	PeerMessage handshakeMsg = new PeerMessage(PeerMessage.HANDSHAKE, handshakeContent);
    	RemotePeerPacket handshakePacket = new RemotePeerPacket(remoteHostAddress, remoteHostPort, handshakeMsg.toJson());

    	eventBus.publish(Topic.REMOTE_PEER_INBOX, handshakePacket.toJson());
    	
    }

}
