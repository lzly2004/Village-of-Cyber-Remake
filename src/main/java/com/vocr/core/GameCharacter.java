package com.vocr.core;

import com.vocr.domain.whyDie;

class GameCharacter
{//游戏角色封装类
    //角色编号
    int number;//青年1 研究2 ...
    //职业相关
    int actualRole;//真实职业 claimedRole
    private int claimedRole;//当前声称的职业 claimedRole
    private int claimedRoleOrder;//当前声称的职业是同职业声称者中的第几个,从1开始计数
    private int comingOutDay;//当前角色是第几天co出来的职业 默认值是-1 若当前角色是村人，此项无意义
    boolean nonHumanMarker;//角色是否已经明确破绽了
    //生死相关
    private whyDie whyDie;//死亡原因枚举类型
    private int dieDay;//死亡日
    //日期的定义：
    //游戏开始时，日期为1日
    //1日夜->2日昼->2日夜->3日昼->3日夜->4日昼->4日夜->...
    GameCharacter(int number,int actualRole)
    {
        this.number = number;
        this.actualRole = actualRole;
        this.claimedRole = 0;
        this.dieDay = 0;
        this.claimedRoleOrder = 0;
        this.nonHumanMarker = false;
        this.whyDie = whyDie.NONE;
        this.comingOutDay = -1;
    }

    boolean isAlive() { return whyDie == whyDie.NONE; }
    int getClaimedRole() { return claimedRole; }
    int getClaimedRoleOrder() { return claimedRoleOrder; }
    int getComingOutDay() { return comingOutDay; }
    whyDie getDeathReason() { return whyDie; }
    int getDeathDay() { return dieDay; }

    void recordClaimIdentity(int role, int day)
    {
        if (claimedRole == role) return;
        claimedRole = role;
        comingOutDay = day;
    }

    void assignClaimOrder(int order)
    {
        if (claimedRoleOrder == 0)
            claimedRoleOrder = order;
    }

    void recordDeath(whyDie reason, int day)
    {
        this.whyDie = reason;
        this.dieDay = day;
    }
}
