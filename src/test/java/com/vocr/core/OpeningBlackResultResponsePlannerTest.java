package com.vocr.core;

import com.vocr.domain.IntPair;
import com.vocr.domain.peiyi;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpeningBlackResultResponsePlannerTest
{
    @Test
    void collectResponses_shouldDeduplicateBlackTargetsAndUseActualVillageRole()
    {
        GameContext context = createContext();
        int seer = context.getActualRoleIndex(1);
        int secondClaimedSeer = context.getActualRoleIndex(2);
        int villager = context.getActualRoleIndex(6);
        markBlackResult(context, seer, villager);
        markBlackResult(context, secondClaimedSeer, villager);
        List<IntPair> responses = new ArrayList<>();

        context.getRandom().startRecording();
        new OpeningBlackResultResponsePlanner(context)
                .collectResponses(context.getPlayerSum(), responses);

        assertEquals(List.of(new IntPair(villager, 6)), responses);
        assertTrue(context.getRandom().stopRecording().isEmpty());
    }

    @Test
    void collectResponses_shouldPreserveExistingWolfClaimPlanWithoutRandomChoice()
    {
        GameContext context = createContext();
        int wolf = context.getRlIndex(1);
        context.setNonHumanPlan(wolf, NonHumanPlan.CLAIM_MEDIUM);
        markBlackResult(context, context.getActualRoleIndex(1), wolf);
        List<IntPair> responses = new ArrayList<>();

        context.getRandom().startRecording();
        new OpeningBlackResultResponsePlanner(context)
                .collectResponses(context.getPlayerSum(), responses);

        assertEquals(List.of(new IntPair(wolf, 2)), responses);
        assertEquals(NonHumanPlan.CLAIM_MEDIUM, context.getNonHumanPlan(wolf));
        assertTrue(context.getRandom().stopRecording().isEmpty());
    }

    @Test
    void collectResponses_shouldKeepHiddenWolfRandomSequenceStable()
    {
        GameContext context = createContext();
        int wolf = context.getRlIndex(1);
        markBlackResult(context, context.getActualRoleIndex(1), wolf);
        List<IntPair> responses = new ArrayList<>();
        context.getRandom().reseedForTest(0L);

        context.getRandom().startRecording();
        new OpeningBlackResultResponsePlanner(context)
                .collectResponses(context.getPlayerSum(), responses);

        assertEquals(List.of(new IntPair(wolf, 1)), responses);
        assertEquals(List.of(0), context.getRandom().stopRecording());
        assertEquals(NonHumanPlan.NONE, context.getNonHumanPlan(wolf));
        assertEquals(0, context.getSkillTarget(wolf, 1));
    }

    @Test
    void collectResponses_shouldKeepBlackPlannedMediumWolfWhenAnotherWolfRespondsAsMedium()
    {
        GameContext context = createContext();
        int respondingWolf = context.getRlIndex(1);
        int plannedMediumWolf = context.getRlIndex(2);
        assertTrue(respondingWolf != 2 && plannedMediumWolf != 2,
                "测试场景要求玩家2不在人狼名单中");
        context.setNonHumanPlan(plannedMediumWolf, NonHumanPlan.CLAIM_MEDIUM);
        markBlackResult(context, context.getActualRoleIndex(1), respondingWolf);
        markBlackResult(context, context.getActualRoleIndex(2), plannedMediumWolf);
        List<IntPair> responses = new ArrayList<>();
        context.getRandom().reseedForTest(3L);

        context.getRandom().startRecording();
        new OpeningBlackResultResponsePlanner(context)
                .collectResponses(context.getPlayerSum(), responses);

        assertEquals(List.of(
                new IntPair(respondingWolf, 2),
                new IntPair(plannedMediumWolf, 2)), responses);
        assertEquals(NonHumanPlan.CLAIM_MEDIUM, context.getNonHumanPlan(plannedMediumWolf));
        assertEquals(List.of(14), context.getRandom().stopRecording());
    }

    @Test
    void collectResponses_shouldHideUnblackenedPlannedMediumWolf()
    {
        GameContext context = createContext();
        int respondingWolf = context.getRlIndex(1);
        int plannedMediumWolf = context.getRlIndex(2);
        context.setNonHumanPlan(plannedMediumWolf, NonHumanPlan.CLAIM_MEDIUM);
        markBlackResult(context, context.getActualRoleIndex(1), respondingWolf);
        List<IntPair> responses = new ArrayList<>();
        context.getRandom().reseedForTest(3L);

        context.getRandom().startRecording();
        new OpeningBlackResultResponsePlanner(context)
                .collectResponses(context.getPlayerSum(), responses);

        assertEquals(List.of(new IntPair(respondingWolf, 2)), responses);
        assertEquals(NonHumanPlan.STAY_HIDDEN, context.getNonHumanPlan(plannedMediumWolf));
        assertEquals(List.of(14), context.getRandom().stopRecording());
    }

    @Test
    void whiteResponseWeights_shouldFollowActualRole()
    {
        assertEquals(List.of(95, 5, 0, 0),
                OpeningBlackResultResponsePlanner.whiteResponseWeights(8, 0));
        assertEquals(List.of(95, 5, 0, 0),
                OpeningBlackResultResponsePlanner.whiteResponseWeights(9, 3));
        assertEquals(List.of(80, 5, 10, 5),
                OpeningBlackResultResponsePlanner.whiteResponseWeights(10, 3));
        assertEquals(List.of(48, 30, 20, 2),
                OpeningBlackResultResponsePlanner.whiteResponseWeights(11, 3));
        assertThrows(IllegalArgumentException.class,
                () -> OpeningBlackResultResponsePlanner.whiteResponseWeights(7, 3));
        assertThrows(IllegalArgumentException.class,
                () -> OpeningBlackResultResponsePlanner.whiteResponseWeights(12, 3));
    }

    @Test
    void collectResponses_shouldUseActualRoleWeightsForWhiteNonHumanOutsideNumberRange()
    {
        GameContext context = createContext();
        int target = context.getActualRoleIndex(8);
        assertTrue(target < 8 || target > 11,
                "测试场景要求狂人位于玩家编号 8..11 之外，实际为 " + target);
        int seer = context.getActualRoleIndex(1);
        markBlackResult(context, seer, target);
        List<IntPair> responses = new ArrayList<>();
        long seed = findSeedForWeightedIndex(List.of(95, 5, 0, 0), 1);
        context.getRandom().reseedForTest(seed);

        context.getRandom().startRecording();
        new OpeningBlackResultResponsePlanner(context)
                .collectResponses(context.getPlayerSum(), responses);

        assertEquals(List.of(new IntPair(target, 2)), responses,
                "白方非人应按实际职业的权重选择响应，而不是按玩家编号");
        assertEquals(1, context.getRandom().stopRecording().size(),
                "按实际职业选择权重必须消耗一次随机数");
        assertEquals(NonHumanPlan.NONE, context.getNonHumanPlan(target));
    }

    private static long findSeedForWeightedIndex(List<Integer> weights, int expectedIndex)
    {
        for (long seed = 0; seed < 100_000; seed++)
        {
            if (new GameRandom(seed).weightedIndex(weights) == expectedIndex)
                return seed;
        }
        throw new AssertionError("在 100000 个种子内没有找到目标权重索引");
    }

    private static void markBlackResult(GameContext context, int claimedSeer, int target)
    {
        context.addClaimCandidate(1, claimedSeer);
        context.recordSkillTarget(claimedSeer, 1, context.getPlayerSum() + target);
    }

    private static GameContext createContext()
    {
        GameStatus status = new GameStatus(new GameRandom(20260910L));
        status.startGame(peiyi.jianyi);
        return new GameContext(status, NonHumanConfiguration.from(peiyi.jianyi, 0));
    }
}
