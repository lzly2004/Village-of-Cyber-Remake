package com.vocr.application;

/** 已公开声称技能职业、可以接受单独目标预约的玩家。 */
public record ClaimedRoleActor(int playerNumber, int claimedRoleOrder)
{
    public ClaimedRoleActor
    {
        if (playerNumber <= 0 || claimedRoleOrder <= 0)
            throw new IllegalArgumentException("CO 行动者编号与顺位必须为正数");
    }
}
