package com.daubajee.dheba;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.auth.shiro.ShiroAuthOptions;
import io.vertx.ext.auth.shiro.ShiroAuthRealmType;
import io.vertx.ext.shell.ShellService;
import io.vertx.ext.shell.ShellServiceOptions;
import io.vertx.ext.shell.term.SSHTermOptions;

public class MainVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start() throws Exception {
        String[] verticles = new String[]{"com.daubajee.dheba.NodeVerticle",
                "com.daubajee.dheba.peer.PeerVerticle", "com.daubajee.dheba.BlockVerticle"};
        for (String verticle : verticles) {
            vertx.deployVerticle(verticle, result -> {
                if (result.succeeded()) {
                    LOGGER.info(verticle + " deployed");
                } else {
                    LOGGER.info(verticle + " failed");
                }
            });
        }
        
        ShellService service = ShellService.create(vertx,
                new ShellServiceOptions().setSSHOptions(
                    new SSHTermOptions().
                        setHost("localhost").
                        setPort(5000).
                        setKeyPairOptions(new JksOptions().
                                setPath("/home/surendra/keystore.jks").
                                setPassword("freeworld")
                        ).
                        setAuthOptions(new ShiroAuthOptions().
                                setType(ShiroAuthRealmType.PROPERTIES).
                                setConfig(new JsonObject().
                                    put("properties_path", "file:auth.properties"))
                        )
                )
            );
        service.start();
        
    }

}
