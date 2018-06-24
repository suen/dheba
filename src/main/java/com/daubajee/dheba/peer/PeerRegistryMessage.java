package com.daubajee.dheba.peer;

import java.util.Arrays;
import java.util.List;

import io.vertx.core.json.JsonObject;

public class PeerRegistryMessage {

    private final String type;

    private final JsonObject content;

    public static final String GET_LIST = "GET_LIST";

    public static final String LIST = "LIST";
    
    public static final List<String> TYPES = Arrays.asList(GET_LIST, LIST);

    public PeerRegistryMessage(String type, JsonObject content) {
        this.type = type;
        this.content = content == null ? new JsonObject() : content;
    }

    public String getType() {
        return type;
    }

    public JsonObject getContent() {
        return content;
    }

    public boolean isValid() {
        return type != null && !type.isEmpty() && TYPES.contains(type);
    }

    public JsonObject toJson() {
        return new JsonObject()
                    .put("type", type)
                    .put("content", content);
    }

    public static PeerRegistryMessage from(JsonObject json) {
        String jtype = json.getString("type");
        JsonObject jcontent = json.getJsonObject("content", new JsonObject());
        return new PeerRegistryMessage(jtype, jcontent);
    }
}
