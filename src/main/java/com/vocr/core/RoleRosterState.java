package com.vocr.core;

import com.vocr.domain.Role;

/** 开局角色名册；按玩家编号构建一次，此后只读。 */
final class RoleRosterState
{
    private static final int MAX_SHARED_PLAYERS = 2;

    private final int playerSum;
    private final int[] playerByRole;
    private final int[] sharedPlayers;
    private final int[] wolfPlayers;
    private final int sharedCount;
    private final int wolfCount;

    RoleRosterState(int[] rolesByPlayer)
    {
        if (rolesByPlayer == null || rolesByPlayer.length < 2)
            throw new IllegalArgumentException("角色名册至少需要一名玩家");

        this.playerSum = rolesByPlayer.length - 1;
        this.playerByRole = new int[Role.values().length];
        this.sharedPlayers = new int[MAX_SHARED_PLAYERS + 1];
        this.wolfPlayers = new int[rolesByPlayer.length];

        int currentSharedCount = 0;
        int currentWolfCount = 0;
        for (int player = 1; player <= playerSum; player++)
        {
            int role = rolesByPlayer[player];
            validateRole(role);
            playerByRole[role] = player;

            if (role == Role.gong.ordinal())
            {
                if (currentSharedCount == MAX_SHARED_PLAYERS)
                    throw new IllegalArgumentException("共有者人数超过名册容量: " + (currentSharedCount + 1));
                sharedPlayers[++currentSharedCount] = player;
            }
            else if (role == Role.renlang.ordinal())
            {
                wolfPlayers[++currentWolfCount] = player;
            }
        }
        this.sharedCount = currentSharedCount;
        this.wolfCount = currentWolfCount;
    }

    int getPlayerByRole(int role)
    {
        validateRole(role);
        return playerByRole[role];
    }

    int getSharedPlayer(int order)
    {
        if (order < 1 || order > MAX_SHARED_PLAYERS)
            throw new IllegalArgumentException("共有者顺位越界: " + order);
        return sharedPlayers[order];
    }

    int getWolfPlayer(int order)
    {
        if (order < 1 || order > playerSum)
            throw new IllegalArgumentException("人狼顺位越界: " + order);
        return wolfPlayers[order];
    }

    int getSharedCount()
    {
        return sharedCount;
    }

    int getWolfCount()
    {
        return wolfCount;
    }

    private static void validateRole(int role)
    {
        if (role < 1 || role >= Role.values().length)
            throw new IllegalArgumentException("职业索引越界: " + role);
    }
}
