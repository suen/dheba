package com.daubajee.dheba;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;

public class MsgUtils {

    public static final String REQUEST = "REQUEST";
    public static final String PARAMS = "PARAMS";
    public static final String STATUS = "STATUS";
    public static final String REPLY = "REPLY";

    public static JsonObject createRequest(String request, String params) {
        return new JsonObject()
                .put(REQUEST, request)
                .put(PARAMS, params);
    }
    
    public static JsonObject createRequest(String request) {
        return new JsonObject()
                .put(REQUEST, request);
    }

    public static JsonObject createReply(String status, String reply) {
        return new JsonObject()
                .put(STATUS, status)
                .put(REPLY, reply);
    }

    public static JsonObject createReply(String status) {
        return new JsonObject().put(STATUS, status);
    }

    public static String getStatus(JsonObject replyJson) {
        return replyJson.getString(STATUS);
    }

    public static String getReplyMsgString(JsonObject replyJson) {
        return replyJson.getString(REPLY);
    }

    public static DeliveryOptions deliveryOpWithTimeout(long ms) {
        DeliveryOptions options = new DeliveryOptions();
        options.setSendTimeout(ms);
        return options;
    }

}
