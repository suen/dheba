package com.daubajee.dheba.peer;

import io.vertx.core.json.JsonObject;

public class PeerMessage {

    private final String type;

    private final JsonObject content;

    public static final String HANDSHAKE = "HANDSHAKE";
    
    public PeerMessage(String type, JsonObject content) {
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
        return !type.isEmpty() && !content.isEmpty();
    }

    public static PeerMessage from(JsonObject packet) {
        String type = packet.getString(S.TYPE, "");
        JsonObject content = packet.getJsonObject(S.CONTENT, new JsonObject());
        return new PeerMessage(type, content);
    }
    
    public JsonObject toJson() {
        return new JsonObject()
                    .put(S.TYPE, type)
                    .put(S.CONTENT, content);
    }

}
