package com.vocr.application;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** 询问一组指定玩家是否 CO。 */
public record AskClaimByPlayersCommand(List<Integer> players)
{
    public AskClaimByPlayersCommand
    {
        Objects.requireNonNull(players, "询问玩家列表不能为空");
        players = List.copyOf(players);
        if (players.isEmpty())
            throw new IllegalArgumentException("至少需要询问一名玩家");
        if (players.stream().anyMatch(player -> player <= 0))
            throw new IllegalArgumentException("玩家编号必须为正数: " + players);
        if (new HashSet<>(players).size() != players.size())
            throw new IllegalArgumentException("询问玩家不能重复: " + players);
    }
}
