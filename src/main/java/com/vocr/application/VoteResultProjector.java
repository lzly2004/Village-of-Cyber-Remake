package com.vocr.application;

import com.vocr.domain.GameContextView;

import java.util.ArrayList;
import java.util.List;

/** 将核心投票状态投影为不含真实身份与内部策略的公开结果。 */
final class VoteResultProjector
{
    private VoteResultProjector() { }

    static List<VoteResultSnapshot> projectHistory(GameContextView context)
    {
        List<VoteResultSnapshot> history = new ArrayList<>();
        for (int day = 1; day <= context.getEffectiveGameDay(); day++)
        {
            int roundCount = context.getVoteRoundCount(day);
            if (roundCount > 0)
                history.add(project(context, day, roundCount));
        }
        return List.copyOf(history);
    }

    private static VoteResultSnapshot project(
            GameContextView context, int day, int roundCount)
    {
        List<VoteRoundSnapshot> rounds = new ArrayList<>();
        for (int round = 1; round <= roundCount; round++)
            rounds.add(projectRound(context, day, round));
        return new VoteResultSnapshot(
                day,
                VotingMethod.fromLegacyCode(context.getVotingRule(day)),
                context.getVotingCandidates(day),
                rounds);
    }

    private static VoteRoundSnapshot projectRound(
            GameContextView context, int day, int round)
    {
        int playerSum = context.getPlayerSum();
        int[] targets = new int[playerSum + 1];
        int[] receivedVotes = new int[playerSum + 1];
        for (int player = 1; player <= playerSum; player++)
        {
            int target = context.getVoteTarget(player, day, round);
            targets[player] = target;
            if (target > 0)
                receivedVotes[target]++;
        }

        int maximumVotes = 0;
        for (int player = 1; player <= playerSum; player++)
            maximumVotes = Math.max(maximumVotes, receivedVotes[player]);

        List<VoteLineSnapshot> lines = new ArrayList<>();
        List<Integer> leaders = new ArrayList<>();
        for (int player = 1; player <= playerSum; player++)
        {
            if (targets[player] > 0)
                lines.add(new VoteLineSnapshot(player, receivedVotes[player], targets[player]));
            if (receivedVotes[player] == maximumVotes && maximumVotes > 0)
                leaders.add(player);
        }
        return new VoteRoundSnapshot(round, lines, maximumVotes, leaders);
    }
}
