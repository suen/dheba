package com.daubajee.dheba;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.daubajee.dheba.peer.MessengerVerticle;
import com.daubajee.dheba.peer.PeerManagerVerticle;
import com.daubajee.dheba.peer.RemotePeerEvent;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class TestPeerManagerVerticles {

    @BeforeEach
    public void setup(Vertx vertx, VertxTestContext testContext) throws Throwable {

        Checkpoint verticleAcheck = testContext.checkpoint();
        Checkpoint verticleBcheck = testContext.checkpoint();
        Checkpoint verticleCcheck = testContext.checkpoint();

        System.setProperty(Config.P_P2P_PORT, "42041");
        Config configA = new Config();
        MessengerVerticle messengerVerticleA = new MessengerVerticle(configA);

        System.setProperty(Config.P_P2P_PORT, "42042");
        Config configB = new Config();
        MessengerVerticle messengerVerticleB = new MessengerVerticle(configB);

        PeerManagerVerticle peerManagerVerticle = new PeerManagerVerticle();

        vertx.deployVerticle(messengerVerticleA, testContext.succeeding(handler -> {
            verticleAcheck.flag();

            vertx.deployVerticle(messengerVerticleB, testContext.succeeding(h -> {
                verticleBcheck.flag();
            }));

            vertx.deployVerticle(peerManagerVerticle, testContext.succeeding(h -> {
                verticleCcheck.flag();
            }));
        }));

        testContext.awaitCompletion(5, TimeUnit.SECONDS);
        if (testContext.failed()) {
            throw testContext.causeOfFailure();
        }
    }

    @Test
    public void test(Vertx vertx, VertxTestContext testContext) throws Throwable {
        Checkpoint strictCheckpoint = testContext.strictCheckpoint();

        EventBus eventBus = vertx.eventBus();

        RemotePeerEvent event = new RemotePeerEvent("localhost", 42042, RemotePeerEvent.NEW_PEER);

        eventBus.publish(Topic.REMOTE_PEER_EVENTS, event.toJson());

        vertx.timerStream(5000).handler(h -> {
            strictCheckpoint.flag();
        });

    }

}
