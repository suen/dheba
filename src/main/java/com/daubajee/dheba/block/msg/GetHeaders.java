package com.daubajee.dheba.block.msg;

import io.vertx.core.json.JsonObject;

public class GetHeaders {

    private final BlockHeader after;

    private final int limit;

    public GetHeaders(BlockHeader after, int limit) {
        this.after = after;
        this.limit = limit;
    }

    public BlockHeader getAfter() {
        return after;
    }

    public int getLimit() {
        return limit;
    }
    
    public boolean isValid() {
        return after.isValid();
    }

    public JsonObject toJson() {
        return new JsonObject()
                    .put("after", after)
                    .put("limit", limit);
    }

    public static GetHeaders from(JsonObject json) {
        JsonObject afterJson = json.getJsonObject("after", new JsonObject());
        BlockHeader after = BlockHeader.from(afterJson);
        Integer limit = json.getInteger("limit", -1);
        return new GetHeaders(after, limit);
    }

}
