package com.daubajee.dheba;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Collection;

import org.junit.Test;

public class TestConfig {

    @Test
    public void testSeedAddress() {

        System.setProperty(Config.P_P2P_SEEDS,
                "localhost:4202;notaaddress;adreù$':12;localhost:9211;2349.12.1:122;rffsr.12.fr:121");

        Config config = new Config();

        Collection<String> seeds = config.getInitialPeerSeeds();

        assertThat(seeds.size(), equalTo(4));
    }

    @Test
    public void testPorts() {
        System.setProperty(Config.P_HTTP_PORT, "8989");
        System.setProperty(Config.P_SSH_PORT, "6969");
        System.setProperty(Config.P_P2P_PORT, "10245");

        Config config = new Config();

        assertThat(config.getP2PPort(), equalTo(10245));
        assertThat(config.getHttpPort(), equalTo(8989));
        assertThat(config.getSSHPort(), equalTo(6969));
    }

}
