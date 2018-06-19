package com.daubajee.dheba;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.daubajee.dheba.peer.MessengerVerticle;
import com.daubajee.dheba.peer.RemotePeerPacket;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class TestMessengerVerticle {

	@Test
	public void test() throws Throwable {
		VertxTestContext testContext = new VertxTestContext();
		
		Checkpoint verticleAcheck = testContext.checkpoint();
		Checkpoint verticleBcheck = testContext.checkpoint();
		Checkpoint msgRevCheck = testContext.checkpoint(2);
		
		Vertx vertx = Vertx.vertx();
		EventBus eventBus = vertx.eventBus();


		Config config = Config.instance();
		System.setProperty(Config.P_P2P_PORT, "42041");
		MessengerVerticle messengerVerticleA = new MessengerVerticle(config);
		MessengerVerticle messengerVerticleB = new MessengerVerticle(config);

		vertx.deployVerticle(messengerVerticleA, testContext.succeeding(handler -> {
			verticleAcheck.flag();
			System.setProperty(Config.P_P2P_PORT, "42042");
			vertx.deployVerticle(messengerVerticleB, testContext.succeeding(h -> {
				verticleBcheck.flag();
				
				RemotePeerPacket packet = new RemotePeerPacket("localhost", 42042, new JsonObject().put("TYPE", "TEST"));
				eventBus.publish(Topic.REMOTE_PEER_OUTBOX, packet.toJson());
			}));
		}));
		
		eventBus.consumer(Topic.REMOTE_PEER_INBOX, handler -> {
			assertTrue(handler.body() != null);
			msgRevCheck.flag();
		});
		
		eventBus.consumer(Topic.REMOTE_PEER_EVENTS, handler -> {
			assertTrue(handler.body() != null);
			msgRevCheck.flag();
		});
		
		assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
		if (testContext.failed()) {
			throw testContext.causeOfFailure();
		}
	}
}
