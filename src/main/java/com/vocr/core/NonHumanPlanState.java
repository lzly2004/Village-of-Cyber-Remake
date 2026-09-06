package com.vocr.core;

import java.util.Arrays;

/** 非人公开身份计划的唯一存储。 */
final class NonHumanPlanState
{
    private final int playerSum;
    private final NonHumanPlan[] plans;

    NonHumanPlanState(int playerSum)
    {
        if (playerSum < 1)
            throw new IllegalArgumentException("玩家人数必须为正数: " + playerSum);
        this.playerSum = playerSum;
        this.plans = new NonHumanPlan[playerSum + 1];
        Arrays.fill(plans, NonHumanPlan.NONE);
    }

    NonHumanPlan get(int player)
    {
        validatePlayer(player);
        return plans[player];
    }

    void set(int player, NonHumanPlan plan)
    {
        validatePlayer(player);
        if (plan == null)
            throw new IllegalArgumentException("非人计划不能为空");
        plans[player] = plan;
    }

    void clear(int player)
    {
        set(player, NonHumanPlan.NONE);
    }

    private void validatePlayer(int player)
    {
        if (player < 1 || player > playerSum)
            throw new IllegalArgumentException("非人计划玩家索引越界: " + player);
    }
}
