package com.daubajee.dheba;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class Config {
    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);

    public static final String P_HTTP_PORT = "dheba.port.http";

    public static final String P_SSH_PORT = "dheba.port.ssh";

    public static final String P_P2P_PORT = "dheba.port.p2p";

    public static final String P_P2P_SEEDS = "dheba.seeds.p2p";

    public static final int DEFAULT_HTTP_PORT = 8080;

    public static final int DEFAULT_SSH_PORT = 22022;

    public static final int DEFAULT_P2P_PORT = 42042;

    public static final Pattern P2P_ADDRESS_PATTERN = Pattern.compile("((?:[\\w\\d]+)(?:\\.[\\w\\d]+)*)(\\:)(\\d+)");

    public static final Integer MAX_PEER_CONNECTIONS = 2;

    private final int httpPort = getFromSysEnvOrDefault(P_HTTP_PORT, DEFAULT_HTTP_PORT);

    private final int p2pPort = getFromSysEnvOrDefault(P_P2P_PORT, DEFAULT_P2P_PORT);

    private final int sshPort = getFromSysEnvOrDefault(P_SSH_PORT, DEFAULT_SSH_PORT);

    public Config() {

    }

    public int getHttpPort() {
        return httpPort;
    }

    public int getSSHPort() {
        return sshPort;
    }

    public int getP2PPort() {
        return p2pPort;
    }

    public String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
    }

    public Collection<String> getInitialPeerSeeds() {
        String seeds = getFromSysEnvOrDefault(P_P2P_SEEDS, "");
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

    public String getFromSysEnvOrDefault(String name, String defaultValue) {
        String sysProp = System.getProperty(name, "");
        if (!sysProp.isEmpty()) {
            return sysProp;
        }
        String envProp = System.getenv(name);
        if (envProp != null && !envProp.isEmpty()) {
            return envProp;
        }
        return defaultValue;

    }

    public Integer getFromSysEnvOrDefault(String name, Integer defaultValue) {
        String sysProp = System.getProperty(name, "");
        if (!sysProp.isEmpty()) {
            return parseInteger(sysProp);
        }
        String envProp = System.getenv(name);
        if (envProp != null && !envProp.isEmpty()) {
            return parseInteger(envProp);
        }
        return defaultValue;
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

    public static Config instance(){
        return new Config();
    }

}
