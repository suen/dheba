package com.daubajee.dheba;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;

import org.junit.jupiter.api.Test;

public class TestConfig {

    @Test
    public void testSeedAddress() {

        System.setProperty(Config.P_P2P_SEEDS,
                "localhost:4202;notaaddress;adreù$':12;localhost:9211;2349.12.1:122;rffsr.12.fr:121");

        Config config = new Config();

        Collection<String> seeds = config.getInitialPeerSeeds();
        assertEquals(seeds.size(), 4);
    }

    @Test
    public void testPorts() {
        System.setProperty(Config.P_HTTP_PORT, "8989");
        System.setProperty(Config.P_SSH_PORT, "6969");
        System.setProperty(Config.P_P2P_PORT, "10245");

        Config config = new Config();

        assertEquals(config.getP2PPort(), 10245);
        assertEquals(config.getHttpPort(), 8989);
        assertEquals(config.getSSHPort(), 6969);
    }

}
