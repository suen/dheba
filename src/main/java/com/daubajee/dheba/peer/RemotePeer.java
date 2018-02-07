package com.daubajee.dheba.peer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;

import com.daubajee.dheba.Config;
import com.daubajee.dheba.utils.JsonUtils;

import io.vertx.core.json.JsonObject;

public class RemotePeer {

    private int port;

    private String hostname;

    private String hostAddress;

    private long lastConnected;

    private boolean active;

    private String version;

    private String services;

    private String agent;

    private int bestHeight;

    public RemotePeer() {

    }

    public RemotePeer(String address) {
        Matcher matcher = Config.P2P_ADDRESS_PATTERN.matcher(address);
        if (matcher.matches()) {
            this.hostname = matcher.group(1);
            this.port = Integer.parseInt(matcher.group(3));
        } else {
            throw new IllegalArgumentException("Invalid address " + address);
        }

        initHostAddress();
    }

    private void initHostAddress() {
        try {
            hostAddress = InetAddress.getByName(hostname).getHostAddress();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Unknown host " + hostname);
        }
    }

    public RemotePeer(int port, String hostname) {
        this.port = port;
        this.hostname = hostname;
        initHostAddress();
    }

    public String identifier() {
        return hostAddress + ":" + port;
    }

    public int getPort() {
        return port;
    }

    public String getHostname() {
        return hostname;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    public long getLastConnected() {
        return lastConnected;
    }

    public void setLastConnected(long lastConnected) {
        this.lastConnected = lastConnected;
    }

    public void updateLastConnected(long lastConnected) {
        setLastConnected(lastConnected);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getServices() {
        return services;
    }

    public void setServices(String services) {
        this.services = services;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public int getBestHeight() {
        return bestHeight;
    }

    public void setBestHeight(int bestHeight) {
        this.bestHeight = bestHeight;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((hostname == null) ? 0 : hostname.hashCode());
        result = prime * result + port;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        // use only hostname and port for equals
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RemotePeer other = (RemotePeer) obj;
        if (hostname == null) {
            if (other.hostname != null)
                return false;
        } else if (!hostname.equals(other.hostname))
            return false;
        if (port != other.port)
            return false;
        return true;
    }

    public JsonObject toJson() {
        return JsonUtils.toJson(this);
    }

    public static RemotePeer fromJson(JsonObject json) {
        return JsonUtils.fromJson(json, RemotePeer.class);
    }
}
