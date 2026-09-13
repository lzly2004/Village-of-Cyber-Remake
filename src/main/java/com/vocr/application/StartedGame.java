package com.vocr.application;

import com.vocr.domain.peiyi;

/** 开局后可安全返回给展示层的最小摘要，不包含玩家身份等秘密。 */
public record StartedGame(peiyi configuration, int playerCount, int gameDay)
{
    public StartedGame
    {
        if (configuration == null || configuration == peiyi.NONE)
            throw new IllegalArgumentException("开局结果必须包含有效配役");
        if (playerCount <= 0)
            throw new IllegalArgumentException("玩家人数必须大于零");
        if (gameDay <= 0)
            throw new IllegalArgumentException("游戏日期必须大于零");
    }
}
