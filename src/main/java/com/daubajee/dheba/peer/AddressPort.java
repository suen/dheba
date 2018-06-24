package com.daubajee.dheba.peer;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddressPort {

    private final String address;

    private final int port;

    public static final Pattern P2P_ADDRESS_PATTERN = Pattern
            .compile("(?<hostname>(?:[\\w\\d]+)(?:\\.[\\w\\d]+)*)(\\:)(?<port>\\d+)");

    public AddressPort(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public static Optional<AddressPort> from(String addressPortStr) {
        Matcher matcher = P2P_ADDRESS_PATTERN.matcher(addressPortStr);
        AddressPort adrPort = null;
        try {
            if (matcher.find()) {
                String address = matcher.group("hostname");
                int port = Integer.parseInt(matcher.group("port"));
                adrPort = new AddressPort(address, port);
            }
        } catch (Exception e) {
            // DO NOTHING
        }
        return Optional.ofNullable(adrPort);
    }

    @Override
    public String toString() {
        return address + ":" + port;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((address == null) ? 0 : address.hashCode());
        result = prime * result + port;
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
        AddressPort other = (AddressPort) obj;
        if (address == null) {
            if (other.address != null)
                return false;
        } else if (!address.equals(other.address))
            return false;
        if (port != other.port)
            return false;
        return true;
    }

}
