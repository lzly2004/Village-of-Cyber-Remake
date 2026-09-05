package com.vocr.core;

import com.vocr.domain.peiyi;
import com.vocr.domain.whyDie;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameContextDeathTest
{
    @ParameterizedTest(name = "死亡原因 {0}")
    @EnumSource(value = whyDie.class, names = "NONE", mode = EnumSource.Mode.EXCLUDE)
    void recordDeath_shouldAtomicallyUpdatePlayerAndCounters(whyDie reason)
    {
        GameContext context = createContext();
        int player = 1;
        int aliveBefore = context.getAliveCounter();
        int deathBefore = context.getDeathCounter();

        context.recordDeath(player, reason);

        assertTrue(context.isDead(player));
        assertEquals(reason, context.getDeathReason(player));
        assertEquals(context.getGameDay(), context.getDeathDay(player));
        assertEquals(aliveBefore - 1, context.getAliveCounter());
        assertEquals(deathBefore + 1, context.getDeathCounter());
        assertEquals(context.getPlayerSum(), context.getAliveCounter() + context.getDeathCounter());
        assertEquals(reason.isNightDeath(), context.dieAtNight(context.getGameDay()).contains(player));
    }

    @Test
    void recordDeath_shouldRejectInvalidOrDuplicateWritesWithoutChangingCounters()
    {
        GameContext context = createContext();
        int aliveBefore = context.getAliveCounter();
        int deathBefore = context.getDeathCounter();

        assertThrows(IllegalArgumentException.class, () -> context.recordDeath(0, whyDie.beiyao));
        assertThrows(IllegalArgumentException.class,
                () -> context.recordDeath(context.getPlayerSum() + 1, whyDie.beiyao));
        assertThrows(IllegalArgumentException.class, () -> context.recordDeath(1, whyDie.NONE));
        assertThrows(IllegalArgumentException.class, () -> context.recordDeath(1, null));
        assertEquals(aliveBefore, context.getAliveCounter());
        assertEquals(deathBefore, context.getDeathCounter());

        context.recordDeath(1, whyDie.beiyao);
        assertThrows(IllegalStateException.class, () -> context.recordDeath(1, whyDie.zhousha));
        assertEquals(aliveBefore - 1, context.getAliveCounter());
        assertEquals(deathBefore + 1, context.getDeathCounter());
        assertEquals(whyDie.beiyao, context.getDeathReason(1));
    }

    @Test
    void recordNightDeaths_shouldSynchronizeDefensiveSnapshotCountAndDoubleDeathFlag()
    {
        GameContext context = createContext();
        int day = context.getGameDay();
        context.recordDeath(1, whyDie.beiyao);
        context.recordDeath(2, whyDie.zhousha);
        ArrayList<Integer> callerList = new ArrayList<>(List.of(1, 2));

        context.recordNightDeaths(callerList);
        callerList.clear();

        assertEquals(List.of(1, 2), context.getDiebody());
        assertEquals(2, context.getNightDeathCount(day));
        assertTrue(context.isDoubleDeathOccurred(day));
        assertThrows(UnsupportedOperationException.class, () -> context.getDiebody().add(3));

    }

    @Test
    void recordNightDeaths_shouldRejectNonNightOrDuplicateEntriesAtomically()
    {
        GameContext context = createContext();
        int day = context.getGameDay();
        context.recordDeath(1, whyDie.chuxing);

        assertThrows(IllegalStateException.class, () -> context.recordNightDeaths(List.of(1)));
        assertThrows(IllegalStateException.class, () -> context.recordNightDeaths(List.of(2)));
        assertEquals(List.of(), context.getDiebody());
        assertEquals(0, context.getNightDeathCount(day));
        assertFalse(context.isDoubleDeathOccurred(day));

        GameContext nightContext = createContext();
        nightContext.recordDeath(1, whyDie.beiyao);
        assertThrows(IllegalArgumentException.class, () -> nightContext.recordNightDeaths(List.of(1, 1)));
        assertEquals(List.of(), nightContext.getDiebody());
        assertEquals(0, nightContext.getNightDeathCount(nightContext.getGameDay()));
    }

    private static GameContext createContext()
    {
        GameStatus status = new GameStatus();
        status.startGame(peiyi.jianyi);
        return new GameContext(status);
    }
}
