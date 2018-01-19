package com.daubajee.dheba.peer.msg;

import io.vertx.core.json.JsonObject;

public class HandShake {

    private String version;

    private String services;

    private long timestamp;

    private String addrYou;

    private String addrMe;

    private String agent;

    private int bestHeight;

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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getAddrYou() {
        return addrYou;
    }

    public void setAddrYou(String addrYou) {
        this.addrYou = addrYou;
    }

    public String getAddrMe() {
        return addrMe;
    }

    public void setAddrMe(String addrMe) {
        this.addrMe = addrMe;
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
    
    public JsonObject toJson() {
        return new JsonObject()
                .put("version", version)
                .put("services", services)
                .put("timestamp", timestamp)
                .put("addrYou", addrYou)
                .put("addrMe", addrMe)
                .put("agent", agent)
                .put("bestHeight", bestHeight);
    }

}
