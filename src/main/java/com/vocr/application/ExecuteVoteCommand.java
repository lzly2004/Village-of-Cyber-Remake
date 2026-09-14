package com.vocr.application;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** 执行当日投票所需的不可变输入；回避开关仅在指定投票时由现有规则使用。 */
public record ExecuteVoteCommand(
        VotingMethod method,
        List<Integer> candidates,
        boolean avoidanceEnabled)
{
    public ExecuteVoteCommand
    {
        Objects.requireNonNull(method, "投票方式不能为空");
        Objects.requireNonNull(candidates, "投票候补不能为空");
        candidates = List.copyOf(candidates);
        if (candidates.isEmpty())
            throw new IllegalArgumentException("投票候补不能为空");
        if (candidates.stream().anyMatch(player -> player <= 0))
            throw new IllegalArgumentException("投票候补编号必须为正数: " + candidates);
        if (new HashSet<>(candidates).size() != candidates.size())
            throw new IllegalArgumentException("投票候补不能重复: " + candidates);
    }
}
