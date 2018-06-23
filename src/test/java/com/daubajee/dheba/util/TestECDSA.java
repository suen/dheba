package com.daubajee.dheba.util;

import java.security.KeyPair;
import java.security.PrivateKey;

import org.junit.Test;

import com.daubajee.dheba.utils.ECDSA;

public class TestECDSA {

    @Test
    public void testECDSA() throws Exception {
        KeyPair keypair = ECDSA.generatePrivateKey();
        PrivateKey privKey = keypair.getPrivate();

        System.out.println("Private key: " + ECDSA.toHexString(privKey.getEncoded()));

        byte[] rawBytes = "hello world".getBytes();
        byte[] signatureBytes = ECDSA.sign(privKey.getEncoded(), rawBytes);

        System.out.println(ECDSA.toHexString(signatureBytes));

        boolean verified = ECDSA.verify(keypair.getPublic(), rawBytes, signatureBytes);
        System.out.println("Signature: " + verified);
    }

}
