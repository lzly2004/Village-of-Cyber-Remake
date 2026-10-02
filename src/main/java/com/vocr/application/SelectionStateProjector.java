package com.vocr.application;

import com.vocr.domain.GameContextView;
import com.vocr.domain.Role;
import com.vocr.domain.SelectionType;

import java.util.ArrayList;
import java.util.List;

/** 从完整对局状态白名单投影当前可公开的目标预约。 */
final class SelectionStateProjector
{
    private SelectionStateProjector() { }

    static SelectionStateSnapshot project(
            GameContextView context, boolean avoidanceEnabled)
    {
        int day = context.getGameDay();
        return new SelectionStateSnapshot(
                avoidanceEnabled,
                selectedTargets(context, target -> context.isSelectedVoteTarget(target, day)),
                skillSelections(context, SelectionType.DIVINATION, day),
                skillSelections(context, SelectionType.GUARD, day));
    }

    private static SkillTargetSelections skillSelections(
            GameContextView context, SelectionType type, int day)
    {
        Role claimedRole = switch (type)
        {
            case DIVINATION -> Role.zhan;
            case GUARD -> Role.lie;
            case VOTE -> throw new IllegalArgumentException("指定投票不属于技能预约");
        };
        List<Integer> hiddenTargets = selectedTargets(context, target -> switch (type)
        {
            case DIVINATION -> context.isHiddenSeerScheduled(target, day);
            case GUARD -> context.isHiddenHunterScheduled(target, day);
            case VOTE -> throw new IllegalArgumentException("指定投票不属于技能预约");
        });
        List<ClaimedActorTargets> claimedActors = new ArrayList<>();
        for (int player = 1; player <= context.getPlayerSum(); player++)
        {
            if (!context.isAlive(player)
                    || context.getClaimedRole(player) != claimedRole.ordinal())
                continue;
            int actor = player;
            claimedActors.add(new ClaimedActorTargets(
                    actor,
                    context.getClaimedRoleOrder(actor),
                    selectedTargets(context, target -> context.isClaimedRoleScheduled(
                            actor, target, day))));
        }
        return new SkillTargetSelections(hiddenTargets, claimedActors);
    }

    private static List<Integer> selectedTargets(
            GameContextView context, TargetPredicate predicate)
    {
        List<Integer> targets = new ArrayList<>();
        for (int target = 1; target <= context.getPlayerSum(); target++)
            if (predicate.isSelected(target))
                targets.add(target);
        return List.copyOf(targets);
    }

    @FunctionalInterface
    private interface TargetPredicate
    {
        boolean isSelected(int target);
    }
}
