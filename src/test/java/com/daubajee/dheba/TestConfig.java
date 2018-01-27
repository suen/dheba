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

}
