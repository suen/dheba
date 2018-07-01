package com.daubajee.dheba;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.daubajee.dheba.peer.PeerRegistryMessage;
import com.daubajee.dheba.peer.msg.GetPeerList;
import com.daubajee.dheba.peer.msg.PeerList;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class NodeVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeVerticle.class);

    private EventBus eventBus;

    @Override
    public void start() throws Exception {

        eventBus = vertx.eventBus();

        HttpServer server = vertx.createHttpServer();

        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());

        router.route().method(HttpMethod.GET).path("/blocks").handler(this::handleGetBlock);

        router.route().method(HttpMethod.POST).path("/mineBlock").handler(this::handleMineBlock);

        router.route().method(HttpMethod.GET).path("/peers").handler(this::handlePeers);

        router.route().method(HttpMethod.POST).path("/addPeer").handler(this::handleAddPeer);

        server.requestHandler(router::accept);
        int httpPort = new Config(vertx).getHttpPort();
        server.listen(httpPort, handler -> {
            if (handler.failed()) {
                LOGGER.error("Failed binding on " + httpPort);
            } else {
                LOGGER.info("HTTP listening on " + httpPort);
            }
        });
    }

    private void handleGetBlock(RoutingContext cxt) {
        JsonObject requestObj = MsgUtils.createRequest("");
        HttpServerResponse response = cxt.response();

        eventBus.send("BLOCK", requestObj, result -> {
            if (result.succeeded()) {
                JsonObject reply = (JsonObject) result.result().body();
                String replyMsgString = MsgUtils.getReplyMsgString(reply);

                response.putHeader("Content-Type", "application/json");
                response.putHeader("Content-Length", String.valueOf(replyMsgString.length()));
                response.write(replyMsgString);
            } else {
                response.setStatusCode(500);
            }
            response.close();
        });
    }

    private void handleMineBlock(RoutingContext cxt) {
        String data = cxt.getBody().toString();
        HttpServerResponse response = cxt.response();

        JsonObject request = MsgUtils.createRequest("", data);

        DeliveryOptions options = MsgUtils.deliveryOpWithTimeout(60000);

        eventBus.send("BLOCK", request, callback -> {
            if (callback.succeeded()) {
                JsonObject reply = (JsonObject) callback.result().body();
                String status = MsgUtils.getStatus(reply);
                if ("OK".equals(status)) {
                    response.setStatusCode(204);
                    response.close();
                    return;
                }
            }
            response.setStatusCode(500);
            response.close();
        });
    }

    private void handlePeers(RoutingContext cxt) {

        GetPeerList getPeerList = new GetPeerList(10000, Collections.emptyList());
        PeerRegistryMessage peerListMsg = new PeerRegistryMessage(PeerRegistryMessage.GET_LIST, getPeerList.toJson());
        
        eventBus.send(Topic.PEER_REGISTRY, peerListMsg.toJson(), handler -> {
            HttpServerResponse response = cxt.response();
            if (handler.failed()) {
                String msg = handler.cause().getMessage();
                response.putHeader("Content-Length", String.valueOf(msg.length()));
                response.write(msg);
                response.setStatusCode(500);
                response.end();
                return;
            }
            Message<Object> result = handler.result();
            JsonObject registMsgJson = (JsonObject) result.body();
            PeerRegistryMessage registryMsg = PeerRegistryMessage.from(registMsgJson);
            PeerList peerList = PeerList.from(registryMsg.getContent());
            String peersStr = new JsonArray(peerList.getPeers()).toString();
            response.putHeader("Content-Length", String.valueOf(peersStr.length()));
            response.putHeader("Content-Type", "application/json");
            response.write(peersStr);
            response.end();
        });

    }

    private void handleAddPeer(RoutingContext cxt) {

    }

}
