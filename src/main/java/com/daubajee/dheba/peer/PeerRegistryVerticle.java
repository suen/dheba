package com.daubajee.dheba.peer;

import com.daubajee.dheba.Config;
import com.daubajee.dheba.Topic;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;

public class PeerRegistryVerticle extends AbstractVerticle {

    private Config config;

    private EventBus eventBus;

    @Override
    public void start() throws Exception {
        eventBus = vertx.eventBus();

        config = new Config(vertx);

        vertx.setTimer(1000, this::initPeerList);

    }

    private void initPeerList(Long tick) {

        Integer maxConnections = config.getMaxPeerConnections();
        config.getPeerSeeds()
            .stream()
            .limit(maxConnections)
            .forEach(peer -> {
                String address = peer.getAddress();
                int outgoingPort = peer.getOutgoingPort();
                RemotePeerEvent event = new RemotePeerEvent(address, outgoingPort, RemotePeerEvent.NEW_PEER);
                eventBus.publish(Topic.REMOTE_PEER_EVENTS, event.toJson());
            });
        
    }

}
