package com.daubajee.dheba;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class MainVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start() throws Exception {
        String[] verticles = new String[]{"com.daubajee.dheba.NodeVerticle",
                "com.daubajee.dheba.PeerVerticle", "com.daubajee.dheba.BlockVerticle"};
        for (String verticle : verticles) {
            vertx.deployVerticle(verticle, result -> {
                if (result.succeeded()) {
                    LOGGER.info(verticle + " deployed");
                } else {
                    LOGGER.info(verticle + " failed");
                }
            });
        }
    }

}
