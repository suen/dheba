package com.daubajee.dheba.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public PeerIncomingChannel(String remoteHostAddress, int remoteHostPort) {
        this.remoteHostAddress = remoteHostAddress;
        this.remoteHostPort = remoteHostPort;
        LOGGER = LoggerFactory
                .getLogger(PeerOutgoingChannel.class.getSimpleName() + "-" + remoteHostAddress + ":" + remoteHostPort);
    }

    @Override
    public void start() throws Exception {

        eventBus = vertx.eventBus();

        consumer = eventBus.consumer(getRemotePeerInboxTopic(), this::onInboxMessage);
    }

    @Override
    public void stop() throws Exception {
        consumer.unregister();
    }

    private String getRemotePeerInboxTopic() {
        return String.format("%s:%d-INBOX", remoteHostAddress, remoteHostPort);
    }

    private void onInboxMessage(Message<JsonObject> msg) {
        LOGGER.info("Msg received {}", msg.body());
    }

}
