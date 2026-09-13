package com.vocr.core;

import com.vocr.domain.peiyi;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricatorTest
{
    @Test
    void fakeHunterDiary_shouldUseFreeNightSlotsWithoutConflictingWithSeerMediumPlans()
    {
        GameStatus status = new GameStatus(new GameRandom(20260912L));
        status.startGame(peiyi.jianyi);
        GameContext context = new GameContext(status, NonHumanConfiguration.from(peiyi.jianyi, 0));
        context.incrementGameDay();

        List<Integer> nonHumans = new ArrayList<>();
        for (int player = 1; player <= context.getPlayerSum(); player++)
            if (context.getActualRole(player) >= 7) nonHumans.add(player);
        assertTrue(nonHumans.size() >= 2);

        int plannedSeer = nonHumans.get(0);
        int plannedMedium = nonHumans.get(1);
        context.setNonHumanPlan(plannedSeer, NonHumanPlan.CLAIM_SEER);
        context.setNonHumanPlan(plannedMedium, NonHumanPlan.CLAIM_MEDIUM);

        SuspicionSystem suspicion = context.getSuspicion();
        HunterGuarder hunterGuarder = new HunterGuarder(context, suspicion);
        Fabricator fabricator = new Fabricator(
                context, suspicion, new SeerDiviner(context, suspicion), hunterGuarder);

        context.getRandom().startRecording();
        fabricator.prepareFakeHunterDiaries();
        assertEquals(0, context.getSkillTarget(plannedSeer, 2));
        assertEquals(0, context.getSkillTarget(plannedMedium, 2));

        context.clearNonHumanPlan(plannedSeer);
        fabricator.prepareFakeHunterDiaries();
        assertTrue(context.getSkillTarget(plannedSeer, 2) > 0,
                "占卜计划被取消后，当夜空闲技能槽应开始保存潜在假猎人日记");
        assertTrue(context.getRandom().stopRecording().isEmpty(), "潜在日记不得改变主游戏随机序列");
    }
}
