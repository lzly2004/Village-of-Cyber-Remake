package com.vocr.core;

import com.vocr.domain.Role;
import com.vocr.domain.peiyi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DayVoteCoordinatorTest
{
    @AfterEach
    void restoreRandomSource()
    {
        ConstNum.stopRandomRecording();
        ConstNum.resetRandomSeed();
    }

    @Test
    void prepareVotablePlayers_shouldApplyRuleSpecificCandidateFilters()
    {
        GameContext context = createContext();
        int seer = context.getActualRoleIndex(Role.zhan.ordinal());
        int wolf = context.getRlIndex(1);
        int villager = context.getActualRoleIndex(Role.wu.ordinal());
        context.registerClaim(seer, Role.zhan.ordinal());
        context.recordSkillTarget(seer, 1, context.getPlayerSum() + wolf);
        context.incrementGameDay();
        DayVoteCoordinator coordinator = new DayVoteCoordinator(
                context, new VoteSelector(context, context.getSuspicion()), null);

        boolean[] greyVote = coordinator.prepareVotablePlayers(
                context.getPlayerSum(), context.getGameDay(), 1, List.of());
        boolean[] designatedVote = coordinator.prepareVotablePlayers(
                context.getPlayerSum(), context.getGameDay(), 2, List.of(wolf));
        boolean[] openVote = coordinator.prepareVotablePlayers(
                context.getPlayerSum(), context.getGameDay(), 0, List.of());

        assertFalse(greyVote[seer], "已 CO 占卜师不属于灰区");
        assertFalse(greyVote[wolf], "既有黑结果目标应从灰区排除");
        assertTrue(greyVote[villager]);
        assertTrue(designatedVote[wolf]);
        assertFalse(designatedVote[villager]);
        assertTrue(openVote[seer]);
        assertTrue(openVote[wolf]);
        assertTrue(openVote[villager]);
    }

    @Test
    void conductVoting_shouldCompleteDesignatedVoteAndPublishHistory()
    {
        GameContext context = createContext();
        context.incrementGameDay();
        int target = context.getActualRoleIndex(Role.wu.ordinal());
        DayVoteCoordinator coordinator = new DayVoteCoordinator(
                context, new VoteSelector(context, context.getSuspicion()), null);

        ConstNum.startRandomRecording();
        int executedPlayer = coordinator.conductVoting(
                context.getPlayerSum(), context.getGameDay(), 2, List.of(target));

        assertEquals(target, executedPlayer);
        assertEquals(1, context.getVoteRoundCount(context.getGameDay()));
        assertEquals(2, context.getVotingRule(context.getGameDay()));
        assertEquals(List.of(target), context.getVotingCandidates(context.getGameDay()));
        for (int player = 1; player <= context.getPlayerSum(); player++)
        {
            int voteTarget = context.getVoteTarget(player, context.getGameDay(), 1);
            if (player == target)
                assertNotEquals(target, voteTarget, "玩家不能投给自己");
            else
                assertEquals(target, voteTarget);
        }
        assertFalse(ConstNum.stopRandomRecording().isEmpty());
    }

    private static GameContext createContext()
    {
        ConstNum.setRandomSeed(20260910L);
        GameStatus status = new GameStatus();
        status.startGame(peiyi.jianyi);
        return new GameContext(status, NonHumanConfiguration.from(peiyi.jianyi, 0));
    }
}
