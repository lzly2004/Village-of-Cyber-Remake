package com.vocr.core;

import com.vocr.domain.peiyi;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpeningClaimCandidateBuilderTest
{
    @Test
    void build_shouldIncludeLivingTrueSeerAndMediumWithoutRandomCalls()
    {
        GameContext context = createContext();

        context.getRandom().startRecording();
        new OpeningClaimCandidateBuilder(context).build();

        assertEquals(List.of(context.getActualRoleIndex(1)), context.getZhans());
        assertEquals(List.of(context.getActualRoleIndex(2)), context.getLings());
        assertTrue(context.getRandom().stopRecording().isEmpty());
    }

    @Test
    void build_shouldPreservePlannedCandidateOrderAndSeerResults()
    {
        GameContext context = createContext();
        int wolfSeer = context.getRlIndex(1);
        int wolfMedium = context.getRlIndex(2);
        int leader = context.getNonHumanLeader();
        int wolfResult = context.getPlayerSum() + context.getActualRoleIndex(2);
        int leaderResult = context.getActualRoleIndex(1);
        context.setNonHumanPlan(wolfSeer, NonHumanPlan.CLAIM_SEER);
        context.recordPlannedDivinationResult(wolfSeer, wolfResult);
        context.setNonHumanPlan(wolfMedium, NonHumanPlan.CLAIM_MEDIUM);
        context.setNonHumanPlan(leader, NonHumanPlan.CLAIM_SEER);
        context.recordPlannedDivinationResult(leader, leaderResult);

        new OpeningClaimCandidateBuilder(context).build();

        assertEquals(List.of(wolfSeer, leader, context.getActualRoleIndex(1)), context.getZhans());
        assertEquals(List.of(wolfMedium, context.getActualRoleIndex(2)), context.getLings());
        assertEquals(wolfResult, context.getSkillTarget(wolfSeer, 1));
        assertEquals(leaderResult, context.getSkillTarget(leader, 1));
    }

    @Test
    void build_shouldSelectOnlyFirstPlannedWolfMedium()
    {
        GameContext context = createContext();
        int firstWolf = context.getRlIndex(1);
        int secondWolf = context.getRlIndex(2);
        context.setNonHumanPlan(firstWolf, NonHumanPlan.CLAIM_MEDIUM);
        context.setNonHumanPlan(secondWolf, NonHumanPlan.CLAIM_MEDIUM);

        new OpeningClaimCandidateBuilder(context).build();

        assertEquals(List.of(firstWolf, context.getActualRoleIndex(2)), context.getLings());
        assertFalse(context.getLings().contains(secondWolf));
    }

    private static GameContext createContext()
    {
        GameStatus status = new GameStatus(new GameRandom(20260910L));
        status.startGame(peiyi.jianyi);
        return new GameContext(status, NonHumanConfiguration.from(peiyi.jianyi, 0));
    }
}
