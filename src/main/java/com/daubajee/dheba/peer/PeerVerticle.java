package com.daubajee.dheba.peer;

import com.daubajee.dheba.peer.msg.HandShake;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class PeerVerticle extends AbstractVerticle {

    public static final String NAME = PeerVerticle.class.getSimpleName();

    public static final String ADD_NEW_PEER = "ADD_NEW_PEER";

    public static final String NEW_PEER_CONNECTED = "NEW_PEER_CONNECTED";

    public static final String PEER_MSG = "PEER_MSG";

    private EventBus eventBus;

    private static final Logger LOGGER = LoggerFactory.getLogger(PeerVerticle.class);

    @Override
    public void start() throws Exception {
        vertx.deployVerticle(new RemotePeerVerticle());

        eventBus = vertx.eventBus();

        eventBus.consumer(ADD_NEW_PEER, this::onAddNewPeer);

        eventBus.consumer(NAME, this::onMessage);

    }

    private void onMessage(Message<JsonObject> msg) {
        JsonObject body = msg.body();
        String type = body.getString(S.TYPE, "");
        if (type.isEmpty()) {
            return;
        }
        switch (type) {
            case NEW_PEER_CONNECTED :
                onNewPeerConnected(msg);
                break;
            case PEER_MSG :
                onPeerMsg(msg);
                break;
            default :
                break;
        }
    }

    private void cron() {
        vertx.periodicStream(1000).handler(handler -> {
            long longValue = System.currentTimeMillis();
            System.out.println("Server sending long value :" + longValue);
            eventBus.publish("P2P_OUT", new JsonObject().put("long", longValue));
        });

        eventBus.consumer("P2P_IN", handler -> {
            System.out.println("Server Received : " + handler.body().toString());
        });
    }

    private void onAddNewPeer(Message<Object> msg) {
        Object bodyObj = msg.body();
        JsonObject body;
        if (bodyObj instanceof String) {
            String[] parts = bodyObj.toString().split(":");
            body = new JsonObject()
                .put(S.REMOTE_HOST, parts[0])
                .put(S.REMOTE_PORT, Integer.parseInt(parts[1]));
        } else if (bodyObj instanceof JsonObject) {
            body = (JsonObject) bodyObj;
        } else {
            LOGGER.error("Bad request : ", bodyObj);
            return;
        }

        String remoteHost = body.getString(S.REMOTE_HOST, "");
        Integer remotePort = body.getInteger(S.REMOTE_PORT, 0);
        if (remoteHost.isEmpty() || remotePort == 0) {
            LOGGER.warn("Bad message ", msg);
            return;
        }
        JsonObject peerConnectMsg = body.copy();
        peerConnectMsg.put(S.TYPE, RemotePeerVerticle.PEER_CONNECT);

        eventBus.publish(RemotePeerVerticle.NAME, peerConnectMsg);
    }

    private void onNewPeerConnected(Message<JsonObject> msg) {
        JsonObject body = msg.body();
        String remoteHost = body.getString(S.REMOTE_HOST, "");
        Integer remotePort = body.getInteger(S.REMOTE_PORT, 0);
        if (remoteHost.isEmpty() || remotePort == 0) {
            LOGGER.warn("Bad message ", msg);
            return;
        }

        JsonObject handShakeMsg = createHandShakeMessage(remoteHost, remotePort);
        JsonObject peerMsg = createPeerSendMsg(remoteHost, remotePort, handShakeMsg);
        eventBus.publish(RemotePeerVerticle.NAME, peerMsg);
    }

    private void onPeerMsg(Message<JsonObject> msg) {
        JsonObject body = msg.body();
        LOGGER.info("Received PEER_MSG : " + body.toString());
    }

    private JsonObject createPeerSendMsg(String remoteHost, Integer remotePort, JsonObject handShakeMsg) {
        return new JsonObject()
                .put(S.REMOTE_HOST, remoteHost)
                .put(S.REMOTE_PORT, remotePort)
                .put(S.TYPE, RemotePeerVerticle.PEER_SEND)
                .put(S.MESSAGE, handShakeMsg);
    }

    private JsonObject createHandShakeMessage(String remoteHost, Integer remotePort) {
        HandShake handShake = new HandShake();
        handShake.setAddrMe("0.0.0.0:7000");
        handShake.setAddrYou(remoteHost + ":" + remotePort);
        handShake.setAgent("dheba 0.1");
        handShake.setBestHeight(1);
        handShake.setServices("NODE BETA ALPHA");
        handShake.setTimestamp(System.currentTimeMillis());
        handShake.setVersion("0.42");
        return handShake.toJson();
    }

}
