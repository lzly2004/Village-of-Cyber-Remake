package com.vocr.application;

import com.vocr.domain.peiyi;

/** 开局后可安全返回给展示层的最小摘要，不包含玩家身份等秘密。 */
public record StartedGame(long revision, peiyi configuration, int playerCount, int gameDay)
{
    public StartedGame
    {
        if (revision <= 0)
            throw new IllegalArgumentException("开局后的状态版本必须为正数");
        if (configuration == null || configuration == peiyi.NONE)
            throw new IllegalArgumentException("开局结果必须包含有效配役");
        if (playerCount <= 0)
            throw new IllegalArgumentException("玩家人数必须大于零");
        if (gameDay <= 0)
            throw new IllegalArgumentException("游戏日期必须大于零");
    }
}
