package com.daubajee.dheba;

import java.util.List;
import java.util.stream.Collectors;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

@Component
@SpringVerticle(springConfig = BeanConfiguration.class)
public class NodeVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeVerticle.class);

    private Blocks blocks;

    @Autowired
    public void setBlockChain(Blocks blockChain) {
        this.blocks = blockChain;
    }

    @Override
    public void start() throws Exception {

        HttpServer server = vertx.createHttpServer();

        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());

        router.route().method(HttpMethod.GET).path("/blocks").handler(this::handleGetBlock);

        router.route().method(HttpMethod.POST).path("/mineBlock").handler(this::handleMineBlock);

        router.route().method(HttpMethod.GET).path("/peers").handler(this::handlePeers);

        router.route().method(HttpMethod.POST).path("/addPeer").handler(this::handleAddPeer);

        server.requestHandler(router::accept);
        server.listen(42042);
        LOGGER.info("Node listening at 42042");
    }

    private void handleGetBlock(RoutingContext cxt) {
        List<Block> blockchain = blocks.getBlockchain();
        List<JsonObject> jsonBlocks = blockchain.stream()
            .map(block -> block.toJson())
            .collect(Collectors.toList());
        JsonArray jsonArray = new JsonArray(jsonBlocks);
        String jsonArrayStr = jsonArray.toString();
        
        HttpServerResponse response = cxt.response();
        response.putHeader("Content-Type", "application/json");
        response.putHeader("Content-Length", String.valueOf(jsonArrayStr.length()));
        response.write(jsonArrayStr);
    }

    private void handleMineBlock(RoutingContext cxt) {
        HttpServerRequest request = cxt.request();
        String data = cxt.getBody().toString();
        List<Block> blockchain = blocks.getBlockchain();
        Block newBlock = blocks.generateNewBlock(data, blockchain.get(blockchain.size() - 1));
    }

    private void handlePeers(RoutingContext cxt) {

    }

    private void handleAddPeer(RoutingContext cxt) {

    }

}
