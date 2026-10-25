package com.vocr.core;

import com.vocr.config.GameConstants;

import java.util.ArrayList;
import java.util.List;

/** 本夜尸体快照及逐日夜间死亡摘要的唯一存储。 */
final class NightDeathState
{
    private final int playerSum;
    private final int[] deathCounts;
    private final boolean[] doubleDeathOccurred;
    private List<Integer> latestDeaths = List.of();

    NightDeathState(int playerSum)
    {
        if (playerSum < 1)
            throw new IllegalArgumentException("玩家人数必须为正数: " + playerSum);
        this.playerSum = playerSum;
        this.deathCounts = new int[GameConstants.MAX_GAME_DAYS + 1];
        this.doubleDeathOccurred = new boolean[GameConstants.MAX_GAME_DAYS + 1];
    }

    List<Integer> getLatestDeaths()
    {
        return List.copyOf(latestDeaths);
    }

    int getDeathCount(int day)
    {
        validateQueryDay(day);
        return deathCounts[day];
    }

    boolean isDoubleDeathOccurred(int day)
    {
        validateQueryDay(day);
        return doubleDeathOccurred[day];
    }

    /** 原子替换最新尸体快照，并同步指定日期的计数与双死标记。 */
    void recordNightDeaths(int day, List<Integer> deaths)
    {
        validateRecordDay(day);
        if (deaths == null)
            throw new IllegalArgumentException("夜间死亡列表不能为空");

        boolean[] seen = new boolean[playerSum + 1];
        ArrayList<Integer> snapshot = new ArrayList<>(deaths.size());
        for (Integer player : deaths)
        {
            if (player == null || player < 1 || player > playerSum)
                throw new IllegalArgumentException("夜间死亡玩家索引无效: " + player);
            if (seen[player])
                throw new IllegalArgumentException("夜间死亡列表包含重复玩家: " + player);
            seen[player] = true;
            snapshot.add(player);
        }

        latestDeaths = List.copyOf(snapshot);
        deathCounts[day] = snapshot.size();
        doubleDeathOccurred[day] = snapshot.size() > 1;
    }

    /** 是否已经出现足以确认猫又出局的夜间死亡组合。 */
    boolean hasDefinitiveCatEvidenceThrough(int day)
    {
        validateQueryDay(day);
        int doubleDeathCount = 0;
        for (int currentDay = 1; currentDay <= day; currentDay++)
        {
            if (doubleDeathOccurred[currentDay])
                doubleDeathCount++;
            if (doubleDeathCount >= 2 || deathCounts[currentDay] >= 4)
                return true;
        }
        return false;
    }

    private static void validateRecordDay(int day)
    {
        if (day < 1 || day > GameConstants.MAX_GAME_DAYS)
            throw new IllegalArgumentException("夜间死亡日期越界: " + day);
    }

    private static void validateQueryDay(int day)
    {
        if (day < 0 || day > GameConstants.MAX_GAME_DAYS)
            throw new IllegalArgumentException("夜间死亡日期越界: " + day);
    }
}
