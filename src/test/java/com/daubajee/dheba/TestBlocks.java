package com.daubajee.dheba;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.daubajee.dheba.block.Blockchain;

public class TestBlocks {

    @Test
    public void testHashMatchesDifficulty() {
        String hash = "0000000000000000003ef96045c1de89a32f1a13510cca0bf3ca261b4f02e9e3";
        for (int j = 0; j < hash.length(); j++) {
            boolean goodHash = Blockchain.hashMatchesDifficulty(hash, j);
            assertThat("Failed for : " + j, goodHash, equalTo(j <= 18));
            assertThat("Failed for : " + j, !goodHash, equalTo(j > 18));
        }
    }

}
