package com.daubajee.dheba.peer;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import com.daubajee.dheba.peer.msg.HandShake;
import com.google.common.collect.Lists;

public class Peer {

    private final String address;

    private int incomingPort = 0;

    private int outgoingPort = 0;

    private HandShake handshake;

    private Instant lastActivity = Instant.EPOCH;

    private List<Instant> connection = Lists.newArrayList();

    public Peer(String address) {
        this.address = address;
    }
    
    public int getIncomingPort() {
        return incomingPort;
    }

    public void setIncomingPort(int incomingPort) {
        this.incomingPort = incomingPort;
    }

    public int getOutgoingPort() {
        return outgoingPort;
    }

    public void setOutgoingPort(int outgoingPort) {
        this.outgoingPort = outgoingPort;
    }

    public String getAddress() {
        return address;
    }

    public HandShake getHandshake() {
        return handshake;
    }

    public void setHandshake(HandShake handshake) {
        this.handshake = handshake;
    }

    public Instant getLastActivity() {
        return lastActivity;
    }

    public void setActiveNow() {
        this.lastActivity = Instant.now();
        connection.add(this.lastActivity);
    }

    public void deactivate() {
        this.lastActivity = Instant.EPOCH;
    }

    public int getOutgoingPortFromHandshake() {
        if (this.hasHandshaked()) {
            String addrMe = handshake.getAddrMe();
            return AddressPort.from(addrMe)
                .map(adr -> adr.getPort())
                .orElse(0);
        }
        return 0;
    }
    
    public boolean isOutgoing() {
        return this.outgoingPort != 0;
    }
    
    public boolean isOutgoingUnconnected() {
        return this.isOutgoing() && !this.isActive() && this.canBeAttempted();
    }

    public boolean isOnlyIncomingHandshaked() {
        return !this.isOutgoing() && this.isIncoming() && this.hasHandshaked();
    }

    public boolean isIncoming() {
    	return this.incomingPort != 0;
    }

    public boolean isActive() {
        return lastActivity != Instant.EPOCH;
    }

    public boolean hasHandshaked() {
        return handshake != null && handshake.isValid();
    }

    public boolean canBeAttempted() {
        boolean empty = connection.isEmpty();
        Supplier<Boolean> lastTriedIs10SecAgo = () -> {
            Instant last = connection.get(connection.size() - 1);
            Instant now = Instant.now().minusSeconds(10);
            boolean b = last.compareTo(now) < 0;
            return b;
        };
        return empty || lastTriedIs10SecAgo.get();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((address == null) ? 0 : address.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Peer other = (Peer) obj;
        if (address == null) {
            if (other.address != null)
                return false;
        } else if (!address.equals(other.address))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Peer [address=" + address + ", incomingPort=" + incomingPort + ", outgoingPort=" + outgoingPort + "]";
    }

}
