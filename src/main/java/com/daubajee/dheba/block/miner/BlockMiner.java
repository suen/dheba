package com.daubajee.dheba.block.miner;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.daubajee.dheba.Topic;
import com.daubajee.dheba.block.Block;
import com.daubajee.dheba.block.BlockUtils;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class BlockMiner extends AbstractVerticle {

    private EventBus eventBus;

    private Optional<Block> currentBlock = Optional.empty();

    private static final Logger LOGGER = LoggerFactory.getLogger(BlockMiner.class);

    @Override
    public void start() throws Exception {

        eventBus = vertx.eventBus();

        eventBus.consumer(Topic.BLOCK_MINER, this::onMessage);
        
        minerJob();

    }

    private void onMessage(Message<JsonObject> msg) {
        JsonObject body = msg.body();
        
        BlockMinerMessage blockMinerMsg = BlockMinerMessage.from(body);

        String type = blockMinerMsg.getType();

        JsonObject blockJson = blockMinerMsg.getContent();
        Block block = Block.from(blockJson);
        LOGGER.info("Message of type {} received on {}", type, Topic.BLOCK_MINER);

        switch (type) {
            case BlockMinerMessage.MINE_BLOCK :
                onMineBlock(block);
                break;
            case BlockMinerMessage.BLOCK_FOUND :
                onBlockFoundReceived(block);
                break;
            default :
                break;
        }

    }
    private void onMineBlock(Block block) {
        if (!currentBlock.isPresent() || currentBlock.get().getIndex() <= block.getIndex()) {
            LOGGER.info("A new of block of higher index was received, updating inner currentBlock");
            updateCurrentBlock(block);
        } else {
            LOGGER.info("A new of block of lower index was received, discarding ..");
        }
    }

    private void minerJob() {

        ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder();
        threadFactoryBuilder.setNameFormat("MineThread-%d");
        ThreadFactory threadFactory = threadFactoryBuilder.build();
        ExecutorService executor = Executors.newSingleThreadExecutor(threadFactory);

        Scheduler minerScheduler = Schedulers.from(executor);

        Observable.interval(1, TimeUnit.SECONDS, minerScheduler)
            .filter(tick -> currentBlock.isPresent())
            .map(tick -> currentBlock.get())
            .flatMap(rawBlock -> {
                    return findNonce(rawBlock, rawBlock.getNonce(), 10000L);
            })
            .doOnNext(block -> LOGGER.info("Block #{} - mined : {}, nonce : {}", block.getIndex(), block.isValid(), block.getNonce()))
            .flatMap(block -> {
                if (block.isValid()) {
                    return Observable.just(block);
                } else {
                        block.setNonce(block.getNonce() + 10000L);
                    updateCurrentBlock(block);
                    return Observable.empty();
                }
            })
            .subscribe(block -> {
                onBlockFound(block);
            });
    }

    private void onBlockFoundReceived(Block block) {
        if (currentBlock.isPresent() && currentBlock.get().getIndex() <= block.getIndex()) {
            updateCurrentBlock(block);
        }
    }

    private synchronized void updateCurrentBlock(Block block) {
        if (currentBlock.isPresent() 
                && currentBlock.get().getIndex() <= block.getIndex()
                && block.isValid()) {
            currentBlock = Optional.empty();
        } else if (!currentBlock.isPresent()) {
            currentBlock = Optional.of(block);
        } else if (currentBlock.get().getIndex() <= block.getIndex() 
                && !block.isValid()) {
            currentBlock = Optional.of(block);
        }
    }

    private void onBlockFound(Block block) {
        LOGGER.info("Block found : {}", block.toJson());
        BlockMinerMessage msg = new BlockMinerMessage(BlockMinerMessage.BLOCK_FOUND, block.toJson());
        eventBus.publish(Topic.BLOCK_MINER, msg.toJson());
        updateCurrentBlock(block);
    }

    public static Observable<Block> findNonce(Block rawBlock, long startNonce, long range) {
    
        int index = rawBlock.getIndex();
        String previousHash = rawBlock.getPreviousHash();
        long timestamp = rawBlock.getTimestamp();
        String data = rawBlock.getData();
        long difficulty = rawBlock.getDifficulty();
        long endNonce = startNonce + range;
    
        return Observable.create(source -> {
            long nonce = startNonce;
            Block lastBlock = null;
            while (nonce <= endNonce) {
                String hash = BlockUtils.sha256(index, previousHash, timestamp, data, difficulty, nonce);
                if (BlockUtils.hashMatchesDifficulty(hash, difficulty)) {
                    lastBlock = new Block(index, hash, previousHash, timestamp, nonce, difficulty, data);
                    break;
                }
                nonce++;
            }
            if (lastBlock == null) {
                lastBlock = new Block(index, "", previousHash, timestamp, nonce, difficulty, data);
            }
            source.onNext(lastBlock);
            source.onComplete();
        });
    }



}
