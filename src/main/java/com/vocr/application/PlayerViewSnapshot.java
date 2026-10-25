package com.vocr.application;

import com.vocr.domain.GameResult;
import com.vocr.domain.peiyi;

import java.util.List;
import java.util.Objects;

/**
 * 面向普通玩家界面的白名单状态投影。
 *
 * 不包含实际身份、怀疑度、技能结果、非人计划或任何仅供核心使用的秘密。
 */
public record PlayerViewSnapshot(
        peiyi configuration,
        int gameDay,
        int aliveCount,
        int deathCount,
        GameResult result,
        List<VisiblePlayer> players)
{
    public PlayerViewSnapshot
    {
        if (configuration == null || configuration == peiyi.NONE)
            throw new IllegalArgumentException("玩家视图必须包含有效配役");
        if (gameDay <= 0 || aliveCount < 0 || deathCount < 0)
            throw new IllegalArgumentException("日期与玩家计数必须有效");
        Objects.requireNonNull(result, "胜负状态不能为空");
        players = List.copyOf(players);
        if (players.size() != aliveCount + deathCount)
            throw new IllegalArgumentException("玩家列表数量与存亡计数不一致");
    }
}
