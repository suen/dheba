package com.daubajee.dheba.block;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.daubajee.dheba.MsgUtils;
import com.daubajee.dheba.Topic;
import com.daubajee.dheba.block.msg.BlockHeader;
import com.daubajee.dheba.block.msg.BlockHeaders;
import com.daubajee.dheba.block.msg.BlockMessage;
import com.daubajee.dheba.block.msg.GetHeaders;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class BlockVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlockVerticle.class);

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

        BlockMessage blockMsg = BlockMessage.from(msgBody);
        if (!blockMsg.isValid()) {
            LOGGER.error("Invalid message on {}, content : {}", Topic.BLOCK, msgBody);
            return;
        }

        String type = blockMsg.getType();

        switch (type) {
            case BlockMessage.GET_HEADERS :
                BlockHeaders headers = onGetHeaders(blockMsg.getContent());
                BlockMessage getHeadersReply = new BlockMessage(BlockMessage.HEADERS, headers.toJson());
                msg.reply(getHeadersReply.toJson());
                break;
            case BlockMessage.GET_BLOCK :
                onGetBlock(blockMsg.getContent());
                break;
            default :
                LOGGER.warn("Unknown type {}", type);
                break;
        }
    }


    private BlockHeaders onGetHeaders(JsonObject requestJson) {

        GetHeaders getHeadersReq = GetHeaders.from(requestJson);
        if (!getHeadersReq.isValid()) {
            LOGGER.error("GetHeader request invalid, content : {}", requestJson);
            return new BlockHeaders(Collections.emptyList());
        }

        BlockHeader after = getHeadersReq.getAfter();

        BlockHeaders blockHeaders = new BlockHeaders(Arrays.asList(after));

        return blockHeaders;
    }

    private void onGetBlock(JsonObject requestJson) {

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
