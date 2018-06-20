package com.daubajee.dheba.peer;

import io.vertx.core.json.JsonObject;

public class RemotePeerEvent {

    private final String remoteHostAddress;

    private final int remoteHostPort;

    private final String type;

    public static final String CONNECTED = "CONNECTED";

    public static final String DISCONNECTED = "DISCONNECTED";

    public static final String NEW_PEER = "NEW_PEER";

    public RemotePeerEvent(String remoteHostAddress, int remoteHostPort, String type) {
        this.remoteHostAddress = remoteHostAddress;
        this.remoteHostPort = remoteHostPort;
        this.type = type;
    }

    public String getRemoteHostAddress() {
        return remoteHostAddress;
    }

    public int getRemoteHostPort() {
        return remoteHostPort;
    }

    public String getType() {
        return type;
    }

    public boolean isValid() {
        return !remoteHostAddress.isEmpty() && remoteHostPort != 0 && !type.isEmpty();
    }

    public static RemotePeerEvent from(JsonObject json) {
        String remoteHostAddress = json.getString(S.REMOTE_HOST_ADDRESS, "");
        Integer remoteHostPort = json.getInteger(S.REMOTE_HOST_PORT, 0);
        String type = json.getString(S.TYPE, "");
        return new RemotePeerEvent(remoteHostAddress, remoteHostPort, type);
    }
    
    public JsonObject toJson() {
        return new JsonObject()
                    .put(S.REMOTE_HOST_ADDRESS, remoteHostAddress)
                    .put(S.REMOTE_HOST_PORT, remoteHostPort)
                    .put(S.TYPE, type);
    }

}
