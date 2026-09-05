package com.vocr.core;

import com.vocr.config.GameConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 投票状态的唯一存储：每轮票型、指定候补以及已完成投票的历史元数据。
 */
final class VoteState
{
    private final int playerSum;
    private final int[][][] voteTargets;
    private final boolean[][] selectedVoteTargets;
    private final int[] completedRoundCounts;
    private final int[] votingRules;
    private final ArrayList<List<Integer>> votingCandidates;

    VoteState(int playerSum)
    {
        if (playerSum < 1)
            throw new IllegalArgumentException("玩家人数必须为正数: " + playerSum);
        this.playerSum = playerSum;
        this.voteTargets = new int[playerSum + 1]
                [GameConstants.MAX_GAME_DAYS + 1][GameConstants.MAX_VOTE_ROUNDS + 1];
        this.selectedVoteTargets = new boolean[playerSum + 1]
                [GameConstants.MAX_GAME_DAYS + 1];
        this.completedRoundCounts = new int[GameConstants.MAX_GAME_DAYS + 1];
        this.votingRules = new int[GameConstants.MAX_GAME_DAYS + 1];
        Arrays.fill(votingRules, -1);
        this.votingCandidates = new ArrayList<>(GameConstants.MAX_GAME_DAYS + 1);
        for (int day = 0; day <= GameConstants.MAX_GAME_DAYS; day++)
            votingCandidates.add(List.of());
    }

    int getVoteTarget(int player, int day, int round)
    {
        validatePlayer(player, "投票玩家");
        validateDay(day);
        validateRound(round);
        return voteTargets[player][day][round];
    }

    /**
     * 原子记录一整轮票型。0 表示该玩家本轮没有投票，其余值必须是实际玩家编号。
     */
    void recordVoteRound(int day, int round, int[] targets)
    {
        validateDay(day);
        validateRound(round);
        if (targets == null || targets.length != playerSum + 1)
            throw new IllegalArgumentException("票型数组长度必须为 " + (playerSum + 1));
        if (completedRoundCounts[day] != 0)
            throw new IllegalStateException("当天投票已经完成，不能覆盖票型: " + day);

        for (int player = 1; player <= playerSum; player++)
        {
            int target = targets[player];
            if (target != 0)
                validatePlayer(target, "投票目标");
        }

        for (int player = 1; player <= playerSum; player++)
            voteTargets[player][day][round] = targets[player];
    }

    /**
     * 在所有轮次写入后，原子记录当天投票采用的规则、候补快照和最终轮数。
     */
    void completeVoting(int day, int votingRule, List<Integer> candidates, int roundCount)
    {
        validateDay(day);
        validateVotingRule(votingRule);
        validateRound(roundCount);
        if (completedRoundCounts[day] != 0)
            throw new IllegalStateException("当天投票已经完成: " + day);
        if (candidates == null)
            throw new IllegalArgumentException("投票候补不能为空");

        boolean[] seen = new boolean[playerSum + 1];
        for (Integer candidate : candidates)
        {
            if (candidate == null)
                throw new IllegalArgumentException("投票候补不能包含空值");
            validatePlayer(candidate, "投票候补");
            if (seen[candidate])
                throw new IllegalArgumentException("投票候补重复: " + candidate);
            seen[candidate] = true;
        }
        for (int round = 1; round <= roundCount; round++)
        {
            boolean hasVote = false;
            for (int player = 1; player <= playerSum; player++)
            {
                if (voteTargets[player][day][round] != 0)
                {
                    hasVote = true;
                    break;
                }
            }
            if (!hasVote)
                throw new IllegalStateException("投票轮次没有有效票型: day=" + day + ", round=" + round);
        }

        List<Integer> candidateSnapshot = List.copyOf(candidates);
        completedRoundCounts[day] = roundCount;
        votingRules[day] = votingRule;
        votingCandidates.set(day, candidateSnapshot);
    }

    int getCompletedRoundCount(int day)
    {
        validateDay(day);
        return completedRoundCounts[day];
    }

    int getVotingRule(int day)
    {
        validateDay(day);
        return votingRules[day];
    }

    List<Integer> getVotingCandidates(int day)
    {
        validateDay(day);
        return votingCandidates.get(day);
    }

    boolean isSelectedVoteTarget(int player, int day)
    {
        validatePlayer(player, "指定投票目标");
        validateDay(day);
        return selectedVoteTargets[player][day];
    }

    void setSelectedVoteTarget(int player, int day, boolean selected)
    {
        validatePlayer(player, "指定投票目标");
        validateDay(day);
        selectedVoteTargets[player][day] = selected;
    }

    int[][] getVoteTargetsSnapshot(int player)
    {
        validatePlayer(player, "投票玩家");
        int[][] snapshot = new int[voteTargets[player].length][];
        for (int day = 0; day < voteTargets[player].length; day++)
            snapshot[day] = voteTargets[player][day].clone();
        return snapshot;
    }

    boolean[] getSelectedVoteTargetsSnapshot(int player)
    {
        validatePlayer(player, "指定投票目标");
        return selectedVoteTargets[player].clone();
    }

    private void validatePlayer(int player, String stateName)
    {
        if (player < 1 || player > playerSum)
            throw new IllegalArgumentException(stateName + "索引越界: " + player);
    }

    private static void validateDay(int day)
    {
        if (day < 1 || day > GameConstants.MAX_GAME_DAYS)
            throw new IllegalArgumentException("投票日期越界: " + day);
    }

    private static void validateRound(int round)
    {
        if (round < 1 || round > GameConstants.MAX_VOTE_ROUNDS)
            throw new IllegalArgumentException("投票轮次越界: " + round);
    }

    private static void validateVotingRule(int votingRule)
    {
        if (votingRule < 0 || votingRule > 2)
            throw new IllegalArgumentException("投票规则越界: " + votingRule);
    }
}
