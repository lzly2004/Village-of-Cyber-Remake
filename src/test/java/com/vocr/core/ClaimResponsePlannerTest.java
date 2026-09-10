package com.vocr.core;

import com.vocr.config.ProbabilityCalculator;
import com.vocr.domain.IntPair;
import com.vocr.domain.Role;
import com.vocr.domain.peiyi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaimResponsePlannerTest
{
    @AfterEach
    void restoreRandomSource()
    {
        ConstNum.stopRandomRecording();
        ConstNum.resetRandomSeed();
    }

    @Test
    void planResponsesForRole_shouldReturnTrueRoleAndOnlyHonorFirstRequest()
    {
        GameContext context = createContext();
        ClaimResponsePlanner planner = new ClaimResponsePlanner(context, new ProbabilityCalculator());
        int seer = context.getActualRoleIndex(Role.zhan.ordinal());

        ConstNum.startRandomRecording();
        List<IntPair> firstResponses = planner.planResponsesForRole(Role.zhan);
        List<IntPair> repeatedResponses = planner.planResponsesForRole(Role.zhan);

        assertEquals(List.of(new IntPair(seer, Role.zhan.ordinal())), firstResponses);
        assertTrue(repeatedResponses.isEmpty());
        assertEquals(context.getGameDay(), context.getClaimedRoleAskDay(Role.zhan.ordinal()));
        assertTrue(ConstNum.stopRandomRecording().isEmpty());
    }

    @Test
    void planResponsesForPlayers_shouldPreserveInputOrderAndPlannedSeerResult()
    {
        GameContext context = createContext();
        ClaimResponsePlanner planner = new ClaimResponsePlanner(context, new ProbabilityCalculator());
        int wolf = context.getRlIndex(1);
        int seer = context.getActualRoleIndex(Role.zhan.ordinal());
        int plannedResult = context.getPlayerSum() + context.getActualRoleIndex(Role.ling.ordinal());
        context.setNonHumanPlan(wolf, NonHumanPlan.CLAIM_SEER);
        context.recordPlannedDivinationResult(wolf, plannedResult);

        ConstNum.startRandomRecording();
        List<IntPair> responses = planner.planResponsesForPlayers(List.of(wolf, seer));

        assertEquals(List.of(
                new IntPair(wolf, Role.zhan.ordinal()),
                new IntPair(seer, Role.zhan.ordinal())), responses);
        assertEquals(plannedResult, context.getSkillTarget(wolf, 1));
        assertEquals(0, context.getClaimedRole(wolf), "规划阶段不应执行实际 CO");
        assertTrue(ConstNum.stopRandomRecording().isEmpty());
    }

    @Test
    void probabilityParameters_shouldPreserveWolfClaimAndSurvivorCounts()
    {
        GameContext context = createContext();
        ClaimResponsePlanner planner = new ClaimResponsePlanner(context, new ProbabilityCalculator());
        int wolf = context.getRlIndex(1);

        assertEquals(0, planner.getP1(7));
        assertEquals(context.getInitialWolfCount() - 1, planner.getP2(7));
        assertEquals(context.getInitialWolfCount() - 1, planner.getP2(9));
        assertEquals(0, planner.getP2(8));

        context.registerClaim(wolf, Role.lie.ordinal());

        assertEquals(1, planner.getP1(7));
        assertEquals(0, planner.getP1(8));
    }

    private static GameContext createContext()
    {
        ConstNum.setRandomSeed(20260910L);
        GameStatus status = new GameStatus();
        status.startGame(peiyi.jianyi);
        return new GameContext(status, NonHumanConfiguration.from(peiyi.jianyi, 0));
    }
}
