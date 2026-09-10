package com.vocr.core;

import com.vocr.domain.Role;
import com.vocr.domain.peiyi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WolfBiteAnalyzerTest
{
    @AfterEach
    void restoreRandomSource()
    {
        ConstNum.stopRandomRecording();
        ConstNum.resetRandomSeed();
    }

    @Test
    void analyze_shouldDescribeDefaultOpeningWithoutRandomCalls()
    {
        GameContext context = createContext();
        WolfBiteAnalyzer analyzer = new WolfBiteAnalyzer(context);

        ConstNum.startRandomRecording();
        WolfBiteAnalyzer.Analysis analysis = analyzer.analyze(
                context.getPlayerSum(), context.getGameDay());

        assertEquals(context.getInitialWolfCount(), analysis.aliveWolfCount);
        assertEquals(0, analysis.bittenSeerAccurateBonus);
        assertEquals(1, analysis.bittenSeerNotAccurateBonus);
        assertEquals(0, analysis.plausibleSeerCount);
        assertEquals(0, analysis.plausibleHunterCount);
        assertEquals(0, analysis.plausibleMediumCount);
        assertEquals(0.8, analysis.hunterGuardFactor, 0.000_001);
        assertEquals(0, analysis.hiddenHunterTargetCount);
        assertArrayEquals(new boolean[context.getPlayerSum() + 1], analysis.plausibleSeers);
        assertArrayEquals(new boolean[context.getPlayerSum() + 1], analysis.plausibleHunters);
        assertArrayEquals(new boolean[context.getPlayerSum() + 1], analysis.plausibleMediums);
        assertArrayEquals(new int[context.getPlayerSum() + 1], analysis.whiteBallsBySeer);
        assertArrayEquals(new int[context.getPlayerSum() + 1], analysis.whiteBallsReceived);
        assertTrue(ConstNum.stopRandomRecording().isEmpty());
    }

    @Test
    void analyze_shouldAggregateClaimedRolesWhiteBallsAndHiddenGuardTargets()
    {
        GameContext context = createContext();
        int seer = context.getActualRoleIndex(Role.zhan.ordinal());
        int medium = context.getActualRoleIndex(Role.ling.ordinal());
        int hunter = context.getActualRoleIndex(Role.lie.ordinal());
        int villager = context.getActualRoleIndex(Role.wu.ordinal());
        context.registerClaim(seer, Role.zhan.ordinal());
        context.registerClaim(medium, Role.ling.ordinal());
        context.registerClaim(hunter, Role.lie.ordinal());
        context.recordSkillTarget(seer, 1, villager);
        context.recordSkillTarget(medium, 1, villager);
        context.setHiddenHunterScheduled(villager, context.getPlayerSum(), true);
        context.incrementGameDay();

        WolfBiteAnalyzer.Analysis analysis = new WolfBiteAnalyzer(context).analyze(
                context.getPlayerSum(), context.getGameDay());

        assertTrue(analysis.plausibleSeers[seer]);
        assertTrue(analysis.plausibleMediums[medium]);
        assertTrue(analysis.plausibleHunters[hunter]);
        assertEquals(1, analysis.plausibleSeerCount);
        assertEquals(1, analysis.plausibleMediumCount);
        assertEquals(1, analysis.plausibleHunterCount);
        assertEquals(1, analysis.whiteBallsBySeer[seer]);
        assertEquals(1, analysis.whiteBallsReceived[villager]);
        assertEquals(0.5, analysis.hunterGuardFactor, 0.000_001);
        assertEquals(1, analysis.hiddenHunterTargetCount);
    }

    private static GameContext createContext()
    {
        ConstNum.setRandomSeed(20260910L);
        GameStatus status = new GameStatus();
        status.startGame(peiyi.jianyi);
        return new GameContext(status, NonHumanConfiguration.from(peiyi.jianyi, 0));
    }
}
