package com.daubajee.dheba;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;
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
    public void testAppendBlockChains() {
        Blockchain chainA = new Blockchain();
        Blockchain chainB = new Blockchain();
        Blockchain chainC = new Blockchain();

        BlockHeader genesisBlockHeader = chainA.getLastHeader();
        assertThat("Genesis hashes are not equal", genesisBlockHeader.getHash(),
                equalTo(chainB.getLastHeader().getHash()));

        //Simulation of three chains.
        
        long diff = 300000L;
        for (int i = 0; i < 15; i++) {
            BlockHeader lastHeaderA = chainA.getLastHeader();
            Block currentA = chainA.getBlock(lastHeaderA).get();

            Function<String, Block> blockLookupChainA = hash -> chainA.getBlock(hash).get();

            long difficultyNextBlock = BlockUtils.getDifficultyForNextBlock(currentA, blockLookupChainA);
            long ts = currentA.getTimestamp() + diff;
            Block rawBlockA = new Block(currentA.getIndex() + 1, "", currentA.getHash(), ts, 0,
                    difficultyNextBlock, "Test Block");

            Block nextBlockA = BlockMiner.findNonce(rawBlockA, 0, 1_000_000_000_000L)
                .filter(block -> !block.getHash().isEmpty())
                .blockingFirst();

            Optional<BlockHeader> result = chainA.append(nextBlockA);
            assertThat("Block rejected", result.isPresent(), is(true));

            System.out.println(Thread.currentThread().getName() + " - " + nextBlockA.toJson());

            BlockHeader acceptedBlockHeaderA = result.get();
            Optional<Block> getAcceptedBlockA = chainA.getBlock(acceptedBlockHeaderA);
            assertThat("Block lookup failed", getAcceptedBlockA.isPresent(), is(true));

            Optional<Block> nextBlockChainC;
            if (i % 2 == 0) {
                BlockHeader lastHeaderC = chainC.getLastHeader();

                Block rawBlockC = new Block(lastHeaderC.getHeight() + 1, "", lastHeaderC.getHash(), ts, 0,
                        difficultyNextBlock, "Test Block Chain C");

                Block nextBlockC = BlockMiner.findNonce(rawBlockC, 0, 1_000_000_000_000L)
                        .filter(block -> !block.getHash().isEmpty())
                        .blockingFirst();

                Optional<BlockHeader> resultAppendC = chainC.append(nextBlockC);
                assertThat("Block rejected by Chain C", resultAppendC.isPresent(), is(true));

                nextBlockChainC = Optional.of(nextBlockC);
            } else {
                nextBlockChainC = Optional.empty();
            }

            Optional<BlockHeader> resultAppendCA = chainC.append(nextBlockA);
            assertThat("Chain C rejected BlockA", resultAppendCA.isPresent(), is(true));

            Optional<BlockHeader> blockBResult = chainB.append(nextBlockA);
            assertThat("Chain B rejected block", blockBResult.isPresent(), is(true));

            nextBlockChainC.ifPresent(blkC -> {
                Optional<BlockHeader> resultAppendBC = chainB.append(blkC);
                assertThat("C", resultAppendBC.isPresent(), is(true));
            });

        }

        List<BlockHeader> headersA = chainA.getHeaders(genesisBlockHeader, 20);
        List<BlockHeader> headersB = chainB.getHeaders(genesisBlockHeader, 20);
        List<BlockHeader> headersC = chainC.getHeaders(genesisBlockHeader, 20);

        for (int i = 0; i < headersA.size(); i++) {
            BlockHeader headerA = headersA.get(i);
            BlockHeader headerB = headersB.get(i);
            BlockHeader headerC = headersC.get(i);
            boolean aEqualsB = headerA.equals(headerB);
            boolean aEqualsC = headerA.equals(headerC);
            assertThat("Blocks at index " + i  + " on chain A and B not equal", aEqualsB, is(true));
            assertThat("Blocks at index " + i  + " on chain A and C not equal", aEqualsC, is(true));
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
