package com.vocr.application;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** 当前业务阶段允许提交的一种投票方式及其权威候选项。 */
public record LegalVoteAction(VotingMethod method, List<Integer> candidates)
{
    public LegalVoteAction
    {
        Objects.requireNonNull(method, "投票方式不能为空");
        candidates = List.copyOf(candidates);
        if (candidates.isEmpty())
            throw new IllegalArgumentException("合法投票动作必须包含候选项");
        if (candidates.stream().anyMatch(player -> player <= 0))
            throw new IllegalArgumentException("投票候选编号必须为正数: " + candidates);
        if (new HashSet<>(candidates).size() != candidates.size())
            throw new IllegalArgumentException("投票候选不能重复: " + candidates);
    }
}
