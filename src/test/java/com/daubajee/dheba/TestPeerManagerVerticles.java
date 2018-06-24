package com.daubajee.dheba;

import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.daubajee.dheba.peer.PeerListManagerVerticle;
import com.daubajee.dheba.peer.PeerManagerVerticle;
import com.daubajee.dheba.peer.PeerMessage;
import com.daubajee.dheba.peer.RemotePeerEvent;
import com.daubajee.dheba.peer.RemotePeerPacket;
import com.daubajee.dheba.peer.S;
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

        Checkpoint managerVerticleCheck = testContext.checkpoint();
        
        PeerManagerVerticle peerManagerVerticle = new PeerManagerVerticle();
        
        vertx.deployVerticle(peerManagerVerticle, testContext.succeeding(h -> {
            managerVerticleCheck.flag();
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

        eventBus.consumer(Topic.REMOTE_PEER_OUTBOX, handler -> {

    		JsonObject body = (JsonObject)handler.body();
    		JsonObject packetContent = body.getJsonObject("content", new JsonObject());
    		String msgType = packetContent.getString("type", "");

    		assertThat("A " + PeerMessage.HANDSHAKE + " was expected", msgType, CoreMatchers.equalTo(PeerMessage.HANDSHAKE));
        	
            HandShake handShake = new HandShake();
            handShake.setAddrMe("localhost:42041");
            handShake.setAddrYou("localhost:42042");
            handShake.setAgent("Agent 1");
            handShake.setServices("SERVICES BETA");
            handShake.setVersion("0.1");
            handShake.setTimestamp(System.currentTimeMillis());
            handShake.setBestHeight(1);

            JsonObject handshakeAckContent = handShake.toJson();

            PeerMessage handshakeAckMsg = new PeerMessage(PeerMessage.HANDSHAKE, handshakeAckContent);
        	RemotePeerPacket handshakeAckPacket = new RemotePeerPacket("localhost", 42042, handshakeAckMsg.toJson());
        	eventBus.publish(Topic.REMOTE_PEER_INBOX, handshakeAckPacket.toJson());
       	
        	strictCheckpoint.flag();
        });
        
        System.setProperty(Config.P_P2P_SEEDS, "localhost:42042");
        PeerListManagerVerticle peerListManagerVerticle = new PeerListManagerVerticle();
        vertx.deployVerticle(peerListManagerVerticle, testContext.succeeding());

    }
    
    @Test
    public void testIncomingHandshake(Vertx vertx, VertxTestContext testContext) throws Throwable {
        Checkpoint strictCheckpoint = testContext.strictCheckpoint(2);
    	
    	EventBus eventBus = vertx.eventBus();
    	
    	String remoteHostAddress = "localhost";
        int remoteOutgoingHostPort = 51200;
        int remoteIncomingHostPort = 51241;
		
        HandShake handShake = new HandShake();
        handShake.setAddrMe("localhost:" + remoteIncomingHostPort);
        handShake.setAddrYou("localhost:42042");
        handShake.setAgent("Agent 1");
        handShake.setServices("SERVICES BETA");
        handShake.setVersion("0.1");
        handShake.setTimestamp(System.currentTimeMillis());
        handShake.setBestHeight(1);

        AtomicBoolean flip = new AtomicBoolean(false);
    	eventBus.consumer(Topic.REMOTE_PEER_OUTBOX, handler -> {
            JsonObject body = (JsonObject) handler.body();
            JsonObject packetContent = body.getJsonObject("content", new JsonObject());
            String msgType = packetContent.getString("type", "");
            String packetHost = body.getString(S.REMOTE_HOST_ADDRESS);
            int packetPort = body.getInteger(S.REMOTE_HOST_PORT);

            assertThat("A " + PeerMessage.HANDSHAKE + " was expected", msgType,
                    CoreMatchers.equalTo(PeerMessage.HANDSHAKE));
            if (!flip.getAndSet(true)) {
                assertThat("Unexpected host", packetHost, CoreMatchers.equalTo(remoteHostAddress));
                assertThat("Unexpected port", packetPort, CoreMatchers.equalTo(remoteOutgoingHostPort));
                strictCheckpoint.flag();
            }
            else {
                assertThat("Unexpected host", packetHost, CoreMatchers.equalTo(remoteHostAddress));
                assertThat("Unexpected port", packetPort, CoreMatchers.equalTo(remoteIncomingHostPort));
                strictCheckpoint.flag();
            }

    	});
    	
    	RemotePeerEvent event = new RemotePeerEvent(remoteHostAddress, remoteOutgoingHostPort, RemotePeerEvent.CONNECTED);
    	eventBus.publish(Topic.REMOTE_PEER_EVENTS, event.toJson());

        JsonObject handshakeContent = handShake.toJson();
    	PeerMessage handshakeMsg = new PeerMessage(PeerMessage.HANDSHAKE, handshakeContent);
    	RemotePeerPacket handshakePacket = new RemotePeerPacket(remoteHostAddress, remoteOutgoingHostPort, handshakeMsg.toJson());

    	eventBus.publish(Topic.REMOTE_PEER_INBOX, handshakePacket.toJson());
    	
    }

}
