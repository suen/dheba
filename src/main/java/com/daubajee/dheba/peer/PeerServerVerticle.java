package com.daubajee.dheba.peer;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.bridge.BridgeOptions;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.eventbus.bridge.tcp.TcpEventBusBridge;

public class PeerServerVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(PeerServerVerticle.class);

    private EventBus eventBus;

    @Override
    public void start() throws Exception {
        eventBus = vertx.eventBus();

        NetServer server = vertx.createNetServer();

        server.connectHandler(this::onPeerConnect);

        server.listen(7000, res -> {
            if (res.failed()) {
                LOGGER.error("Server bind on port 7000 failed");
                return;
            }
        });
    }

    private Object onPeerConnect(NetSocket socket) {

        return null;
    }

    public void sd() throws Exception {

        this.eventBus = vertx.eventBus();
        
        NetServer server = vertx.createNetServer();

        server.connectHandler(socket -> {
            SocketAddress remoteAddress = socket.remoteAddress();
            socket.handler(buffer -> {
                System.out.println("Server Received (buffer) : " + buffer.toString());
                int offset = 0;
                while (offset < buffer.length()) {
                    int frameLength = buffer.getInt(offset);
                    if (offset + 4 + frameLength > buffer.length()) {
                        System.err.println("Frame ended prematurely");
                        break;
                    }
                    Buffer jsonBuffer = buffer.getBuffer(offset + 4, offset + 4 + frameLength);
                    offset += 4 + frameLength;
                    try {
                        JsonObject json = parseIncomingJson(jsonBuffer);
                        processJsonObject(json, socket);
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                        continue;
                    }
                }
            });
        });

        server.listen(7000, res -> {
            if (res.failed()) {
                System.err.println("Failed");
                return;
            }
        });
            
    }

    private void processJsonObject(JsonObject json, NetSocket socket) {
        JsonObject body = json.getJsonObject("body", new JsonObject());
        String type = json.getString("type");
        String address = json.getString("address");

        switch (type) {
            case "register" :
                MessageConsumer<Object> consumer = eventBus.consumer(address, handler -> {
                    JsonObject replyBody = (JsonObject) handler.body();
                    JsonObject reply = new JsonObject().put("type", "publish").put("address", address).put("body", replyBody);
                    socket.write(PeerUtils.toSocketFrame(reply));
                });
                break;
            case "unregister":
                //TODO
                break;
                
            case "publish":
                eventBus.publish(address, body);
        }
    }

    private JsonObject parseIncomingJson(Buffer jsonBuffer) {
        try {
            JsonObject json = new JsonObject(jsonBuffer);

            String type = json.getString("type", "");
            String address = json.getString("address", "");

            if (type.isEmpty() || address.isEmpty()) {
                throw new IllegalArgumentException("Invalid json type and address should not be empty");
            }

            return json;

        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }



    private void tst() {
        
        BridgeOptions bridgeOptions = new BridgeOptions()
                .addInboundPermitted(new PermittedOptions().setAddress("P2P_IN"))
                .addOutboundPermitted(new PermittedOptions().setAddress("P2P_OUT"));
        NetServerOptions netServerOptions = new NetServerOptions();
        TcpEventBusBridge bridge = TcpEventBusBridge.create(vertx, bridgeOptions, netServerOptions);
        

        bridge.listen(7000, res -> {
            if (res.succeeded()) {
                System.out.println("P2P Listening on 7000");
            } else {

            }

            TcpEventBusBridge tcpBridge = res.result();

        });


        EventBus eventBus = vertx.eventBus();
        eventBus.consumer("P2P_IN")
            .handler(handler -> {
               Object body = handler.body();
               System.out.println("Server Received : " + body.toString());
            });

        vertx.periodicStream(1000).handler(handler -> {
            long longValue = System.currentTimeMillis();
            System.out.println("Server sending long value :" + longValue);
            eventBus.publish("P2P_OUT", new JsonObject().put("long", longValue));
        });
    }

}
