package com.vocr.core;

import com.vocr.config.GameConstants;
import com.vocr.domain.peiyi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GameContextSuspicionTest
{
    @Test
    void initialState_shouldUseNeutralValuesExceptForSelf()
    {
        GameContext context = createContext();
        int lastPlayer = context.getPlayerSum();

        assertEquals(-GameConstants.INF, context.getSuspicionValue(1, 1));
        assertEquals(50, context.getSuspicionValue(1, 2));
        assertEquals(50, context.getSuspicionValue(lastPlayer, 1));
        assertEquals(-GameConstants.INF, context.getSuspicionValue(lastPlayer, lastPlayer));
        assertEquals(0, context.getLazySuspicionValue(1));
        assertEquals(0, context.getTop3SuspectedPlayer(1, 1, 1));
    }

    @Test
    void suspicionValues_shouldRejectInvalidCoordinatesBeforeMutation()
    {
        GameContext context = createContext();
        context.setSuspicionValue(1, 2, 75);

        assertThrows(IllegalArgumentException.class, () -> context.getSuspicionValue(0, 2));
        assertThrows(IllegalArgumentException.class, () -> context.getSuspicionValue(1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> context.setSuspicionValue(context.getPlayerSum() + 1, 2, 10));
        assertThrows(IllegalArgumentException.class,
                () -> context.addSuspicionValue(1, context.getPlayerSum() + 1, 10));

        assertEquals(75, context.getSuspicionValue(1, 2));
    }

    @Test
    void mutualSuspicion_shouldBeAtomicWhenEitherPlayerIsInvalid()
    {
        GameContext context = createContext();
        SuspicionSystem suspicion = context.getSuspicion();
        int firstBefore = context.getSuspicionValue(1, 2);
        int secondBefore = context.getSuspicionValue(2, 1);

        suspicion.adjustMutualSuspicion(1, 2, 7, -3);
        assertEquals(firstBefore + 7, context.getSuspicionValue(1, 2));
        assertEquals(secondBefore - 3, context.getSuspicionValue(2, 1));

        assertThrows(IllegalArgumentException.class,
                () -> suspicion.adjustMutualSuspicion(1, context.getPlayerSum() + 1, 100, 100));
        assertEquals(firstBefore + 7, context.getSuspicionValue(1, 2));
        assertEquals(secondBefore - 3, context.getSuspicionValue(2, 1));
    }

    @Test
    void lazySuspicion_shouldSupportUpdatesAndWholeCycleReset()
    {
        GameContext context = createContext();

        context.addLazySuspicionValue(1, 10);
        context.addLazySuspicionValue(2, -20);
        assertEquals(10, context.getLazySuspicionValue(1));
        assertEquals(-20, context.getLazySuspicionValue(2));

        context.resetLazySuspicionValues();
        assertEquals(0, context.getLazySuspicionValue(1));
        assertEquals(0, context.getLazySuspicionValue(2));

        assertThrows(IllegalArgumentException.class, () -> context.getLazySuspicionValue(0));
        assertThrows(IllegalArgumentException.class,
                () -> context.addLazySuspicionValue(context.getPlayerSum() + 1, 1));
    }

    @Test
    void topSuspects_shouldValidatePlayerRankDayAndTarget()
    {
        GameContext context = createContext();
        int maxDay = GameConstants.MAX_GAME_DAYS;

        context.setTop3SuspectedPlayer(1, 1, 1, 2);
        context.setTop3SuspectedPlayer(1, 3, maxDay, context.getPlayerSum());
        assertEquals(2, context.getTop3SuspectedPlayer(1, 1, 1));
        assertEquals(context.getPlayerSum(), context.getTop3SuspectedPlayer(1, 3, maxDay));

        assertThrows(IllegalArgumentException.class,
                () -> context.getTop3SuspectedPlayer(0, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> context.getTop3SuspectedPlayer(1, 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> context.getTop3SuspectedPlayer(1, GameConstants.TOP3_ARRAY_SIZE, 1));
        assertThrows(IllegalArgumentException.class,
                () -> context.getTop3SuspectedPlayer(1, 1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> context.setTop3SuspectedPlayer(1, 1, 1, context.getPlayerSum() + 1));

        assertEquals(2, context.getTop3SuspectedPlayer(1, 1, 1));
    }

    private static GameContext createContext()
    {
        GameStatus status = new GameStatus();
        status.startGame(peiyi.jianyi);
        return new GameContext(status, NonHumanConfiguration.from(peiyi.jianyi, 0));
    }
}
