package com.daubajee.dheba;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.daubajee.dheba.peer.Peer;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class Config {

    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);

    public static final String P_HTTP_PORT = "dheba.port.http";

    public static final String P_SSH_PORT = "dheba.port.ssh";

    public static final String P_P2P_PORT = "dheba.port.p2p";

    public static final String P_P2P_SEEDS = "dheba.seeds.p2p";

    public static final int DEFAULT_HTTP_PORT = 8080;

    public static final int DEFAULT_SSH_PORT = 22022;

    public static final int DEFAULT_P2P_PORT = 42042;

    public static final Pattern P2P_ADDRESS_PATTERN = Pattern
            .compile("(?<hostname>(?:[\\w\\d]+)(?:\\.[\\w\\d]+)*)(\\:)(?<port>\\d+)");

    public static final Integer MAX_PEER_CONNECTIONS = 2;

    private final JsonObject conf;

    public Config(Vertx vertx) {

        JsonObject defaultValue = new JsonObject()
                .put(P_HTTP_PORT, DEFAULT_HTTP_PORT)
                .put(P_SSH_PORT, DEFAULT_SSH_PORT)
                .put(P_P2P_PORT, DEFAULT_P2P_PORT);
        
        ConfigStoreOptions jsonStore = new ConfigStoreOptions()
                .setType("json")
                .setConfig(defaultValue);
        
        ConfigStoreOptions envStore = new ConfigStoreOptions()
                .setType("env");
        
        ConfigStoreOptions sysPropStore = new ConfigStoreOptions()
                .setType("sys")
                .setConfig(new JsonObject().put("cache", false));
        ConfigRetrieverOptions options = new ConfigRetrieverOptions().addStore(jsonStore).addStore(envStore)
                .addStore(sysPropStore);

        ConfigRetriever configRetriever = ConfigRetriever.create(vertx, options);

        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        configRetriever.getConfig(h -> {
            future.complete(h.result());
        });

        try {
            conf = future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }

    }

    public int getHttpPort() {
        return conf.getInteger(P_HTTP_PORT);
    }

    public int getSSHPort() {
        return conf.getInteger(P_SSH_PORT);
    }

    public int getP2PPort() {
        return conf.getInteger(P_P2P_PORT);
    }

    public String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
    }

    @Deprecated
    public Collection<String> getInitialPeerSeeds() {
        String seeds = conf.getString(P_P2P_SEEDS, "");
        if (seeds.isEmpty()) {
            return new ArrayList<>(0);
        }
        String[] seedSplits = seeds.split(";");
        Set<String> validSeeds = Arrays.asList(seedSplits).stream()
            .filter(s -> {
                boolean matches = P2P_ADDRESS_PATTERN.matcher(s.trim()).matches();
                if (!matches) {
                    LOGGER.warn("Seed address invalid : " + s);
                }
                return matches;
            })
            .collect(Collectors.toSet());
        if (validSeeds.isEmpty()) {
            LOGGER.warn("No initial seed address configured");
        }
        return validSeeds;
    }

    public Set<Peer> getPeerSeeds() {
        String seeds = conf.getString(P_P2P_SEEDS, "");
        if (seeds.isEmpty()) {
            LOGGER.warn("No initial seed address configured");
            return Collections.emptySet();
        }
        String[] seedSplits = seeds.split(";");
        Set<Peer> validSeeds = Arrays.asList(seedSplits).stream()
            .map(s -> {
                Matcher matcher = P2P_ADDRESS_PATTERN.matcher(s.trim());
                Peer peer = null;
                if (matcher.find()) {
                    try {
                        String hostname = matcher.group("hostname");
                        Integer port = Integer.parseInt(matcher.group("port"));
                        InetAddress inetAddress = InetAddress.getByName(hostname);
                        String hostAddress = inetAddress.getHostAddress();                    
                        peer = new Peer(hostAddress);
                        peer.setOutgoingPort(port);
                    } catch (NumberFormatException | UnknownHostException e) {
                        LOGGER.warn("Unknown host : " + s);
                    }
                } else {
                    LOGGER.warn("Seed address invalid : " + s);
                }
                return Optional.ofNullable(peer);
            })
            .filter(op -> op.isPresent())
            .map(op -> op.get())
            .collect(Collectors.toSet());
        
        if (validSeeds.isEmpty()) {
            LOGGER.warn("No initial seed address configured");
        }
        return validSeeds;
    }

    public Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value);
        } catch (Exception e){
            throw new IllegalArgumentException(e);
        }
    }

    public Integer getMaxPeerConnections() {
        return MAX_PEER_CONNECTIONS;
    }

}
