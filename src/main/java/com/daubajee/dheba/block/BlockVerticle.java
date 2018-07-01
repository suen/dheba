package com.daubajee.dheba.block;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.daubajee.dheba.MsgUtils;
import com.daubajee.dheba.Topic;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class BlockVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlockVerticle.class);

    public static final String GET_ALL_BLOCKS = "GET_ALL_BLOCKS";

    public static final String MINE_NEW_BLOCK = "MINE_NEW_BLOCK";

    private Blocks blocks;

    @Override
    public void start() throws Exception {
        blocks = new Blocks();

        EventBus eventBus = vertx.eventBus();

        eventBus.consumer(Topic.BLOCK, this::handleMessage);
    }

    private void handleMessage(Message<JsonObject> msg) {
        vertx.executeBlocking(handler -> {
            onMessage(msg);
            handler.complete();
        }, result -> {

        });
    }

    private void onMessage(Message<JsonObject> msg) {
        JsonObject msgBody = msg.body();
        String request = msgBody.getString("REQUEST");
        String params = msgBody.getString("PARAMS", "");

        switch (request) {
            case GET_ALL_BLOCKS :
                handleGetAllBlocks(msg::reply);
                break;
            case MINE_NEW_BLOCK :
                handleMineNewBlock(params, msg::reply);
                break;
            default :
                LOGGER.info("unknown Request" + request);
                break;
        }
    }


    private void handleMineNewBlock(String data, Consumer<Object> object) {
        List<Block> blockchain = blocks.getBlockchain();
        Block latestBlock = blockchain.get(blockchain.size() - 1);
        long difficulty = Blocks.getDifficulty(blockchain);
        long currentTimestamp = System.currentTimeMillis();
        Block newBlock = Blocks.findNewBlock(latestBlock.getIndex() + 1, latestBlock.getHash(), currentTimestamp, data,
                difficulty);
        blockchain.add(newBlock);

        JsonObject reply = MsgUtils.createReply("OK");
        object.accept(reply);
    }

    private void handleGetAllBlocks(Consumer<Object> consumer) {
        List<Block> blockchain = blocks.getBlockchain();
        List<JsonObject> jsonBlocks = blockchain.stream()
            .map(block -> block.toJson())
            .collect(Collectors.toList());
        JsonArray jsonArray = new JsonArray(jsonBlocks);
        String jsonArrayStr = jsonArray.toString();

        JsonObject reply = MsgUtils.createReply("OK", jsonArrayStr);
        consumer.accept(reply);
    }

}
