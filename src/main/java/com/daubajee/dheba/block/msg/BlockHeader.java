package com.daubajee.dheba.block.msg;

import io.vertx.core.json.JsonObject;

public class BlockHeader {

    private final int height;

    private final String hash;

    public BlockHeader(int height, String hash) {
        super();
        this.height = height;
        this.hash = hash;
    }

    public int getHeight() {
        return height;
    }

    public String getHash() {
        return hash;
    }

    public boolean isValid() {
        return height != -1 && !hash.isEmpty();
    }

    public JsonObject toJson() {
        return new JsonObject()
                    .put("height", height)
                    .put("hash", hash);
    }

    public static BlockHeader from(JsonObject json) {
        Integer height = json.getInteger("height", -1);
        String hash = json.getString("hash");
        return new BlockHeader(height, hash);
    }
    
}
