package com.daubajee.dheba.peer.msg;

import java.util.Collections;
import java.util.List;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class GetPeerList {

    private final int max;

    private final List<String> exclude;

    public GetPeerList(int max, List<String> exclude) {
        this.max = max;
        this.exclude = Collections.unmodifiableList(exclude);
    }

    public int getMax() {
        return max;
    }

    public List<String> getExclude() {
        return exclude;
    }

    public JsonObject toJson() {
        return new JsonObject()
                .put("max", max)
                .put("exclude", new JsonArray(exclude));
    }

    public static GetPeerList from(JsonObject json) {
        Integer max = json.getInteger("max", -1);
        JsonArray jsonArray = json.getJsonArray("exclude", new JsonArray());
        List<String> excludeList = jsonArray.getList();
        return new GetPeerList(max, excludeList);
    }

}
