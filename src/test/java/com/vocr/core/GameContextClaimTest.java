package com.vocr.core;

import com.vocr.domain.peiyi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameContextClaimTest
{
    @ParameterizedTest(name = "主动声明职业 {0}")
    @ValueSource(ints = {1, 2, 3, 5})
    void registerClaim_shouldAtomicallyUpdateIdentityOrderAndCandidateList(int role)
    {
        GameContext context = createContext();

        context.registerClaim(1, role);
        context.registerClaim(1, role);

        assertEquals(role, context.getClaimedRole(1));
        assertEquals(context.getGameDay(), context.getComingOutDay(1));
        assertEquals(1, context.getClaimedRoleOrder(1));
        assertEquals(1, context.getClaimedRoleOrderCount(role));
        assertEquals(List.of(1), candidates(context, role));

        context.incrementGameDay();
        context.registerClaim(2, role);

        assertEquals(2, context.getComingOutDay(2));
        assertEquals(2, context.getClaimedRoleOrder(2));
        assertEquals(2, context.getClaimedRoleOrderCount(role));
        assertEquals(List.of(1, 2), candidates(context, role));
    }

    @Test
    void revealClaim_shouldRecordIdentityWithoutRegisteringActiveCandidate()
    {
        GameContext context = createContext();

        context.revealClaim(1, 5, 2);
        context.revealClaim(1, 5, 3);

        assertEquals(5, context.getClaimedRole(1));
        assertEquals(2, context.getComingOutDay(1));
        assertEquals(0, context.getClaimedRoleOrder(1));
        assertEquals(0, context.getClaimedRoleOrderCount(5));
        assertEquals(List.of(), context.getMaos());
    }

    @ParameterizedTest(name = "无候补列表职业 {0}")
    @ValueSource(ints = {4, 6})
    void registerClaim_shouldSupportRolesWithoutCandidateLists(int role)
    {
        GameContext context = createContext();

        context.registerClaim(1, role);

        assertEquals(role, context.getClaimedRole(1));
        assertEquals(context.getGameDay(), context.getComingOutDay(1));
        assertEquals(0, context.getClaimedRoleOrder(1));
        assertTrue(context.getZhans().isEmpty());
        assertTrue(context.getLings().isEmpty());
        assertTrue(context.getLies().isEmpty());
        assertTrue(context.getMaos().isEmpty());
    }

    @Test
    void claimCandidatePlanning_shouldBeUniqueDefensiveAndRemovable()
    {
        GameContext context = createContext();

        context.addClaimCandidate(1, 1);
        context.addClaimCandidate(1, 1);

        assertEquals(List.of(1), context.getZhans());
        assertThrows(UnsupportedOperationException.class, () -> context.getZhans().add(2));

        context.removeClaimCandidate(1, 1);
        assertTrue(context.getZhans().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> context.removeClaimCandidate(1, 0));
    }

    @Test
    void claimRequest_shouldOnlyRecordTheFirstValidDay()
    {
        GameContext context = createContext();

        assertTrue(context.tryRecordClaimRequest(3, 2));
        assertFalse(context.tryRecordClaimRequest(3, 4));
        assertEquals(2, context.getClaimedRoleAskDay(3));
        assertThrows(IllegalArgumentException.class, () -> context.tryRecordClaimRequest(0, 2));
        assertThrows(IllegalArgumentException.class, () -> context.tryRecordClaimRequest(3, 0));
    }

    @Test
    void claimWrites_shouldRejectInvalidStateWithoutPartialUpdates()
    {
        GameContext context = createContext();

        assertThrows(IllegalArgumentException.class, () -> context.registerClaim(0, 1));
        assertThrows(IllegalArgumentException.class, () -> context.registerClaim(1, 7));
        assertThrows(IllegalArgumentException.class, () -> context.revealClaim(1, 1, 0));

        assertEquals(0, context.getClaimedRole(1));
        assertEquals(-1, context.getComingOutDay(1));
        assertTrue(context.getZhans().isEmpty());
    }

    @Test
    void wolfClaim_shouldSynchronizeHunterAndCatFlags()
    {
        GameContext hunterContext = createContext();
        int hunterWolf = findWolf(hunterContext);
        hunterContext.registerClaim(hunterWolf, 3);
        assertTrue(hunterContext.isWolfClaimedHunter());
        assertFalse(hunterContext.isWolfClaimedCat());

        GameContext catContext = createContext();
        int catWolf = findWolf(catContext);
        catContext.registerClaim(catWolf, 5);
        assertFalse(catContext.isWolfClaimedHunter());
        assertTrue(catContext.isWolfClaimedCat());
    }

    private static List<Integer> candidates(GameContext context, int role)
    {
        return switch (role)
        {
            case 1 -> context.getZhans();
            case 2 -> context.getLings();
            case 3 -> context.getLies();
            case 5 -> context.getMaos();
            default -> throw new IllegalArgumentException("没有候补列表的职业: " + role);
        };
    }

    private static int findWolf(GameContext context)
    {
        for (int player = 1; player <= context.getPlayerSum(); player++)
            if (context.getActualRole(player) == 7)
                return player;
        throw new AssertionError("测试配役中没有人狼");
    }

    private static GameContext createContext()
    {
        GameStatus status = new GameStatus();
        status.startGame(peiyi.jianyi);
        return new GameContext(status, NonHumanConfiguration.from(peiyi.jianyi, 0));
    }
}
