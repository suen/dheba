package com.daubajee.dheba.peer;

import io.vertx.core.json.JsonObject;

public class RemotePeerPacket {

    private final String remoteHostAddress;

    private final int remoteHostPort;

    private final JsonObject content;

    public RemotePeerPacket(String remoteHostAddress, int remoteHostPort, JsonObject content) {
        this.remoteHostAddress = remoteHostAddress;
        this.remoteHostPort = remoteHostPort;
        this.content = content;
    }

    public String getRemoteHostAddress() {
        return remoteHostAddress;
    }

    public int getRemoteHostPort() {
        return remoteHostPort;
    }

    public JsonObject getContent() {
        return content;
    }

    public boolean isValid() {
        return !remoteHostAddress.isEmpty() && remoteHostPort != 0 && !content.isEmpty();
    }

    public static RemotePeerPacket from(JsonObject packet) {
        String remoteHostAddress = packet.getString(S.REMOTE_HOST_ADDRESS, "");
        Integer remoteHostPort = packet.getInteger(S.REMOTE_HOST_PORT, 0);
        JsonObject content = packet.getJsonObject(S.CONTENT, new JsonObject());
        return new RemotePeerPacket(remoteHostAddress, remoteHostPort, content);
    }
    
    public JsonObject toJson() {
        return new JsonObject()
                    .put(S.REMOTE_HOST_ADDRESS, remoteHostAddress)
                    .put(S.REMOTE_HOST_PORT, remoteHostPort)
                    .put(S.CONTENT, content);
    }

}
