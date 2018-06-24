package com.daubajee.dheba;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.daubajee.dheba.peer.MessengerVerticle;
import com.daubajee.dheba.peer.PeerListManagerVerticle;
import com.daubajee.dheba.peer.PeerManagerVerticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
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
        String[] verticles = new String[]{
                PeerManagerVerticle.class.getCanonicalName(),
                MessengerVerticle.class.getCanonicalName(),
                PeerListManagerVerticle.class.getCanonicalName(),
                NodeVerticle.class.getCanonicalName(),
                BlockVerticle.class.getCanonicalName()};
        
        for (String verticle : verticles) {
            vertx.deployVerticle(verticle, result -> {
                if (result.succeeded()) {
                    LOGGER.info(verticle + " deployed");
                } else {
                    LOGGER.info(verticle + " failed");
                }
            });
        }
        Config config = new Config(vertx);

        int sshPort = config.getSSHPort();

        ShellService service = ShellService.create(vertx,
                new ShellServiceOptions().setSSHOptions(
                    new SSHTermOptions().
                        setHost("localhost").
                        setPort(sshPort).
                        setKeyPairOptions(new JksOptions().
                                setPath("keystore.jks").
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
