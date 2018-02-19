package com.daubajee.dheba.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class PeerServerHandlerVerticle extends AbstractVerticle {

    private final String remotePeerAddress;

    private final int remotePeerPort;

    private EventBus eventBus;

    private static final Logger LOGGER = LoggerFactory.getLogger(PeerServerHandlerVerticle.class);

    public PeerServerHandlerVerticle(String remotePeerAddress, int remotePeerPort) {
        this.remotePeerAddress = remotePeerAddress;
        this.remotePeerPort = remotePeerPort;
    }

    public String getRemotePeerIdentifier() {
        return remotePeerAddress + ":" + remotePeerPort;
    }

    private String getRemoteHandlerAddress() {
        return "handler-" + getRemotePeerIdentifier();
    }

    public String getAdminAddress() {
        return "handler-admin-" + getRemotePeerIdentifier();
    }

    @Override
    public void start() throws Exception {
        eventBus = vertx.eventBus();

        eventBus.consumer(getRemoteHandlerAddress(), this::remoteMsgDispatcher);

        eventBus.consumer(getAdminAddress(), this::internalCmdDispatcher);

    }

    public void remoteMsgDispatcher(Message<JsonObject> message) {
        JsonObject body = message.body();
        if (body.isEmpty()) {
            log("Empty content ");
            return;
        }

        String type = body.getString(S.TYPE, "");
        JsonObject content = body.getJsonObject(S.CONTENT, new JsonObject());
        switch (type) {
            case "HANDSHAKE" :
                sendToRemote(new JsonObject().put(S.TYPE, "HANDSHAKE_ACK"));
                break;
            default :
                log("unknown type : '" + type + "'");
                break;
        }
    }

    public void internalCmdDispatcher(Message<JsonObject> message) {

    }

    private void log(String msg) {
        LOGGER.info("[{}] - {}", getRemotePeerIdentifier(), msg);
    }

    private void sendToRemote(JsonObject content) {
        JsonObject remotePeerMsgPacket = getEnvelope().put(S.MESSAGE, content);
        eventBus.publish("REMOTE_PEER_SEND", remotePeerMsgPacket);
    }

    public JsonObject getEnvelope() {
        return new JsonObject().put(S.REMOTE_HOST, remotePeerAddress).put(S.REMOTE_PORT, remotePeerPort);
    }

}
