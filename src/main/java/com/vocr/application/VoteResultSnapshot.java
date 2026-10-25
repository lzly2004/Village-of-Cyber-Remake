package com.vocr.application;

import java.util.List;
import java.util.Objects;

/** 某一游戏日完成的全部投票轮次及最终处刑结论。 */
public record VoteResultSnapshot(
        int day,
        VotingMethod method,
        List<Integer> candidates,
        List<VoteRoundSnapshot> rounds)
{
    public VoteResultSnapshot
    {
        if (day <= 0)
            throw new IllegalArgumentException("投票日期必须为正数");
        Objects.requireNonNull(method, "投票方式不能为空");
        candidates = List.copyOf(candidates);
        rounds = List.copyOf(rounds);
        if (candidates.isEmpty() || rounds.isEmpty())
            throw new IllegalArgumentException("已完成投票必须包含候补与轮次");
        for (int index = 0; index < rounds.size(); index++)
        {
            VoteRoundSnapshot round = rounds.get(index);
            if (round.round() != index + 1)
                throw new IllegalArgumentException("投票轮次必须从 1 连续排列");
            if (index < rounds.size() - 1 && !round.tied())
                throw new IllegalArgumentException("非最终轮次必须是同票重投");
        }
        if (rounds.get(rounds.size() - 1).tied())
            throw new IllegalArgumentException("最终轮次必须产生唯一处刑者");
    }

    public VoteRoundSnapshot finalRound()
    {
        return rounds.get(rounds.size() - 1);
    }

    public int executedPlayer()
    {
        return finalRound().soleLeader().orElseThrow();
    }
}
