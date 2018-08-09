package com.daubajee.dheba.block;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.daubajee.dheba.Topic;
import com.daubajee.dheba.block.msg.BlockHeader;
import com.daubajee.dheba.block.msg.BlockHeaders;
import com.daubajee.dheba.block.msg.BlockMessage;
import com.daubajee.dheba.block.msg.GetBlock;
import com.daubajee.dheba.block.msg.GetHeaders;
import com.daubajee.dheba.block.msg.OneBlock;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class BlockVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlockVerticle.class);

    private Blockchain blockchain;

    @Override
    public void start() throws Exception {
        blockchain = new Blockchain();

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
                onGetBlock(blockMsg.getContent())
                    .ifPresent(oneBlock -> {
                        BlockMessage getBlockReply = new BlockMessage(BlockMessage.BLOCK, oneBlock.toJson());
                        msg.reply(getBlockReply.toJson());
                    });
                break;
            case BlockMessage.BLOCK :
                BlockHeader header = onIncomingBlock(blockMsg.getContent());
                msg.reply(header.toJson());
            default :
                LOGGER.warn("Unknown type {}", type);
                break;
        }
    }


    private BlockHeader onIncomingBlock(JsonObject content) {

        OneBlock oneBLock = OneBlock.from(content);

        Block block = oneBLock.getBlock();
        String hash = block.getHash();
        // let's suppose the block gets accepted
        BlockHeader header = new BlockHeader(block.getIndex(), block.getHash());

        return header;
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

    private Optional<OneBlock> onGetBlock(JsonObject requestJson) {

        GetBlock getBlockReq = GetBlock.from(requestJson);
        if (!getBlockReq.isValid()) {
            LOGGER.error("GetBlock request invalid, content : {}", requestJson);
            return Optional.empty();
        }

        Block gensisBlock = Blockchain.genesisBlock();
        OneBlock blockReply = new OneBlock(gensisBlock);
        return Optional.of(blockReply);
    }

}
