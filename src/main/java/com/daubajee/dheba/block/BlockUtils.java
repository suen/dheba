package com.daubajee.dheba.block;

import java.util.function.Function;

import com.google.common.collect.Range;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

public final class BlockUtils {
    
    public static String sha256(int index, String previousHash,
            long timestamp, String data, long difficulty, long nonce) {
        StringBuffer sb = new StringBuffer();
        sb.append(String.valueOf(index))
                .append(previousHash)
                .append(timestamp)
                .append(difficulty)
                .append(data)
                .append(nonce);
        HashCode hashBytes = Hashing.sha256()
                .hashBytes(sb.toString().getBytes());
        return hashBytes.toString();
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

    public static long getDifficulty(int blockchainLength, Function<Integer, Block> blockAccessor) {
        Block latestBlock = blockAccessor.apply(blockchainLength - 1);

        if (blockchainLength % BlockConstant.DIFFICULTY_ADJUSTMENT_INTERVAL == 0 && blockchainLength > 0) {
            int lastAdjustedIndex = blockchainLength - blockchainLength % BlockConstant.DIFFICULTY_ADJUSTMENT_INTERVAL;
            Block previousAdjustedBlock = blockAccessor.apply(lastAdjustedIndex);
            return getAdjustedDifficulty(blockchainLength, latestBlock, previousAdjustedBlock);
        }

        return latestBlock.getDifficulty();
    }

    public static long getAdjustedDifficulty(int blockchainLength, Block latestBlock, Block previousAdjustedBlock) {

        int estimatedTimeInMin = BlockConstant.BLOCK_GENERATION_INTERVAL * BlockConstant.DIFFICULTY_ADJUSTMENT_INTERVAL;

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
}
