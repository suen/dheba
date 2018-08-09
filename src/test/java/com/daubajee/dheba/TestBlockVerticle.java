package com.daubajee.dheba;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.daubajee.dheba.block.Block;
import com.daubajee.dheba.block.BlockVerticle;
import com.daubajee.dheba.block.Blockchain;
import com.daubajee.dheba.block.msg.BlockHeader;
import com.daubajee.dheba.block.msg.BlockHeaders;
import com.daubajee.dheba.block.msg.BlockMessage;
import com.daubajee.dheba.block.msg.GetBlock;
import com.daubajee.dheba.block.msg.GetHeaders;
import com.daubajee.dheba.block.msg.OneBlock;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;

@ExtendWith(VertxExtension.class)
public class TestBlockVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestBlockVerticle.class);

    Block genesisBlock = Blockchain.genesisBlock();

    String genesisHash = genesisBlock.getHash();

    int geneisHeight = genesisBlock.getIndex();

    BlockHeader genesisHeader = new BlockHeader(geneisHeight, genesisHash);

    @Test
    public void testGetHeaders(Vertx vertx, VertxTestContext testContext) throws Throwable {
        LOGGER.info("TestBlockVerticle.testGetHeaders()");
        Checkpoint checkpoint = testContext.checkpoint(2);

        BlockVerticle blockVerticle = new BlockVerticle();

        EventBus eventBus = vertx.eventBus();

        vertx.deployVerticle(blockVerticle, testContext.succeeding(h -> {
            checkpoint.flag();
        }));

        GetHeaders getHeaders = new GetHeaders(genesisHeader, 10);

        BlockMessage getHeaderReq = new BlockMessage(BlockMessage.GET_HEADERS, getHeaders.toJson());

        ObservableFuture<Message<JsonObject>> blockchainReplyStream = RxHelper.observableFuture();

        eventBus.send(Topic.BLOCK, getHeaderReq.toJson(), blockchainReplyStream.toHandler());

        blockchainReplyStream
            .map(msg -> msg.body())
            .map(json -> BlockMessage.from(json))
            .filter(blockMsg -> blockMsg.getType().equals(BlockMessage.HEADERS))
            .take(1)
            .subscribe(blockMsg -> {
                BlockHeaders blockHeaders = BlockHeaders.from(blockMsg.getContent());
                List<BlockHeader> headers = blockHeaders.getHeaders();
                assertThat(headers.size(), equalTo(1));;
                
                BlockHeader firstHeader = headers.get(0);
                
                assertThat(firstHeader.getHash(), equalTo(genesisHash));
                assertThat(firstHeader.getHeight(), equalTo(geneisHeight));
                checkpoint.flag();
            });

        testContext.awaitCompletion(1, TimeUnit.MINUTES);
    }

    @Test
    public void testGetBlock(Vertx vertx, VertxTestContext testContext) throws Throwable {
        LOGGER.info("TestBlockVerticle.testGetBlock()");
        Checkpoint checkpoint = testContext.checkpoint(2);

        BlockVerticle blockVerticle = new BlockVerticle();

        EventBus eventBus = vertx.eventBus();

        vertx.deployVerticle(blockVerticle, testContext.succeeding(h -> {
            checkpoint.flag();
        }));
        
        GetBlock getBlock = new GetBlock(genesisHeader);
        
        BlockMessage blockMessage = new BlockMessage(BlockMessage.GET_BLOCK, getBlock.toJson());
        
        ObservableFuture<Message<JsonObject>> blockchainReplyStream = RxHelper.observableFuture();

        eventBus.send(Topic.BLOCK, blockMessage.toJson(), blockchainReplyStream.toHandler());

        blockchainReplyStream
            .map(msg -> msg.body())
            .map(json -> BlockMessage.from(json))
            .filter(blockMsg -> blockMsg.getType().equals(BlockMessage.BLOCK))
            .take(1)
            .subscribe(blockMsg -> {
                OneBlock oneBlock = OneBlock.from(blockMsg.getContent());
                Block bcGenesisBlock = oneBlock.getBlock();
                
                assertThat(bcGenesisBlock.getHash(), equalTo(genesisHash));
                assertThat(bcGenesisBlock.getIndex(), equalTo(geneisHeight));
                
                checkpoint.flag();
            });
        
    }


}
