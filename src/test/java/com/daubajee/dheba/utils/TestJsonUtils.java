package com.daubajee.dheba.utils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.daubajee.dheba.peer.RemotePeer;
import com.daubajee.dheba.utils.JsonUtils;

import io.vertx.core.json.JsonObject;

public class TestJsonUtils {

    @Test
    public void testToFromJson() {
        String agent = "dbeba 0.1";
        String hostname = "remoteHost";
        int port = 8000;
        int bestHeight = 100;
        long lastconnected = System.currentTimeMillis();
        String version = "0.1";
        String services = "SERVICES";
        boolean active = true;

        RemotePeer peer = new RemotePeer(port, hostname);
        peer.setAgent(agent);
        peer.setBestHeight(bestHeight);
        peer.setLastConnected(lastconnected);
        peer.setVersion(version);
        peer.setServices(services);
        peer.setActive(active);

        JsonObject json = JsonUtils.toJson(peer);

        assertThat(json.getString("hostname"), equalTo(hostname));
        assertThat(json.getString("agent"), equalTo(agent));
        assertThat(json.getString("version"), equalTo(version));
        assertThat(json.getString("services"), equalTo(services));
        assertThat(json.getInteger("port"), equalTo(port));
        assertThat(json.getInteger("bestHeight"), equalTo(bestHeight));
        assertThat(json.getBoolean("active"), equalTo(active));

        RemotePeer peerFromJson = JsonUtils.fromJson(json, RemotePeer.class);
        assertThat(peerFromJson.getHostname(), equalTo(hostname));
        assertThat(peerFromJson.getAgent(), equalTo(agent));
        assertThat(peerFromJson.getVersion(), equalTo(version));
        assertThat(peerFromJson.getServices(), equalTo(services));
        assertThat(peerFromJson.getPort(), equalTo(port));
        assertThat(peerFromJson.getBestHeight(), equalTo(bestHeight));
        assertThat(peerFromJson.isActive(), equalTo(active));

    }

}
