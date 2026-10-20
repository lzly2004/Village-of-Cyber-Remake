package com.vocr.application;

/** 单轮投票中一名公开玩家的投票目标与所得票数。 */
public record VoteLineSnapshot(
        int playerNumber,
        int receivedVotes,
        int targetPlayerNumber)
{
    public VoteLineSnapshot
    {
        if (playerNumber <= 0 || targetPlayerNumber <= 0)
            throw new IllegalArgumentException("投票玩家与目标编号必须为正数");
        if (receivedVotes < 0)
            throw new IllegalArgumentException("所得票数不能为负数");
    }
}
