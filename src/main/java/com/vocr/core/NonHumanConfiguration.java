package com.vocr.core;

import com.vocr.domain.Role;
import com.vocr.domain.peiyi;

/** 一局游戏开局时确定的非人阵营配置；构造后不再变化。 */
record NonHumanConfiguration(
        int initialWolfCount,
        int initialNonHumanCount,
        Role leaderRole,
        int aggression)
{
    private static final int MIN_AGGRESSION = 0;
    private static final int MAX_AGGRESSION = 5;

    NonHumanConfiguration
    {
        if (initialWolfCount < 1)
            throw new IllegalArgumentException("初始人狼数必须为正数: " + initialWolfCount);
        if (initialNonHumanCount <= initialWolfCount)
            throw new IllegalArgumentException("初始非人数必须多于人狼数: " + initialNonHumanCount);
        if (leaderRole != Role.kuangren && leaderRole != Role.kuangxinzhe)
            throw new IllegalArgumentException("非人统领必须是狂人或狂信者: " + leaderRole);
        if (aggression < MIN_AGGRESSION || aggression > MAX_AGGRESSION)
            throw new IllegalArgumentException("非人激进度越界: " + aggression);
    }

    static NonHumanConfiguration from(peiyi configuration, int aggression)
    {
        if (configuration == null)
            throw new IllegalArgumentException("配役不能为空");
        return switch (configuration)
        {
            case NONE -> throw new IllegalArgumentException("配役必须是可开局配置");
            case jianyi -> new NonHumanConfiguration(2, 3, Role.kuangren, aggression);
            case tongchang -> new NonHumanConfiguration(3, 4, Role.kuangren, aggression);
            case yaoohu -> new NonHumanConfiguration(3, 5, Role.kuangren, aggression);
            case kuangxin -> new NonHumanConfiguration(3, 5, Role.kuangxinzhe, aggression);
            case beide -> new NonHumanConfiguration(3, 6, Role.kuangren, aggression);
            case maoyou -> new NonHumanConfiguration(4, 6, Role.kuangren, aggression);
            case daxing -> new NonHumanConfiguration(4, 7, Role.kuangxinzhe, aggression);
        };
    }

    boolean usesFanatic()
    {
        return leaderRole == Role.kuangxinzhe;
    }
}
