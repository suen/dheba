package com.daubajee.dheba.block.msg;

import com.daubajee.dheba.block.Block;

import io.vertx.core.json.JsonObject;

public class OneBlock {

    private final Block block;

    public OneBlock(Block block) {
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }

    public boolean isValid() {
        return block.isValid();
    }

    public JsonObject toJson() {
        return new JsonObject()
                    .put("block", block.toJson());
    }

    public static OneBlock from(JsonObject json) {
        JsonObject blockJson = json.getJsonObject("block", new JsonObject());
        Block block = Block.from(blockJson);
        return new OneBlock(block);
    }
}
