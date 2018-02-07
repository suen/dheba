package com.daubajee.dheba.utils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.daubajee.dheba.peer.RemotePeer;
import com.daubajee.dheba.peer.msg.HandShake;

import io.vertx.core.json.JsonObject;

public class TestJsonUtils {

    @Test
    public void testToFromJson() {
        String agent = "dbeba 0.1";
        String hostname = "localhost";
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

    @Test
    public void testToFromJsonHandshake() {
        String jsonStr = "{\"version\":\"0.42\",\"services\":\"NODE BETA ALPHA\","
                + "\"timestamp\":1517072101027,\"addrYou\":\"127.0.0.1:42042\","
                + "\"addrMe\":\"chaku:3032\",\"agent\":\"dheba 0.1\",\"bestHeight\":1}";
        HandShake handshake = HandShake.fromJson(new JsonObject(jsonStr));
        assertThat(handshake.getAddrMe(), equalTo("chaku:3032"));
        assertThat(handshake.getAddrYou(), equalTo("127.0.0.1:42042"));
        assertThat(handshake.getAgent(), equalTo("dheba 0.1"));
        assertThat(handshake.getBestHeight(), equalTo(1));
        assertThat(handshake.getTimestamp(), equalTo(1517072101027L));
        assertThat(handshake.getVersion(), equalTo("0.42"));
        assertThat(handshake.getServices(), equalTo("NODE BETA ALPHA"));
    }

}
