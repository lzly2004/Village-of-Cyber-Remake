package com.vocr.core;

import com.vocr.domain.Role;
import com.vocr.domain.peiyi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** R06 回归：当天潜伏猎人指定的目标不得被人狼选中。 */
class WolfBiterTest
{
    @Test
    void decideBite_shouldNotChooseHiddenHunterGuardTarget()
    {
        GameStatus status = new GameStatus(new GameRandom(20260910L));
        status.startGame(peiyi.jianyi);
        GameContext context = new GameContext(status, NonHumanConfiguration.from(peiyi.jianyi, 0));
        int hunter = context.getActualRoleIndex(Role.lie.ordinal());
        int villager = context.getActualRoleIndex(Role.wu.ordinal());
        context.registerClaim(hunter, Role.lie.ordinal());
        context.incrementGameDay();
        context.setHiddenHunterScheduled(villager, context.getGameDay(), true);
        WolfBiter biter = new WolfBiter(context, context.getSuspicion());

        for (long seed = 0; seed < 100; seed++)
        {
            context.getRandom().reseedForTest(seed);
            int[] bite = biter.decideBite();
            assertNotEquals(villager, bite[1], "潜伏猎人护卫目标不应被咬: seed=" + seed);
        }
    }
}
