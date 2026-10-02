package com.vocr.application;

import java.util.HashSet;
import java.util.List;

/** 一名公开 CO 技能行动者当前预约的目标。 */
public record ClaimedActorTargets(
        int playerNumber,
        int claimedRoleOrder,
        List<Integer> targets)
{
    public ClaimedActorTargets
    {
        if (playerNumber <= 0 || claimedRoleOrder <= 0)
            throw new IllegalArgumentException("CO 行动者编号与顺位必须为正数");
        targets = List.copyOf(targets);
        if (targets.stream().anyMatch(target -> target <= 0)
                || new HashSet<>(targets).size() != targets.size())
            throw new IllegalArgumentException("预约目标必须为不重复的正数: " + targets);
    }
}
