package com.daubajee.dheba.peer;

import com.daubajee.dheba.peer.msg.HandShake;

public class PeerConnection {

    private final String address;

    private int incomingPort = 0;

    private int outgoingPort = 0;

    private HandShake handshake;

    public PeerConnection(String address) {
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
        PeerConnection other = (PeerConnection) obj;
        if (address == null) {
            if (other.address != null)
                return false;
        } else if (!address.equals(other.address))
            return false;
        return true;
    }

}
