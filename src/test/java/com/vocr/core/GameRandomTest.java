package com.vocr.core;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameRandomTest
{
    @RepeatedTest(100)
    void probabilityJudge_100_shouldAlwaysReturnTrue()
    {
        assertTrue(new GameRandom().probabilityJudge(100), "100%概率应始终返回true");
    }

    @RepeatedTest(100)
    void probabilityJudge_0_shouldAlwaysReturnFalse()
    {
        assertFalse(new GameRandom().probabilityJudge(0), "0%概率应始终返回false");
    }

    @Test
    void weightedIndex_singleItem_shouldReturn0()
    {
        assertEquals(0, new GameRandom(1L).weightedIndex(List.of(50)), "单项应返回索引0");
    }

    @Test
    void weightedIndex_zeroWeight_shouldThrow()
    {
        assertThrows(IllegalArgumentException.class,
                () -> new GameRandom(1L).weightedIndex(List.of(0)),
                "零权重应抛IllegalArgumentException");
    }

    @Test
    void shuffledCopy_sameSeed_shouldProduceSameOrderAndChecksum()
    {
        ArrayList<Integer> input = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6));

        GameRandom firstRandom = new GameRandom(20260904L);
        firstRandom.startRecording();
        ArrayList<Integer> first = firstRandom.shuffledCopy(input);
        String firstChecksum = firstRandom.getRecordedChecksum();

        GameRandom secondRandom = new GameRandom(20260904L);
        secondRandom.startRecording();
        ArrayList<Integer> second = secondRandom.shuffledCopy(input);
        String secondChecksum = secondRandom.getRecordedChecksum();

        assertEquals(first, second);
        assertEquals(firstChecksum, secondChecksum);
        assertEquals(List.of(1, 2, 3, 4, 5, 6), input, "洗牌不得修改调用方列表");
        assertTrue(firstChecksum.matches("size=5 sha256=[0-9a-f]{64}"));
    }
}
