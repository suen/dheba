package com.daubajee.dheba.block.miner;

import java.util.Arrays;
import java.util.List;

import com.daubajee.dheba.peer.S;

import io.vertx.core.json.JsonObject;

public class BlockMinerMessage {

    private final String type;

    private final JsonObject content;

    public final static String MINE_BLOCK = "MINE_BLOCK";

    public final static String BLOCK_FOUND = "BLOCK_FOUND";

    private final static List<String> TYPES = Arrays.asList(MINE_BLOCK, BLOCK_FOUND);

    public BlockMinerMessage(String type, JsonObject content) {
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
        return type != null && TYPES.contains(type);
    }

    public static BlockMinerMessage from(JsonObject json) {
        String type = json.getString(S.TYPE, "");
        JsonObject content = json.getJsonObject(S.CONTENT, new JsonObject());
        return new BlockMinerMessage(type, content);
    }

    public JsonObject toJson() {
        return new JsonObject()
                .put(S.TYPE, type)
                .put(S.CONTENT, content);
    }

}
