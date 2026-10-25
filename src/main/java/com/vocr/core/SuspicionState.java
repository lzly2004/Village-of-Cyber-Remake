package com.vocr.core;

import com.vocr.config.GameConstants;

/**
 * 怀疑度状态的唯一存储：玩家间怀疑值、待结算变化和每日怀疑前三。
 */
final class SuspicionState
{
    private static final int TOP_SUSPECT_COUNT = GameConstants.TOP3_ARRAY_SIZE - 1;

    private final int playerSum;
    private final int[][] suspicionValues;
    private final int[] lazySuspicionValues;
    private final int[][][] topSuspectedPlayers;

    SuspicionState(int playerSum)
    {
        if (playerSum < 1)
            throw new IllegalArgumentException("玩家人数必须为正数: " + playerSum);

        this.playerSum = playerSum;
        this.suspicionValues = new int[playerSum + 1][playerSum + 1];
        this.lazySuspicionValues = new int[playerSum + 1];
        this.topSuspectedPlayers = new int[playerSum + 1]
                [GameConstants.TOP3_ARRAY_SIZE][GameConstants.MAX_GAME_DAYS + 1];

        for (int subject = 1; subject <= playerSum; subject++)
        {
            for (int target = 1; target <= playerSum; target++)
            {
                suspicionValues[subject][target] = subject == target
                        ? -GameConstants.INF : 50;
            }
        }
    }

    int getSuspicionValue(int subject, int target)
    {
        validatePair(subject, target);
        return suspicionValues[subject][target];
    }

    void setSuspicionValue(int subject, int target, int value)
    {
        validatePair(subject, target);
        suspicionValues[subject][target] = value;
    }

    void addSuspicionValue(int subject, int target, int delta)
    {
        validatePair(subject, target);
        suspicionValues[subject][target] += delta;
    }

    /** 在完整校验双方玩家后，一次更新两个方向的怀疑值。 */
    void addMutualSuspicion(int firstPlayer, int secondPlayer, int firstDelta, int secondDelta)
    {
        validatePlayer(firstPlayer, "玩家1");
        validatePlayer(secondPlayer, "玩家2");
        suspicionValues[firstPlayer][secondPlayer] += firstDelta;
        suspicionValues[secondPlayer][firstPlayer] += secondDelta;
    }

    int getLazySuspicionValue(int player)
    {
        validatePlayer(player, "惰性怀疑值玩家");
        return lazySuspicionValues[player];
    }

    void addLazySuspicionValue(int player, int delta)
    {
        validatePlayer(player, "惰性怀疑值玩家");
        lazySuspicionValues[player] += delta;
    }

    void resetLazySuspicionValues()
    {
        for (int player = 1; player <= playerSum; player++)
            lazySuspicionValues[player] = 0;
    }

    int getTopSuspectedPlayer(int player, int rank, int day)
    {
        validateTopSuspectCoordinates(player, rank, day);
        return topSuspectedPlayers[player][rank][day];
    }

    void setTopSuspectedPlayer(int player, int rank, int day, int target)
    {
        validateTopSuspectCoordinates(player, rank, day);
        if (target != 0)
            validatePlayer(target, "怀疑目标");
        topSuspectedPlayers[player][rank][day] = target;
    }

    private void validatePair(int subject, int target)
    {
        validatePlayer(subject, "主体玩家");
        validatePlayer(target, "目标玩家");
    }

    private void validateTopSuspectCoordinates(int player, int rank, int day)
    {
        validatePlayer(player, "怀疑排序玩家");
        if (rank < 1 || rank > TOP_SUSPECT_COUNT)
            throw new IllegalArgumentException("怀疑顺位越界: " + rank);
        if (day < 1 || day > GameConstants.MAX_GAME_DAYS)
            throw new IllegalArgumentException("怀疑日期越界: " + day);
    }

    private void validatePlayer(int player, String stateName)
    {
        if (player < 1 || player > playerSum)
            throw new IllegalArgumentException(stateName + "索引越界: " + player);
    }
}
