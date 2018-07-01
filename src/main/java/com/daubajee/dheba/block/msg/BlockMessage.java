package com.daubajee.dheba.block.msg;

import java.util.Arrays;
import java.util.List;

import com.daubajee.dheba.peer.S;

import io.vertx.core.json.JsonObject;

public class BlockMessage {

    private final String type;

    private final JsonObject content;

    public final static String GET_HEADERS = "GET_HEADERS";

    public final static String HEADERS = "HEADERS";

    public final static String GET_BLOCK = "GET_BLOCK";

    public final static String BLOCK = "BLOCK";

    private final static List<String> TYPES = Arrays.asList(GET_HEADERS, HEADERS, GET_BLOCK, BLOCK);

    public BlockMessage(String type, JsonObject content) {
        this.type = type;
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public JsonObject getContent() {
        return content;
    }

    public boolean isValid() {
        return !type.isEmpty() && TYPES.contains(type) && !content.isEmpty();
    }

    public static BlockMessage from(JsonObject json) {
        String type = json.getString(S.TYPE, "");
        JsonObject content = json.getJsonObject(S.CONTENT, new JsonObject());
        return new BlockMessage(type, content);
    }

    public JsonObject toJson() {
        return new JsonObject()
                .put(S.TYPE, type)
                .put(S.CONTENT, content);
    }
}
