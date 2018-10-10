package com.daubajee.dheba;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.daubajee.dheba.block.Block;
import com.daubajee.dheba.block.BlockUtils;
import com.daubajee.dheba.block.Blockchain;
import com.daubajee.dheba.block.miner.BlockMiner;
import com.daubajee.dheba.block.msg.BlockHeader;

import io.reactivex.Observable;

public class TestBlockchain {

    @Test
    public void test() {
        Blockchain chainA = new Blockchain();
        Blockchain chainB = new Blockchain();

        BlockHeader genesisHash = chainA.getLastHeader();
        assertThat("Genesis hashes are not equal", genesisHash.getHash(),
                equalTo(chainB.getLastHeader().getHash()));

        long diff = 300000L;
        for (int i = 0; i < 15; i++) {
            BlockHeader lastHeader = chainA.getLastHeader();
            Block current = chainA.getBlock(lastHeader).get();
            Function<String, Block> blockLookupChainA = hash -> chainA.getBlock(hash).get();
            long difficultyNextBlock = BlockUtils.getDifficultyForNextBlock(current, blockLookupChainA);
            long ts = current.getTimestamp() + diff;
            Block rawBlock = new Block(current.getIndex() + 1, "", current.getHash(), ts, 0,
                    difficultyNextBlock, "Test Block");
            Block nextBlock = BlockMiner.findNonce(rawBlock, 0, 1_000_000_000_000L)
                .filter(block -> !block.getHash().isEmpty())
                .blockingFirst();
            Optional<BlockHeader> result = chainA.append(nextBlock);
            assertThat("Block rejected", result.isPresent(), is(true));
            System.out.println(Thread.currentThread().getName() + " - " + nextBlock.toJson());
        }

    }

    public static Block nextRawBlock(Block block, long ts, long difficulty, String data) {
        return new Block(block.getIndex() + 1, "", block.getHash(), ts, 0, difficulty, data);
    }

    public static Block nextBlock(Block rawBlock) {
        Observable<Block> nextBlock = BlockMiner.findNonce(rawBlock, 0, 1_000_000_000_000L);
        
        return nextBlock
            .blockingNext()
            .iterator()
            .next();

    }
}
