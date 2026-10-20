package com.vocr.application;

import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;

/** 已完成的一轮公开票型及其权威计票结论。 */
public record VoteRoundSnapshot(
        int round,
        List<VoteLineSnapshot> lines,
        int maximumVotes,
        List<Integer> leaders)
{
    public VoteRoundSnapshot
    {
        if (round <= 0)
            throw new IllegalArgumentException("投票轮次必须为正数");
        List<VoteLineSnapshot> immutableLines = List.copyOf(lines);
        List<Integer> immutableLeaders = List.copyOf(leaders);
        lines = immutableLines;
        leaders = immutableLeaders;
        if (immutableLines.isEmpty() || maximumVotes <= 0 || immutableLeaders.isEmpty())
            throw new IllegalArgumentException("已完成轮次必须包含有效票型和领先者");
        if (immutableLines.stream().map(VoteLineSnapshot::playerNumber).distinct().count()
                != immutableLines.size())
            throw new IllegalArgumentException("同一玩家不能在一轮票型中出现多次");
        if (new HashSet<>(immutableLeaders).size() != immutableLeaders.size())
            throw new IllegalArgumentException("领先者不能重复");
        if (immutableLeaders.stream().anyMatch(leader -> immutableLines.stream().noneMatch(
                line -> line.playerNumber() == leader
                        && line.receivedVotes() == maximumVotes)))
            throw new IllegalArgumentException("领先者与权威票数不一致");
        if (immutableLines.stream().mapToInt(VoteLineSnapshot::receivedVotes).max().orElseThrow()
                != maximumVotes)
            throw new IllegalArgumentException("最高票数与票型不一致");
    }

    public boolean tied()
    {
        return leaders.size() > 1;
    }

    public OptionalInt soleLeader()
    {
        return tied() ? OptionalInt.empty() : OptionalInt.of(leaders.get(0));
    }
}
