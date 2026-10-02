package com.vocr.application;

import com.vocr.domain.GameContextView;
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
        LegalTargetSelection legalAction = LegalActionProjector.targetSelections(context).stream()
                .filter(action -> action.type() == type)
                .findFirst()
                .orElseThrow();
        List<Integer> hiddenTargets = selectedTargets(context, target -> switch (type)
        {
            case DIVINATION -> context.isHiddenSeerScheduled(target, day);
            case GUARD -> context.isHiddenHunterScheduled(target, day);
            case VOTE -> throw new IllegalArgumentException("指定投票不属于技能预约");
        });
        List<ClaimedActorTargets> claimedActors = new ArrayList<>();
        for (ClaimedRoleActor actor : legalAction.claimedActors())
        {
            claimedActors.add(new ClaimedActorTargets(
                    actor.playerNumber(),
                    actor.claimedRoleOrder(),
                    selectedTargets(context, target -> context.isClaimedRoleScheduled(
                            actor.playerNumber(), target, day))));
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
