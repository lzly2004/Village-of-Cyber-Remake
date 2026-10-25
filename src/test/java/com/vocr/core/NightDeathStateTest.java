package com.vocr.core;

import com.vocr.config.GameConstants;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NightDeathStateTest
{
    @Test
    void records_shouldPublishDefensiveSnapshotAndIndependentDailyFacts()
    {
        NightDeathState state = new NightDeathState(4);
        ArrayList<Integer> callerDeaths = new ArrayList<>(List.of(1, 2));

        assertEquals(0, state.getDeathCount(0));
        assertFalse(state.isDoubleDeathOccurred(0));

        state.recordNightDeaths(1, callerDeaths);
        callerDeaths.clear();

        assertEquals(List.of(1, 2), state.getLatestDeaths());
        assertEquals(2, state.getDeathCount(1));
        assertTrue(state.isDoubleDeathOccurred(1));
        assertThrows(UnsupportedOperationException.class, () -> state.getLatestDeaths().add(3));

        state.recordNightDeaths(2, List.of(3));

        assertEquals(List.of(3), state.getLatestDeaths());
        assertEquals(2, state.getDeathCount(1));
        assertTrue(state.isDoubleDeathOccurred(1));
        assertEquals(1, state.getDeathCount(2));
        assertFalse(state.isDoubleDeathOccurred(2));
    }

    @Test
    void emptyNight_shouldClearLatestSnapshotAndRecordZeroFacts()
    {
        NightDeathState state = new NightDeathState(3);
        state.recordNightDeaths(1, List.of(1));

        state.recordNightDeaths(2, List.of());

        assertEquals(List.of(), state.getLatestDeaths());
        assertEquals(0, state.getDeathCount(2));
        assertFalse(state.isDoubleDeathOccurred(2));
    }

    @Test
    void catEvidence_shouldRespectDoubleNightAndFourDeathThresholds()
    {
        NightDeathState repeatedDoubleDeaths = new NightDeathState(4);
        repeatedDoubleDeaths.recordNightDeaths(1, List.of(1, 2));
        repeatedDoubleDeaths.recordNightDeaths(3, List.of(3, 4));

        assertFalse(repeatedDoubleDeaths.hasDefinitiveCatEvidenceThrough(2));
        assertTrue(repeatedDoubleDeaths.hasDefinitiveCatEvidenceThrough(3));

        NightDeathState fourDeathsAtOnce = new NightDeathState(4);
        fourDeathsAtOnce.recordNightDeaths(2, List.of(1, 2, 3, 4));

        assertFalse(fourDeathsAtOnce.hasDefinitiveCatEvidenceThrough(1));
        assertTrue(fourDeathsAtOnce.hasDefinitiveCatEvidenceThrough(2));
    }

    @Test
    void invalidOperations_shouldFailBeforeChangingState()
    {
        assertThrows(IllegalArgumentException.class, () -> new NightDeathState(0));

        NightDeathState state = new NightDeathState(3);
        state.recordNightDeaths(1, List.of(1));

        assertThrows(IllegalArgumentException.class, () -> state.getDeathCount(-1));
        assertThrows(IllegalArgumentException.class,
                () -> state.isDoubleDeathOccurred(GameConstants.MAX_GAME_DAYS + 1));
        assertThrows(IllegalArgumentException.class, () -> state.recordNightDeaths(0, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> state.recordNightDeaths(GameConstants.MAX_GAME_DAYS + 1, List.of()));
        assertThrows(IllegalArgumentException.class, () -> state.recordNightDeaths(2, null));
        assertThrows(IllegalArgumentException.class, () -> state.recordNightDeaths(2, List.of(0)));
        assertThrows(IllegalArgumentException.class, () -> state.recordNightDeaths(2, List.of(4)));
        assertThrows(IllegalArgumentException.class, () -> state.recordNightDeaths(2, List.of(2, 2)));
        assertThrows(IllegalArgumentException.class,
                () -> state.recordNightDeaths(2, Arrays.asList(2, null)));

        assertEquals(List.of(1), state.getLatestDeaths());
        assertEquals(1, state.getDeathCount(1));
        assertEquals(0, state.getDeathCount(2));
        assertFalse(state.isDoubleDeathOccurred(2));
    }
}
