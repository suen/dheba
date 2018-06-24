package com.daubajee.dheba.peer.msg;

import java.util.Collections;
import java.util.List;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class PeerList {

    private final List<String> peers;

    public PeerList(List<String> peers) {
        this.peers = Collections.unmodifiableList(peers);
    }

    public List<String> getPeers() {
        return peers;
    }

    public JsonObject toJson() {
        return new JsonObject().put("peers", new JsonArray(peers));
    }

    public static PeerList from(JsonObject json) {
        Integer max = json.getInteger("max", -1);
        JsonArray jsonArray = json.getJsonArray("peers", new JsonArray());
        List<String> peers = jsonArray.getList();
        return new PeerList(peers);
    }
}
