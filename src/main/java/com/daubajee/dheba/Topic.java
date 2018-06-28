package com.daubajee.dheba;

public class Topic {

    public static final String REMOTE_PEER_INBOX = "REMOTE_PEER_INBOX";

    public static final String REMOTE_PEER_OUTBOX = "REMOTE_PEER_OUTBOX";

    public static final String REMOTE_PEER_EVENTS = "REMOTE_PEER_EVENTS";

    public static final String PEER_REGISTRY = "PEER_REGISTRY";
    
    public static String getRemotePeerInboxTopic(String hostAddress, int port) {
    	return String.format("%s:%d-INBOX", hostAddress, port);
    }
    
    public static String getRemotePeerCommandTopic(String hostAddress, int port) {
    	return String.format("%s:%d-COMMAND", hostAddress, port);
    }
}
