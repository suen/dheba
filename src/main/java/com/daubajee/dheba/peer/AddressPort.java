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

}
