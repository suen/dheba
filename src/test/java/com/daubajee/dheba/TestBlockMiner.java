package com.daubajee.dheba;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.daubajee.dheba.block.Block;
import com.daubajee.dheba.block.miner.BlockMiner;
import com.daubajee.dheba.block.miner.BlockMinerMessage;

import io.reactivex.Observable;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.core.eventbus.Message;

@ExtendWith(VertxExtension.class)
public class TestBlockMiner {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlockMiner.class);

    @Test
    public void mineGenesisBlock(io.vertx.core.Vertx coreVertx, VertxTestContext testContext) throws Throwable {
        Vertx vertx = Vertx.newInstance(coreVertx);

        LOGGER.info("TestBlockMiner.mineGenesisBlock()");
        Checkpoint checkpoint = testContext.checkpoint(2);

        EventBus eventBus = vertx.eventBus();

        Block rawGenesisBlock = new Block(0, "", "", 1531163330608L, 700075, 5, "Here comes the sun");

        Observable<Message<JsonObject>> blockMinerStream = eventBus.<JsonObject>consumer(Topic.BLOCK_MINER)
                .toObservable();
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
        

        BlockMinerMessage mineBlockMsg = new BlockMinerMessage(BlockMinerMessage.MINE_BLOCK, rawGenesisBlock.toJson());

        vertx.deployVerticle(BlockMiner.class.getName(), testContext.succeeding(h -> {
            eventBus.publish(Topic.BLOCK_MINER, mineBlockMsg.toJson());
            checkpoint.flag();
        }));

        testContext.awaitCompletion(1, TimeUnit.MINUTES);
    }

    @Test
    public void testInterruptMining(io.vertx.core.Vertx coreVertx, VertxTestContext testContext) throws Throwable {
        Vertx vertx = Vertx.newInstance(coreVertx);

        LOGGER.info("TestBlockMiner.testInterruptMining()");
        Checkpoint checkpoint = testContext.checkpoint(2);

        EventBus eventBus = vertx.eventBus();

        vertx.deployVerticle(BlockMiner.class.getName(), testContext.succeeding(h -> {
            checkpoint.flag();
        }));

        Block rawGenesisBlock1 = new Block(0, "", "", 1531163330608L, 0, 5, "Here comes the sun");
        Block rawfakeGenesisBlock2 = new Block(1, "",
                "000001344b4a8975c8f1a32315ca878efdea70d6b1787d2342f933b636368541", 1531163330608L, 941172, 5,
                "Here comes another one");

        Observable<Message<JsonObject>> blockMinerStream = eventBus.<JsonObject>consumer(Topic.BLOCK_MINER)
                .toObservable();
        blockMinerStream
            .map(msg -> msg.body())
            .map(json -> BlockMinerMessage.from(json))
            .filter(blockMsg -> blockMsg.getType().equals(BlockMinerMessage.BLOCK_FOUND))
            .take(1)
            .subscribe(blockMsg -> {
                Block block = Block.from(blockMsg.getContent());
                assertThat(block.getIndex(), equalTo(1));
    
                assertThat(block.getNonce() > 0L, is(true));
                checkpoint.flag();
            });
        
        

        BlockMinerMessage mineBlockMsg1 = new BlockMinerMessage(BlockMinerMessage.MINE_BLOCK, rawGenesisBlock1.toJson());
        eventBus.publish(Topic.BLOCK_MINER, mineBlockMsg1.toJson());
        
        Observable.timer(2, TimeUnit.SECONDS)
            .subscribe(tick -> {
                BlockMinerMessage mineBlockMsg2 = new BlockMinerMessage(BlockMinerMessage.MINE_BLOCK, rawfakeGenesisBlock2.toJson());
                eventBus.publish(Topic.BLOCK_MINER, mineBlockMsg2.toJson());
            });

        testContext.awaitCompletion(5, TimeUnit.MINUTES);
    }
    
    @Test
    public void testInformBlockFound(io.vertx.core.Vertx coreVertx, VertxTestContext testContext) throws Throwable {
        Vertx vertx = Vertx.newInstance(coreVertx);
        LOGGER.info("TestBlockMiner.testInformBlockFound()");
        Checkpoint checkpoint = testContext.checkpoint(2);
        
        EventBus eventBus = vertx.eventBus();
        
        vertx.deployVerticle(BlockMiner.class.getName(), testContext.succeeding(h -> {
            checkpoint.flag();
        }));
        
        Block rawGenesisBlock = new Block(0, "", "", 1531163330608L, 0, 5, "Here comes the sun");
        
        Block genesisBlock = new Block(0, "000001344b4a8975c8f1a32315ca878efdea70d6b1787d2342f933b636368541", "",
                1531163330608L, 721375, 5, "Here comes the sun");
        
        Block nextRawBlock = new Block(1, "",
                "000001344b4a8975c8f1a32315ca878efdea70d6b1787d2342f933b636368541", 1531163330608L, 941172, 5,
                "Here comes another one");
        
        Observable<Message<JsonObject>> blockMinerStream = eventBus.<JsonObject>consumer(Topic.BLOCK_MINER)
                .toObservable();

        blockMinerStream
            .map(msg -> msg.body())
            .map(json -> BlockMinerMessage.from(json))
            .filter(blockMsg -> blockMsg.getType().equals(BlockMinerMessage.BLOCK_FOUND))
            .skip(1)
            .take(1)
            .subscribe(blockMsg -> {
                Block block = Block.from(blockMsg.getContent());
                assertThat(block.getIndex(), equalTo(1));
                
                assertThat(block.getNonce() > 0L, is(true));
                checkpoint.flag();
            });
        
        
        
        BlockMinerMessage mineBlockMsg1 = new BlockMinerMessage(BlockMinerMessage.MINE_BLOCK, rawGenesisBlock.toJson());
        eventBus.publish(Topic.BLOCK_MINER, mineBlockMsg1.toJson());
        
        Observable.timer(2, TimeUnit.SECONDS)
        .subscribe(tick -> {
            BlockMinerMessage genesisBlockFoundMsg = new BlockMinerMessage(BlockMinerMessage.BLOCK_FOUND,
                            genesisBlock.toJson());
            eventBus.publish(Topic.BLOCK_MINER, genesisBlockFoundMsg.toJson());
        });

        Observable.timer(4, TimeUnit.SECONDS).subscribe(tick -> {
            BlockMinerMessage nextBlockMineMsg = new BlockMinerMessage(BlockMinerMessage.MINE_BLOCK,
                    nextRawBlock.toJson());
            eventBus.publish(Topic.BLOCK_MINER, nextBlockMineMsg.toJson());
        });
        
        testContext.awaitCompletion(3, TimeUnit.MINUTES);
    }
}
