package com.vocr.application;

import java.util.HashSet;
import java.util.List;

/** 一类技能的匿名潜伏预约，以及各公开 CO 行动者的预约。 */
public record SkillTargetSelections(
        List<Integer> hiddenTargets,
        List<ClaimedActorTargets> claimedActors)
{
    public SkillTargetSelections
    {
        hiddenTargets = List.copyOf(hiddenTargets);
        claimedActors = List.copyOf(claimedActors);
        if (hiddenTargets.stream().anyMatch(target -> target <= 0)
                || new HashSet<>(hiddenTargets).size() != hiddenTargets.size())
            throw new IllegalArgumentException(
                    "潜伏技能预约目标必须为不重复的正数: " + hiddenTargets);
        if (claimedActors.stream().map(ClaimedActorTargets::playerNumber).distinct().count()
                != claimedActors.size())
            throw new IllegalArgumentException("公开 CO 技能行动者不能重复");
    }

    public boolean hasSelections()
    {
        return !hiddenTargets.isEmpty()
                || claimedActors.stream().anyMatch(actor -> !actor.targets().isEmpty());
    }

    public boolean isTargetSelected(int target)
    {
        return hiddenTargets.contains(target)
                || claimedActors.stream().anyMatch(actor -> actor.targets().contains(target));
    }
}
