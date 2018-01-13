package com.daubajee.dheba;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.google.common.collect.Range;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

import io.reactivex.Observable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Component
public class Blocks {

    private static final int BLOCK_GENERATION_INTERVAL = 10;

    private static final int DIFFICULTY_ADJUSTMENT_INTERVAL = 10;

    private static final int TIMESTAMP_MARGIN = 60000;

    private List<Block> blockchain = new ArrayList<Block>();

    public Blocks() {
        blockchain.add(gensisBlock());
    }

    private static final Logger LOGGER = LoggerFactory
            .getLogger(Blocks.class);

    public static Block gensisBlock() {
        String hash = calculateHash(0, null, 1515846670488L, "Here comes the sun", 5328471, 6);
        return new Block(0, hash, null, 1515846670488L, 5328471, 6, "Here comes the sun");
    }

    public List<Block> getBlockchain() {
        return blockchain;
    }

    public static String calculateHash(int index, String previousHash,
            long timestamp, String data, long difficulty, long nonce) {
        StringBuffer sb = new StringBuffer();
        sb.append(String.valueOf(index))
                .append(previousHash)
                .append(timestamp)
                .append(nonce)
                .append(difficulty)
                .append(data);
        HashCode hashBytes = Hashing.sha256()
                .hashBytes(sb.toString().getBytes());
        return hashBytes.toString();
    }

    public static Block generateNewBlock(String data, Block latestBlock, long timestamp, long difficulty, long nonce) {
        int index = latestBlock.getIndex() + 1;
        String previousHash = latestBlock.getHash();
        String hash = calculateHash(index, previousHash, timestamp, data, difficulty, nonce);
        return new Block(index, hash, previousHash, timestamp, nonce, difficulty, data);
    }

    public static Block findNewBlock(int index, String previousHash, long timestamp, String data, long difficulty) {
        long nonce = 0;
        while (true) {
            String hash = calculateHash(index, previousHash, timestamp, data, difficulty, nonce);
            if (hashMatchesDifficulty(hash, difficulty)) {
                return new Block(index, hash, previousHash, timestamp, nonce, difficulty, data);
            }
            nonce++;
        }
    }

    public static boolean isValidNewBlock(Block newblock, Block previousBlock) {
        if (previousBlock.getIndex() + 1 != newblock.getIndex()) {
            return false;
        }
        else if (!previousBlock.getHash().equals(newblock.getPreviousHash())) {
            return false;
        }
        String newBlockHash = calculateHash(newblock.getIndex(),
                newblock.getPreviousHash(), newblock.getTimestamp(),
                newblock.getData(), newblock.getDifficulty(), newblock.getNonce());
        if (!newblock.getHash().equals(newBlockHash)) {
            return false;
        }

        return true;
    }

    public static boolean isValidChain(List<Block> blockChain) {
        Block chainGenesisBlock = blockChain.get(0);

        boolean sameGensis = chainGenesisBlock.toJson()
                .equals(gensisBlock().toJson());
        if (!sameGensis) {
            return false;
        }
        
        Observable<Block> blockStream = Observable.fromIterable(blockChain);
        Observable<Block> blockStreamSkipOne = blockStream.skip(1);

        Boolean valid = Observable
                .zip(blockStream, blockStreamSkipOne,
                        (latest, newBlock) -> isValidNewBlock(newBlock, latest))
                .takeWhile(r -> r)
                .defaultIfEmpty(true)
                .blockingFirst();
        return valid;
    }
    
    public void replaceChain(List<Block> newBlockChain) {
        if (isValidChain(newBlockChain) && newBlockChain.size() > blockchain.size()) {
            blockchain = newBlockChain;
        } else {
            LOGGER.warn("Invalid newBlockchain received");
        }
    }

    public static boolean hashMatchesDifficulty(String hash, long difficulty) {
        HashCode hashcode = HashCode.fromString(hash);
        byte[] hashbytes = hashcode.asBytes();
        if (hashbytes.length < difficulty) {
            return false;
        }

        int pos = 0;
        byte[] cmp = new byte[]{0x0f, (byte) 0xf0};
        while (pos < difficulty) {
            int index = pos / 2;
            int offset = pos % 2;
            byte byt = hashbytes[index];
            boolean equal = (byt | cmp[offset]) == cmp[offset];
            if (!equal) {
                return false;
            }
            pos++;
        }
        return true;

    }

    public static long getDifficulty(List<Block> blockchain) {
        int blockchainLength = blockchain.size();
        if (blockchainLength % DIFFICULTY_ADJUSTMENT_INTERVAL == 0 && blockchainLength > 0) {
            return getAdjustedDifficulty(blockchain);
        }
        Block latestBlock = blockchain.get(blockchainLength - 1);
        return latestBlock.getDifficulty();
    }

    public static long getAdjustedDifficulty(List<Block> blockchain) {
        int blockchainLength = blockchain.size();
        Block latestBlock = blockchain.get(blockchainLength - 1);

        int lastAdjustedIndex = blockchainLength - blockchainLength % DIFFICULTY_ADJUSTMENT_INTERVAL;
        Block previousAdjustedBlock = blockchain.get(lastAdjustedIndex - 1);

        int estimatedTimeInMin = BLOCK_GENERATION_INTERVAL * DIFFICULTY_ADJUSTMENT_INTERVAL;

        long timeInMsBetweenDifficultAdjust = latestBlock.getTimestamp() - previousAdjustedBlock.getTimestamp();
        long timeInMinBetweenDifficultAdjust = timeInMsBetweenDifficultAdjust / (6000);

        long deltaInMin = timeInMinBetweenDifficultAdjust - estimatedTimeInMin;

        long currentDifficulty = latestBlock.getDifficulty();

        Range<Long> allowed = Range.closed(-1L, 1L);

        if (allowed.contains(deltaInMin)) {
            return currentDifficulty;
        }

        if (deltaInMin < 1) {
            return currentDifficulty + 1;
        } else {
            return currentDifficulty - 1;
        }
    }
    
    public static boolean isValidTimestamp(Block newBlock, Block previousBlock) {
        return newBlock.getTimestamp() - TIMESTAMP_MARGIN < System.currentTimeMillis()
                && previousBlock.getTimestamp() - TIMESTAMP_MARGIN < newBlock.getTimestamp();
    }

}
