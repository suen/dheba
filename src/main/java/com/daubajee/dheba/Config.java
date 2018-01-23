package com.daubajee.dheba;

public class Config {

    public static final String P_HTTP_PORT = "dbeba.port.http";

    public static final String P_SSH_PORT = "dbeba.port.ssh";

    public static final String P_P2P_PORT = "dbeba.port.p2p";

    public static final int DEFAULT_HTTP_PORT = 8080;

    public static final int DEFAULT_SSH_PORT = 22022;

    public static final int DEFAULT_P2P_PORT = 42042;


    public int getHttpPort() {
        return getFromSysEnvOrDefault(P_HTTP_PORT, DEFAULT_HTTP_PORT);
    }

    public int getSSHPort() {
        return getFromSysEnvOrDefault(P_SSH_PORT, DEFAULT_SSH_PORT);
    }

    public int getP2PPort() {
        return getFromSysEnvOrDefault(P_P2P_PORT, DEFAULT_P2P_PORT);
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

    public static Config instance(){
        return new Config();
    }

}
