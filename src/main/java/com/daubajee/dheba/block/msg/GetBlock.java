package com.daubajee.dheba.block.msg;

import io.vertx.core.json.JsonObject;

public class GetBlock {

    private final BlockHeader header;

    public GetBlock(BlockHeader header) {
        this.header = header;
    }

    public BlockHeader getHeader() {
        return header;
    }

    public boolean isValid() {
        return header.isValid();
    }

    public JsonObject toJson() {
        return new JsonObject()
                    .put("header", header);
    }

    public static GetBlock from(JsonObject json) {
        JsonObject headerJson = json.getJsonObject("header", new JsonObject());
        BlockHeader header = BlockHeader.from(headerJson);
        return new GetBlock(header);
    }    
}
