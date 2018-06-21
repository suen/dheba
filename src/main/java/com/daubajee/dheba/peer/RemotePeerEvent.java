package com.daubajee.dheba.peer;

import io.vertx.core.json.JsonObject;

public class RemotePeerEvent {

    private final String remoteHostAddress;

    private final int remoteHostPort;

    private final String type;

    private final JsonObject content;
    
    public static final String CONNECTED = "CONNECTED";

    public static final String DISCONNECTED = "DISCONNECTED";

    public static final String NEW_PEER = "NEW_PEER";

    public static final String HANDSHAKED = "HANDSHAKED";
    
    public RemotePeerEvent(String remoteHostAddress, int remoteHostPort, String type) {
        this.remoteHostAddress = remoteHostAddress;
        this.remoteHostPort = remoteHostPort;
        this.type = type;
        this.content = new JsonObject();
    }

    public RemotePeerEvent(String remoteHostAddress, int remoteHostPort, String type, JsonObject content) {
		this.remoteHostAddress = remoteHostAddress;
		this.remoteHostPort = remoteHostPort;
		this.type = type;
		this.content = content;
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

    public JsonObject getContent() {
		return content;
	}

	public boolean isValid() {
        return !remoteHostAddress.isEmpty() && remoteHostPort != 0 && !type.isEmpty();
    }

    public static RemotePeerEvent from(JsonObject json) {
        String remoteHostAddress = json.getString(S.REMOTE_HOST_ADDRESS, "");
        Integer remoteHostPort = json.getInteger(S.REMOTE_HOST_PORT, 0);
        String type = json.getString(S.TYPE, "");
        JsonObject content = json.getJsonObject(S.CONTENT, new JsonObject());
        return new RemotePeerEvent(remoteHostAddress, remoteHostPort, type, content);
    }
    
    public JsonObject toJson() {
        return new JsonObject()
                    .put(S.REMOTE_HOST_ADDRESS, remoteHostAddress)
                    .put(S.REMOTE_HOST_PORT, remoteHostPort)
                    .put(S.TYPE, type)
                    .put(S.CONTENT, content);
    }

}
