package com.daubajee.dheba.utils;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import com.google.common.hash.HashCode;

public class ECDSA {

    public static KeyPair generatePrivateKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            ECGenParameterSpec kpgparams = new ECGenParameterSpec("secp256r1");

            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");

            generator.initialize(256, random);

            KeyPair keyPair = generator.generateKeyPair();

            return keyPair;

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String toHexString(byte[] bin) {
        return HashCode.fromBytes(bin).toString();
    }

    public static byte[] toBytes(String hexString) {
        return HashCode.fromString(hexString).asBytes();
    }

    public static byte[] sign(byte[] privKeyBytes, byte[] content) throws Exception {
        
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        PKCS8EncodedKeySpec ec = new PKCS8EncodedKeySpec(privKeyBytes);
        PrivateKey priv = keyFactory.generatePrivate(ec);

        Signature ecdsaSign = Signature.getInstance("SHA256withECDSA");

        ecdsaSign.initSign(priv);
        ecdsaSign.update(content);
        byte[] signature = ecdsaSign.sign();
        return signature;
    }

    public static boolean verify(byte[] pubKey, byte[] data, byte[] signature) throws Exception {

        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        X509EncodedKeySpec ec = new X509EncodedKeySpec(pubKey);
        PublicKey publicKey = keyFactory.generatePublic(ec);

        return verify(publicKey, data, signature);
    }

    public static boolean verify(PublicKey publicKey, byte[] data, byte[] signature)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature ecdsaSign = Signature.getInstance("SHA256withECDSA");
        ecdsaSign.initVerify(publicKey);
        ecdsaSign.update(data);
        return ecdsaSign.verify(signature);
    }
}
