package com.daubajee.dheba.block.msg;

import java.util.List;
import java.util.stream.Collectors;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class BlockHeaders {

    private final List<BlockHeader> headers;

    public BlockHeaders(List<BlockHeader> headers) {
        this.headers = headers;
    }

    public List<BlockHeader> getHeaders() {
        return headers;
    }

    public boolean isValid() {
        return headers.stream()
                .allMatch(header -> header.isValid());
    }
    
    public JsonObject toJson() {
        List<JsonObject> headersJson = headers.stream().map(BlockHeader::toJson).collect(Collectors.toList());
        JsonArray headerArray = new JsonArray(headersJson);
        return new JsonObject()
                    .put("headers", headerArray);
    }
    
    public static BlockHeaders from(JsonObject json) {
        JsonArray headerArray = json.getJsonArray("headers", new JsonArray());
        List<JsonObject> headersJson = headerArray.getList();
        List<BlockHeader> headersList = headersJson.stream()
            .map(headerJson -> BlockHeader.from(headerJson))
            .collect(Collectors.toList());
        return new BlockHeaders(headersList);
    }
    
}
