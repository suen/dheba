package com.daubajee.dheba;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.daubajee.dheba.peer.PeerRegistryMessage;
import com.daubajee.dheba.peer.PeerRegistryVerticle;
import com.daubajee.dheba.peer.RemotePeerEvent;
import com.daubajee.dheba.peer.S;
import com.daubajee.dheba.peer.msg.HandShake;
import com.daubajee.dheba.peer.msg.PeerList;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.ObservableHandler;
import io.vertx.rx.java.RxHelper;

@ExtendWith(VertxExtension.class)
public class TestPeerRegistryVerticle {

    List<String> peersAdrPort = Arrays.asList("127.0.0.1:42042", "192.168.1.1:42042", "192.168.1.3:42042",
            "192.168.1.5:42042");

    @Test
    public void testRegistry(Vertx vertx, VertxTestContext testContext) throws Throwable {

        Checkpoint checkpoint = testContext.checkpoint(4);

        PeerRegistryVerticle peerRegistryVerticle = new PeerRegistryVerticle();

        EventBus eventBus = vertx.eventBus();

        ObservableHandler<Message<JsonObject>> remotePeerEventStream = RxHelper.observableHandler();

        ObservableFuture<Message<JsonObject>> peerList1Stream = RxHelper.observableFuture();
        ObservableFuture<Message<JsonObject>> peerList2Stream = RxHelper.observableFuture();

        remotePeerEventStream
            .map(msg -> msg.body())
            .filter(json -> json.getString(S.TYPE, "").equals(RemotePeerEvent.NEW_PEER))
            .subscribe(json -> {
                RemotePeerEvent event = RemotePeerEvent.from(json);
                assertThat("localhost expected", event.getRemoteHostAddress(), equalTo("127.0.0.1"));
                assertThat("a different port was expected", event.getRemoteHostPort(), equalTo(42042));
                checkpoint.flag();

                RemotePeerEvent connectedEvent = new RemotePeerEvent("127.0.0.1", 42042, RemotePeerEvent.HANDSHAKED, new HandShake().toJson());
                eventBus.publish(Topic.REMOTE_PEER_EVENTS, connectedEvent.toJson());
                
                PeerRegistryMessage registryGetListMsg = new PeerRegistryMessage(PeerRegistryMessage.GET_LIST, new JsonObject());
                eventBus.send(Topic.PEER_REGISTRY, registryGetListMsg.toJson(), peerList1Stream.toHandler());
            });

        peerList1Stream
            .take(1)
            .map(msg -> msg.body())
            .filter(json -> json.getString(S.TYPE, "").equals(PeerRegistryMessage.LIST))
            .subscribe(json -> {
                PeerRegistryMessage msg = PeerRegistryMessage.from(json);
                JsonObject content = msg.getContent();
                List<String> peers = PeerList.from(content).getPeers();
                assertThat(peers.size(), equalTo(1));

                String peerAdrPort = peers.get(0);
                assertThat("localhost expected", peerAdrPort, equalTo(peersAdrPort.get(0)));
                checkpoint.flag();
                
                PeerList newPeerList = new PeerList(peersAdrPort);
                PeerRegistryMessage registryListMsg = new PeerRegistryMessage(PeerRegistryMessage.LIST, newPeerList.toJson());
                eventBus.publish(Topic.PEER_REGISTRY, registryListMsg.toJson());
                
                PeerRegistryMessage registryGetListMsg = new PeerRegistryMessage(PeerRegistryMessage.GET_LIST, new JsonObject());
                eventBus.send(Topic.PEER_REGISTRY, registryGetListMsg.toJson(), peerList2Stream.toHandler());
            });
        
        peerList2Stream
            .take(1)
            .map(msg -> msg.body())
            .filter(json -> json.getString(S.TYPE, "").equals(PeerRegistryMessage.LIST))
            .subscribe(json -> {
                PeerRegistryMessage msg = PeerRegistryMessage.from(json);
                JsonObject content = msg.getContent();
                List<String> peers = PeerList.from(content).getPeers();
                assertThat(peers.size(), equalTo(4));
                
                IntStream.range(0, peers.size())
                    .forEach(i -> {
                        String peerAdrStr = peers.get(i);
                        assertThat(peerAdrStr + " =? " + peersAdrPort, peersAdrPort.contains(peerAdrStr), is(true));
                    });
                checkpoint.flag();
            });

        eventBus.consumer(Topic.REMOTE_PEER_EVENTS, remotePeerEventStream.toHandler());

        System.setProperty(Config.P_P2P_SEEDS, "127.0.0.1:42042");
        vertx.deployVerticle(peerRegistryVerticle, testContext.succeeding(h -> {
            checkpoint.flag();
        }));


    }
}
