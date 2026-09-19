package com.vocr.application;

import com.vocr.domain.CharacterEnglishName;
import com.vocr.domain.Role;
import com.vocr.domain.whyDie;

import java.util.Objects;

/** 普通对局界面可以看到的单个玩家公开状态。 */
public record VisiblePlayer(
        int playerNumber,
        CharacterEnglishName character,
        boolean alive,
        whyDie deathReason,
        int deathDay,
        Role claimedRole,
        int claimedRoleOrder,
        int comingOutDay)
{
    public VisiblePlayer
    {
        if (playerNumber <= 0)
            throw new IllegalArgumentException("玩家编号必须大于零");
        Objects.requireNonNull(character, "角色外观不能为空");
        Objects.requireNonNull(deathReason, "死亡原因不能为空");
        Objects.requireNonNull(claimedRole, "公开声称职业不能为空");
        if (deathDay < 0 || claimedRoleOrder < 0 || comingOutDay < 0)
            throw new IllegalArgumentException("日期与公开声称顺序不能为负数");
    }
}
