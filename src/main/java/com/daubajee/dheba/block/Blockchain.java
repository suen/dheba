package com.daubajee.dheba.block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.daubajee.dheba.block.msg.BlockHeader;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class Blockchain {

    private Map<String, Block> blockHashIndex = new HashMap<>();

    private Map<Integer, List<String>> blockHeightIndex = new HashMap<>();

    public Blockchain() {
        Block genesisBlock = genesisBlock();
        blockHashIndex.put(genesisBlock.getHash(), genesisBlock);
        blockHeightIndex.put(genesisBlock.getIndex(), Arrays.asList(genesisBlock.getHash()));
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

        String nextBlockHash = block.getHash();
        while (nextBlockHash != null || replyHeaders.size() <= limit + 1) {
            Block nextBlock = blockHashIndex.get(nextBlockHash);
            BlockHeader blockHeader = new BlockHeader(nextBlock.getIndex(), nextBlockHash);
            replyHeaders.add(blockHeader);
            nextBlockHash = nextBlock.getPreviousHash();
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

        int blockIndex = block.getIndex();
        String blockHash = block.getHash();
        String previousHash = block.getPreviousHash();

        BlockHeader lastHeader = getLastHeader();


        Optional<Block> prevBlockResult = Optional.ofNullable(blockHashIndex.get(previousHash));
        
        if (!prevBlockResult.isPresent()) {
            LOGGER.info("Previous hash of incoming block unknown, block {}", block.toJson());
            return Optional.empty();
        }

        if (blockIndex > lastHeader.getHeight() - BlockConstant.MAX_FORK_INTERVAL) {
            LOGGER.info("Incoming block is at index {}, current index {}", blockIndex, lastHeader.getHeight());
            return Optional.empty();
        }

        Block previousBlock = prevBlockResult.get();

        if (previousBlock.getIndex() != blockIndex + 1) {
            LOGGER.info("Incoming block is at height {} which is more than +1 than the previous block's height {}",
                    block.getIndex(), previousBlock.getIndex());
            return Optional.empty();
        }
        
        if (block.getTimestamp() > previousBlock.getTimestamp()) {
            LOGGER.info("Incoming block is at timestamp {} which is inferior to the previous block's timestamp {}",
                    block.getTimestamp(), previousBlock.getTimestamp());
            return Optional.empty();
        }

        // check difficulty for index
        long difficulty = BlockUtils.getDifficultyForNextBlock(previousBlock, hash -> blockHashIndex.get(hash));
        if (block.getDifficulty() != difficulty) {
            LOGGER.info("Incoming block does not match difficulty level, difficulty expected {}, block {}", difficulty,
                    block.toJson());
            return Optional.empty();
        }

        boolean hashMatchesDifficulty = BlockUtils.hashMatchesDifficulty(blockHash, difficulty);
        if (!hashMatchesDifficulty) {
            LOGGER.info("Incoming block's hash does not match difficulty level, block {}", block.toJson());
            return Optional.empty();
        }

        blockHashIndex.put(blockHash, block);
        blockHeightIndex.computeIfAbsent(blockIndex, index -> new ArrayList<>()).add(blockHash);
        return Optional.of(getLastHeader());
    }

    private static boolean checkHash(Block block) {
        String blocksha256 = BlockUtils.sha256(block.getIndex(), block.getPreviousHash(), block.getTimestamp(),
                block.getData(), block.getDifficulty(), block.getNonce());
        String hash = block.getHash();
        return blocksha256.equals(hash);
    }

    public BlockHeader getLastHeader() {
        int maxHeight = blockHeightIndex.keySet().stream().mapToInt(index -> index).max().orElse(0);
        List<String> hashes = blockHeightIndex.get(maxHeight);
        if (hashes.size() == 1) {
            return new BlockHeader(maxHeight, hashes.get(0));
        }
        // if many block at the highest point are found, we pick the one with
        // oldest timestamp
        String preferredHash = hashes.get(0);
        long maxTimestamp = blockHashIndex.get(preferredHash).getTimestamp();
        for (int i = 1; i < hashes.size(); i++) {
            String hash = hashes.get(i);
            long timestamp = blockHashIndex.get(hash).getTimestamp();
            if (timestamp < maxTimestamp) {
                maxTimestamp = timestamp;
                preferredHash = hash;
            }
        }
        return new BlockHeader(maxHeight, preferredHash);
    }

}
