package com.vocr.application;

import com.vocr.domain.SelectionType;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** 当前指定投票、技能预约和回避开关的安全权威快照。 */
public record SelectionStateSnapshot(
        boolean avoidanceEnabled,
        List<Integer> voteTargets,
        SkillTargetSelections divination,
        SkillTargetSelections guard)
{
    public SelectionStateSnapshot
    {
        voteTargets = List.copyOf(voteTargets);
        Objects.requireNonNull(divination, "占卜预约不能为空");
        Objects.requireNonNull(guard, "护卫预约不能为空");
        if (voteTargets.stream().anyMatch(target -> target <= 0)
                || new HashSet<>(voteTargets).size() != voteTargets.size())
            throw new IllegalArgumentException(
                    "指定投票目标必须为不重复的正数: " + voteTargets);
    }

    public SkillTargetSelections skillSelections(SelectionType type)
    {
        return switch (Objects.requireNonNull(type, "目标预约类型不能为空"))
        {
            case DIVINATION -> divination;
            case GUARD -> guard;
            case VOTE -> throw new IllegalArgumentException("指定投票不属于技能预约");
        };
    }

    public boolean hasSelections(SelectionType type)
    {
        return switch (Objects.requireNonNull(type, "目标预约类型不能为空"))
        {
            case VOTE -> !voteTargets.isEmpty();
            case DIVINATION -> divination.hasSelections();
            case GUARD -> guard.hasSelections();
        };
    }

    public boolean isTargetSelected(SelectionType type, int target)
    {
        return switch (Objects.requireNonNull(type, "目标预约类型不能为空"))
        {
            case VOTE -> voteTargets.contains(target);
            case DIVINATION -> divination.isTargetSelected(target);
            case GUARD -> guard.isTargetSelected(target);
        };
    }
}
