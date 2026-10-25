package com.vocr.application;

import com.vocr.domain.SelectionType;

import java.util.Objects;

/** 为一名已 CO 的占卜师或猎人更新其技能目标。 */
public record SetClaimedRoleTargetSelectionCommand(
        long expectedRevision,
        SelectionType type,
        int claimedPlayer,
        int target,
        boolean selected) implements RevisionedCommand
{
    public SetClaimedRoleTargetSelectionCommand
    {
        RevisionedCommand.requireValid(expectedRevision);
        Objects.requireNonNull(type, "选择类型不能为空");
        if (type == SelectionType.VOTE)
            throw new IllegalArgumentException("指定投票没有单独的 CO 行动者");
        if (claimedPlayer <= 0)
            throw new IllegalArgumentException("CO 玩家编号必须为正数: " + claimedPlayer);
        if (target <= 0)
            throw new IllegalArgumentException("目标玩家编号必须为正数: " + target);
    }
}
