package com.vocr.application;

import com.vocr.domain.SelectionType;

import java.util.Objects;

/** 更新指定投票目标，或为全部相关占卜/护卫行动者更新同一目标。 */
public record SetAllTargetSelectionCommand(
        SelectionType type,
        int target,
        boolean selected)
{
    public SetAllTargetSelectionCommand
    {
        Objects.requireNonNull(type, "选择类型不能为空");
        if (target <= 0)
            throw new IllegalArgumentException("目标玩家编号必须为正数: " + target);
    }
}
