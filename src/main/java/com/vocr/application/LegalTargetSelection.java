package com.vocr.application;

import com.vocr.domain.SelectionType;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** 一种目标预约操作的合法目标，以及可单独指定的公开 CO 行动者。 */
public record LegalTargetSelection(
        SelectionType type,
        List<Integer> targets,
        List<ClaimedRoleActor> claimedActors)
{
    public LegalTargetSelection
    {
        Objects.requireNonNull(type, "目标预约类型不能为空");
        targets = List.copyOf(targets);
        claimedActors = List.copyOf(claimedActors);
        if (targets.isEmpty()
                || targets.stream().anyMatch(player -> player <= 0)
                || new HashSet<>(targets).size() != targets.size())
            throw new IllegalArgumentException("合法目标必须为不重复的正数: " + targets);
        if (type == SelectionType.VOTE && !claimedActors.isEmpty())
            throw new IllegalArgumentException("指定投票不能包含 CO 技能行动者");
        if (claimedActors.stream().map(ClaimedRoleActor::playerNumber).distinct().count()
                != claimedActors.size())
            throw new IllegalArgumentException("CO 技能行动者不能重复");
    }
}
