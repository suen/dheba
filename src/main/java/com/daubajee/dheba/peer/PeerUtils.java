package com.daubajee.dheba.peer;

import com.daubajee.dheba.peer.msg.HandShake;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

public class PeerUtils {

    public static Buffer toSocketFrame(JsonObject json) {
        byte[] bytes = json.toString().getBytes();
        Buffer frame = Buffer.buffer(4 + bytes.length);
        frame.appendInt(bytes.length);
        frame.appendBytes(bytes);
        return frame;
    }

    public static JsonObject fromSocketFrame(Buffer buffer) {
        int offset = 0;
        try {
            JsonObject json = null;
            while (offset < buffer.length()) {
                int frameLength = buffer.getInt(offset);
                if (offset + 4 + frameLength > buffer.length()) {
                    throw new IllegalArgumentException("Frame ended prematurely");
                }
                Buffer jsonBuffer = buffer.getBuffer(offset + 4, offset + 4 + frameLength);
                offset += 4 + frameLength;
                json = new JsonObject(jsonBuffer);
            }
            return json;
        } catch (Exception e) {
            throw new IllegalArgumentException("An exception occured while parsing " + buffer.toString(), e);
        }
    }
    
    public static JsonObject createPeerSendMsg(JsonObject body) {
        return new JsonObject()
                .put(S.TYPE, "MESSAGE")
                .put(S.ADDRESS, "*")
                .put(S.BODY, body);
    }
    
    public static JsonObject createRemotePeerPacket(String type, JsonObject content) {
        return new JsonObject()
            .put(S.TYPE, type)
            .put(S.CONTENT, content);
    }

    public static JsonObject createPeerSendVerticlePacket(String remoteHost, Integer remotePort,
            JsonObject handShakeMsg) {
        return new JsonObject()
                .put(S.REMOTE_HOST, remoteHost)
                .put(S.REMOTE_PORT, remotePort)
                .put(S.TYPE, "PEER_SEND")
                .put(S.MESSAGE, handShakeMsg);
    }

    public static JsonObject createHandShakeMessage(String remoteHost, Integer remotePort, String selfHost,
            Integer selfPort) {
        HandShake handShake = new HandShake();
        handShake.setAddrMe(selfHost + ":" + selfPort);
        handShake.setAddrYou(remoteHost + ":" + remotePort);
        handShake.setAgent("dheba 0.1");
        handShake.setBestHeight(1);
        handShake.setServices("NODE BETA ALPHA");
        handShake.setTimestamp(System.currentTimeMillis());
        handShake.setVersion("0.1");
        return handShake.toJson();
    }

}
