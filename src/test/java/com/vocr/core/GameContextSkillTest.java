package com.vocr.core;

import com.vocr.config.GameConstants;
import com.vocr.domain.peiyi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameContextSkillTest
{
    @Test
    void recordSkillTarget_shouldSupportEmptyWhiteAndBlackEncodings()
    {
        GameContext context = createContext();
        int players = context.getPlayerSum();

        context.recordSkillTarget(1, 1, 0);
        assertEquals(0, context.getSkillTarget(1, 1));

        context.recordSkillTarget(1, 1, players);
        assertFalse(context.isBlackResult(context.getSkillTarget(1, 1)));
        assertEquals(players, context.getActualTarget(context.getSkillTarget(1, 1)));

        context.recordSkillTarget(1, 1, players * 2);
        assertTrue(context.isBlackResult(context.getSkillTarget(1, 1)));
        assertEquals(players, context.getActualTarget(context.getSkillTarget(1, 1)));

        context.recordSkillTarget(1, GameConstants.MAX_GAME_DAYS, 1);
        assertEquals(1, context.getSkillTarget(1, GameConstants.MAX_GAME_DAYS));
    }

    @Test
    void recordSkillTarget_shouldRejectInvalidWritesWithoutChangingExistingState()
    {
        GameContext context = createContext();
        context.recordSkillTarget(1, 1, 2);

        assertThrows(IllegalArgumentException.class, () -> context.recordSkillTarget(0, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> context.recordSkillTarget(1, 0, 2));
        assertThrows(IllegalArgumentException.class,
                () -> context.recordSkillTarget(1, GameConstants.MAX_GAME_DAYS + 1, 2));
        assertThrows(IllegalArgumentException.class, () -> context.recordSkillTarget(1, 1, -1));
        assertThrows(IllegalArgumentException.class,
                () -> context.recordSkillTarget(1, 1, context.getPlayerSum() * 2 + 1));
        assertThrows(IllegalArgumentException.class, () -> context.getSkillTarget(0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> context.getSkillTarget(1, GameConstants.MAX_GAME_DAYS + 1));

        assertEquals(2, context.getSkillTarget(1, 1));
    }

    @Test
    void schedules_shouldBeIndependentAndToggleable()
    {
        GameContext context = createContext();
        int day = context.getGameDay();

        context.setClaimedRoleScheduled(1, 2, day, true);
        context.setHiddenSeerScheduled(3, day, true);
        context.setHiddenHunterScheduled(4, day, true);

        assertTrue(context.isClaimedRoleScheduled(1, 2, day));
        assertTrue(context.isHiddenSeerScheduled(3, day));
        assertTrue(context.isHiddenHunterScheduled(4, day));
        assertFalse(context.isHiddenHunterScheduled(3, day));

        context.setClaimedRoleScheduled(1, 2, day, false);
        context.setHiddenSeerScheduled(3, day, false);
        context.setHiddenHunterScheduled(4, day, false);
        assertFalse(context.isClaimedRoleScheduled(1, 2, day));
        assertFalse(context.isHiddenSeerScheduled(3, day));
        assertFalse(context.isHiddenHunterScheduled(4, day));
    }

    @Test
    void schedules_shouldRejectInvalidCoordinatesBeforeMutation()
    {
        GameContext context = createContext();
        int day = context.getGameDay();

        assertThrows(IllegalArgumentException.class,
                () -> context.setClaimedRoleScheduled(0, 1, day, true));
        assertThrows(IllegalArgumentException.class,
                () -> context.setClaimedRoleScheduled(1, 0, day, true));
        assertThrows(IllegalArgumentException.class,
                () -> context.setClaimedRoleScheduled(1, 1, 0, true));
        assertThrows(IllegalArgumentException.class,
                () -> context.setHiddenSeerScheduled(context.getPlayerSum() + 1, day, true));
        assertThrows(IllegalArgumentException.class,
                () -> context.setHiddenHunterScheduled(1, GameConstants.MAX_GAME_DAYS + 1, true));
        assertThrows(IllegalArgumentException.class,
                () -> context.isHiddenSeerScheduled(0, day));

        assertFalse(context.isClaimedRoleScheduled(1, 1, day));
        assertFalse(context.isHiddenSeerScheduled(1, day));
        assertFalse(context.isHiddenHunterScheduled(1, day));
    }

    @Test
    void divinationPlans_shouldValidateEncodedTargets()
    {
        GameContext context = createContext();
        int blackTarget = context.getPlayerSum() + 2;

        context.recordPreliminaryDivinationTarget(1, 2);
        context.recordPlannedDivinationResult(1, blackTarget);

        assertEquals(2, context.getPreliminaryDivinationTarget(1));
        assertEquals(blackTarget, context.getPlannedDivinationResult(1));

        assertThrows(IllegalArgumentException.class,
                () -> context.recordPreliminaryDivinationTarget(1, context.getPlayerSum() + 1));
        assertThrows(IllegalArgumentException.class,
                () -> context.recordPlannedDivinationResult(0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> context.recordPlannedDivinationResult(1, context.getPlayerSum() * 2 + 1));
    }

    private static GameContext createContext()
    {
        GameStatus status = new GameStatus();
        status.startGame(peiyi.jianyi);
        return new GameContext(status);
    }
}
