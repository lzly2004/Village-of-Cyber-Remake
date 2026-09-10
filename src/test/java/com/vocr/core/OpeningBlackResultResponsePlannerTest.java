package com.vocr.core;

import com.vocr.domain.IntPair;
import com.vocr.domain.peiyi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpeningBlackResultResponsePlannerTest
{
    @AfterEach
    void restoreRandomSource()
    {
        ConstNum.stopRandomRecording();
        ConstNum.resetRandomSeed();
    }

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

        ConstNum.startRandomRecording();
        new OpeningBlackResultResponsePlanner(context)
                .collectResponses(context.getPlayerSum(), responses);

        assertEquals(List.of(new IntPair(villager, 6)), responses);
        assertTrue(ConstNum.stopRandomRecording().isEmpty());
    }

    @Test
    void collectResponses_shouldPreserveExistingWolfClaimPlanWithoutRandomChoice()
    {
        GameContext context = createContext();
        int wolf = context.getRlIndex(1);
        context.setNonHumanPlan(wolf, NonHumanPlan.CLAIM_MEDIUM);
        markBlackResult(context, context.getActualRoleIndex(1), wolf);
        List<IntPair> responses = new ArrayList<>();

        ConstNum.startRandomRecording();
        new OpeningBlackResultResponsePlanner(context)
                .collectResponses(context.getPlayerSum(), responses);

        assertEquals(List.of(new IntPair(wolf, 2)), responses);
        assertEquals(NonHumanPlan.CLAIM_MEDIUM, context.getNonHumanPlan(wolf));
        assertTrue(ConstNum.stopRandomRecording().isEmpty());
    }

    @Test
    void collectResponses_shouldKeepHiddenWolfRandomSequenceStable()
    {
        GameContext context = createContext();
        int wolf = context.getRlIndex(1);
        markBlackResult(context, context.getActualRoleIndex(1), wolf);
        List<IntPair> responses = new ArrayList<>();
        ConstNum.setRandomSeed(0L);

        ConstNum.startRandomRecording();
        new OpeningBlackResultResponsePlanner(context)
                .collectResponses(context.getPlayerSum(), responses);

        assertEquals(List.of(new IntPair(wolf, 1)), responses);
        assertEquals(List.of(0), ConstNum.stopRandomRecording());
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
        ConstNum.setRandomSeed(3L);

        ConstNum.startRandomRecording();
        new OpeningBlackResultResponsePlanner(context)
                .collectResponses(context.getPlayerSum(), responses);

        assertEquals(List.of(
                new IntPair(respondingWolf, 2),
                new IntPair(plannedMediumWolf, 2)), responses);
        assertEquals(NonHumanPlan.CLAIM_MEDIUM, context.getNonHumanPlan(plannedMediumWolf));
        assertEquals(List.of(14), ConstNum.stopRandomRecording());
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
        ConstNum.setRandomSeed(3L);

        ConstNum.startRandomRecording();
        new OpeningBlackResultResponsePlanner(context)
                .collectResponses(context.getPlayerSum(), responses);

        assertEquals(List.of(new IntPair(respondingWolf, 2)), responses);
        assertEquals(NonHumanPlan.STAY_HIDDEN, context.getNonHumanPlan(plannedMediumWolf));
        assertEquals(List.of(14), ConstNum.stopRandomRecording());
    }

    private static void markBlackResult(GameContext context, int claimedSeer, int target)
    {
        context.addClaimCandidate(1, claimedSeer);
        context.recordSkillTarget(claimedSeer, 1, context.getPlayerSum() + target);
    }

    private static GameContext createContext()
    {
        ConstNum.setRandomSeed(20260910L);
        GameStatus status = new GameStatus();
        status.startGame(peiyi.jianyi);
        return new GameContext(status, NonHumanConfiguration.from(peiyi.jianyi, 0));
    }
}
