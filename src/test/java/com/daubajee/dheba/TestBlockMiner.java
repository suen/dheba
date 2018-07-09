package com.daubajee.dheba;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.daubajee.dheba.block.Block;
import com.daubajee.dheba.block.miner.BlockMiner;
import com.daubajee.dheba.block.miner.BlockMinerMessage;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rx.java.ObservableHandler;
import io.vertx.rx.java.RxHelper;

@ExtendWith(VertxExtension.class)
public class TestBlockMiner {


    @Test
    public void testRegistry(Vertx vertx, VertxTestContext testContext) throws Throwable {

        Checkpoint checkpoint = testContext.checkpoint(2);

        BlockMiner blockMiner = new BlockMiner();

        EventBus eventBus = vertx.eventBus();

        vertx.deployVerticle(blockMiner, testContext.succeeding(h -> {
            checkpoint.flag();
        }));

        Block rawGenesisBlock = new Block(0, "", "", 1531163330608L, 700075, 5, "Here comes the sun");

        ObservableHandler<Message<JsonObject>> blockMinerStream = RxHelper.observableHandler(true);
        blockMinerStream
            .map(msg -> msg.body())
            .map(json -> BlockMinerMessage.from(json))
            .filter(blockMsg -> blockMsg.getType().equals(BlockMinerMessage.BLOCK_FOUND))
            .take(1)
            .subscribe(blockMsg -> {
                Block block = Block.from(blockMsg.getContent());
                assertThat(block.getIndex(), equalTo(0));

                assertThat(block.getNonce() > 0L, is(true));
                checkpoint.flag();
            });
        
        eventBus.consumer(Topic.BLOCK_MINER, blockMinerStream.toHandler());

        BlockMinerMessage mineBlockMsg = new BlockMinerMessage(BlockMinerMessage.MINE_BLOCK, rawGenesisBlock.toJson());

        eventBus.publish(Topic.BLOCK_MINER, mineBlockMsg.toJson());

        testContext.awaitCompletion(5, TimeUnit.MINUTES);
    }

}
