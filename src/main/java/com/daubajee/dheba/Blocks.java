package com.daubajee.dheba;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

import io.reactivex.Observable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Component
public class Blocks {

    private List<Block> blockchain = new ArrayList<Block>();

    public Blocks() {
        blockchain.add(gensisBlock());
    }

    private static final Logger LOGGER = LoggerFactory
            .getLogger(Blocks.class);

    public static Block gensisBlock() {
        return new Block(0, 1465154705,
                "816534932c2b7154836da6afc367695e6337db8a921823784c14378abed4f7d7",
                null, "Here comes the sun");
    }

    public static String calculateHash(int index, String previousHash,
            long timestamp, String data) {
        StringBuffer sb = new StringBuffer();
        sb.append(String.valueOf(index)).append(previousHash)
                .append(String.valueOf(timestamp)).append(data);
        HashCode hashBytes = Hashing.sha256()
                .hashBytes(sb.toString().getBytes());
        return hashBytes.toString();
    }

    public static Block generateNewBlock(String data, Block latestBlock) {
        int index = latestBlock.getIndex() + 1;
        long timestamp = System.currentTimeMillis();
        String previousHash = latestBlock.getHash();
        String hash = calculateHash(index, previousHash, timestamp, data);
        return new Block(index, timestamp, hash, previousHash, data);
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
                newblock.getData());
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

    public List<Block> getBlockchain() {
        return blockchain;
    }

}
