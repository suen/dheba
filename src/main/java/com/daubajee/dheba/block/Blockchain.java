package com.daubajee.dheba.block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.daubajee.dheba.block.msg.BlockHeader;
import com.google.common.collect.Range;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

import io.reactivex.Observable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class Blockchain {

    private static final int BLOCK_GENERATION_INTERVAL = 10;

    private static final int DIFFICULTY_ADJUSTMENT_INTERVAL = 10;

    private static final int TIMESTAMP_MARGIN = 60000;

    private Map<String, Block> blockHashIndex = new HashMap<>();

    private Map<String, String> previousBlockHashIndex = new HashMap<>();

    private Map<Integer, Block> blockHeightIndex = new HashMap<>();

    private List<Block> chain = new ArrayList<Block>();

    public Blockchain() {
        Block genesisBlock = genesisBlock();
        blockHashIndex.put(genesisBlock.getHash(), genesisBlock);
        blockHeightIndex.put(genesisBlock.getIndex(), genesisBlock);
        chain.add(genesisBlock);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Blockchain.class);

    public static Block genesisBlock() {
        String genesisMsg = "Here comes the sun (doo doo doo doo)";
        String hash = BlockUtils.sha256(0, "", 1515846960488L, genesisMsg, 5, 789735);
        return new Block(0, hash, null, 1515846960488L, 789735, 5, genesisMsg);
    }

    public List<BlockHeader> getHeaders(BlockHeader afterHeader, int limit) {

        Block block = blockHashIndex.get(afterHeader.getHash());
        if (block == null || block.getIndex() != afterHeader.getHeight()) {
            return Collections.emptyList();
        }

        List<BlockHeader> replyHeaders = new ArrayList<>();

        String startingHash = block.getHash();
        String nextBlockHash = startingHash;
        while (nextBlockHash != null && replyHeaders.size() <= limit + 1) {
            Block nextBlock = blockHashIndex.get(nextBlockHash);
            BlockHeader blockHeader = new BlockHeader(nextBlock.getIndex(), nextBlockHash);
            replyHeaders.add(blockHeader);
            nextBlockHash = previousBlockHashIndex.get(startingHash);
        }

        return replyHeaders;
    }

    public Optional<Block> getBlock(BlockHeader blockHeader) {
        Block block = blockHashIndex.get(blockHeader.getHash());
        if (block == null || block.getIndex() != blockHeader.getHeight()) {
            LOGGER.trace("A block with hash {} at height {} was requested, not found", blockHeader.getHeight(),
                    blockHeader.getHeight());
            return Optional.empty();
        }
        return Optional.of(block);
    }

    public Optional<BlockHeader> append(Block block) {
        if (!block.isValid()) {
            LOGGER.info("An invalid block was received, block : {}", block.toJson());
            return Optional.empty();
        }

        boolean hashIsCorrect = checkHash(block);
        if (!hashIsCorrect) {
            LOGGER.info("Hash of incoming block invalid, block : {}", block.toJson());
            return Optional.empty();
        }

        int index = block.getIndex();
        String blockHash = block.getHash();
        String previousHash = block.getPreviousHash();

        BlockHeader lastHeader = getLastHeader();

        Block previousBlock = blockHeightIndex.get(index - 1);

        int previousIndex = Optional.ofNullable(previousBlock.getIndex()).orElse(-1);

        if (!previousHash.equals(previousBlock.getHash())) {
            LOGGER.info("Previous hash of incoming block unknown, block {}", block.toJson());
            return Optional.empty();
        }

        if (previousIndex < lastHeader.getHeight() - 5) {
            LOGGER.info("Incoming block is at index {}, current index {}", index, lastHeader.getHeight());
            return Optional.empty();
        }

        // check difficulty for index

        if (previousHash != null) {
            previousBlockHashIndex.put(previousHash, blockHash);
        }
        blockHashIndex.put(blockHash, block);
        return Optional.of(getLastHeader());
    }

    private static boolean checkHash(Block block) {
        String blocksha256 = BlockUtils.sha256(block.getIndex(), block.getPreviousHash(), block.getTimestamp(),
                block.getData(), block.getDifficulty(), block.getNonce());
        String hash = block.getHash();
        return blocksha256.equals(hash);
    }

    public List<Block> getChain() {
        return chain;
    }

    public BlockHeader getLastHeader() {
        Block block = chain.get(chain.size() - 1);
        return new BlockHeader(block.getIndex(), block.getHash());
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
                .equals(genesisBlock().toJson());
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
        if (isValidChain(newBlockChain) && newBlockChain.size() > chain.size()) {
            chain = newBlockChain;
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
