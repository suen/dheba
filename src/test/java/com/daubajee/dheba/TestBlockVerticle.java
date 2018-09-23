package com.daubajee.dheba;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.daubajee.dheba.block.Block;
import com.daubajee.dheba.block.BlockVerticle;
import com.daubajee.dheba.block.Blockchain;
import com.daubajee.dheba.block.miner.BlockMiner;
import com.daubajee.dheba.block.miner.BlockMinerMessage;
import com.daubajee.dheba.block.msg.BlockHeader;
import com.daubajee.dheba.block.msg.BlockHeaders;
import com.daubajee.dheba.block.msg.BlockMessage;
import com.daubajee.dheba.block.msg.GetBlock;
import com.daubajee.dheba.block.msg.GetHeaders;
import com.daubajee.dheba.block.msg.OneBlock;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.core.eventbus.Message;

@ExtendWith(VertxExtension.class)
public class TestBlockVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestBlockVerticle.class);

    Block genesisBlock = Blockchain.genesisBlock();

    String genesisHash = genesisBlock.getHash();

    int geneisHeight = genesisBlock.getIndex();

    BlockHeader genesisHeader = new BlockHeader(geneisHeight, genesisHash);

    List<String> transactions = Arrays.asList("Here comes the sun",
            "Here comes the sun, and I say", 
            "It's all right",
            "Little darling, it's been a long cold lonely winter",
            "Little darling, it feels like years since it's been here",
            "Here comes the sun",
            "Here comes the sun, and I say",
            "It's all right");
    List<Integer> nonces = Arrays.asList(0, 3000000, 2100000, 160000, 400000, 0, 0);

    List<Block> blocks = new ArrayList<>();

    public void preMineBlocks(Vertx vertx, VertxTestContext testContext, Block lastBlock, int maxIndex) throws Exception {
        Checkpoint checkpoint = testContext.checkpoint(1 + (maxIndex - lastBlock.getIndex()));

        EventBus eventBus = vertx.eventBus();

        vertx.deployVerticle(BlockMiner.class.getName(), testContext.succeeding(h -> {
            checkpoint.flag();
        }));

        PublishSubject<Integer> tickStream = PublishSubject.create();
        
        tickStream
            .takeWhile(index -> index != -1)
            .subscribe(index -> {
                String transaction = transactions.get(index);
                int initialNonce = nonces.get(index);
                long blockts = Instant.ofEpochMilli(lastBlock.getTimestamp()).plusSeconds(290).toEpochMilli();
                Block rawBlock = new Block(index, "", lastBlock.getHash(), blockts, initialNonce, 5, transaction);
                
                Observable<Message<JsonObject>> blockMinerStream = eventBus.<JsonObject>consumer(Topic.BLOCK_MINER).toObservable();
                AtomicReference<Block> minedBlockContainer = new AtomicReference<Block>();
                blockMinerStream
                    .map(msg -> msg.body())
                    .map(json -> BlockMinerMessage.from(json))
                    .filter(blockMsg -> blockMsg.getType().equals(BlockMinerMessage.BLOCK_FOUND))
                    .take(1)
                    .subscribe(blockMsg -> {
                        Block block = Block.from(blockMsg.getContent());
                        assertThat(block.getIndex(), equalTo(index));
                        System.out.println(blockMsg.toJson());
                        assertThat(block.getNonce() > 0L, is(true));
                        minedBlockContainer.set(block);
                        blocks.add(block);
                        checkpoint.flag();
                        if (block.getIndex() != maxIndex) {
                            tickStream.onNext(block.getIndex() + 1);
                        } else {
                            tickStream.onNext(-1);
                        }
                    });
                
                BlockMinerMessage mineBlockMsg = new BlockMinerMessage(BlockMinerMessage.MINE_BLOCK, rawBlock.toJson());
                
                eventBus.publish(Topic.BLOCK_MINER, mineBlockMsg.toJson());
        });

        tickStream.onNext(1);

        testContext.awaitCompletion(1, TimeUnit.MINUTES);
    }

    @Test
    public void testGetHeaders(io.vertx.core.Vertx coreVertx, VertxTestContext testContext) throws Throwable {
        Vertx vertx = Vertx.newInstance(coreVertx);
        LOGGER.info("TestBlockVerticle.testGetHeaders()");

        preMineBlocks(vertx, testContext, genesisBlock, 4);

        Checkpoint checkpoint = testContext.checkpoint(2);

        EventBus eventBus = vertx.eventBus();

        GetHeaders getHeaders = new GetHeaders(genesisHeader, 10);

        BlockMessage getHeaderReq = new BlockMessage(BlockMessage.GET_HEADERS, getHeaders.toJson());

        Observable<Message<JsonObject>> blockchainReplyStream = vertx.rxDeployVerticle(BlockVerticle.class.getName())
            .toObservable()
            .doOnNext(deploymentId -> {
                checkpoint.flag();
            })
            .flatMap(id -> {
                Observable<Message<JsonObject>> observable;
                observable = eventBus.<JsonObject>rxSend(Topic.BLOCK, getHeaderReq.toJson()).toObservable();
                return observable;
            });

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
    public void testGetGenesisBlock(io.vertx.core.Vertx coreVertx, VertxTestContext testContext) throws Throwable {
        Vertx vertx = Vertx.newInstance(coreVertx);

        LOGGER.info("TestBlockVerticle.testGetBlock()");
        Checkpoint checkpoint = testContext.checkpoint(3);

        EventBus eventBus = vertx.eventBus();

        AtomicReference<String> deployementId = new AtomicReference<>();
        vertx.deployVerticle(BlockVerticle.class.getName(), testContext.succeeding(h -> {
            checkpoint.flag();
            deployementId.set(h);
        }));
        
        GetBlock getBlock = new GetBlock(genesisHeader);
        
        BlockMessage blockMessage = new BlockMessage(BlockMessage.GET_BLOCK, getBlock.toJson());
        
        Observable<Message<JsonObject>> blockchainReplyStream = vertx.rxDeployVerticle(BlockVerticle.class.getName())
                .toObservable().doOnNext(deploymentId -> {
                    checkpoint.flag();
                }).flatMap(id -> {
                    Observable<Message<JsonObject>> observable;
                    observable = eventBus.<JsonObject>rxSend(Topic.BLOCK, blockMessage.toJson()).toObservable();
                    return observable;
                });

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
