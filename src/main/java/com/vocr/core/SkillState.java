package com.vocr.core;

import com.vocr.config.GameConstants;

/**
 * 技能状态的唯一存储：技能历史、职业预告、潜伏预告和非人占文计划。
 */
final class SkillState
{
    private final int playerSum;
    private final int[][] skillTargets;
    private final boolean[][][] claimedRoleSchedules;
    private final boolean[][] hiddenSeerSchedule;
    private final boolean[][] hiddenHunterSchedule;
    private final int[] preliminaryDivinationTargets;
    private final int[] plannedDivinationResults;

    SkillState(int playerSum)
    {
        if (playerSum < 1)
            throw new IllegalArgumentException("玩家人数必须为正数: " + playerSum);
        this.playerSum = playerSum;
        this.skillTargets = new int[playerSum + 1][GameConstants.MAX_GAME_DAYS + 1];
        this.claimedRoleSchedules = new boolean[playerSum + 1]
                [GameConstants.CHARACTER_SUM + 1][GameConstants.MAX_GAME_DAYS + 1];
        this.hiddenSeerSchedule = new boolean[playerSum + 1][GameConstants.MAX_GAME_DAYS + 1];
        this.hiddenHunterSchedule = new boolean[playerSum + 1][GameConstants.MAX_GAME_DAYS + 1];
        this.preliminaryDivinationTargets = new int[playerSum + 1];
        this.plannedDivinationResults = new int[playerSum * 2 + 1];
    }

    int getSkillTarget(int player, int day)
    {
        validatePlayer(player, "技能使用者");
        validateDay(day);
        return skillTargets[player][day];
    }

    void recordSkillTarget(int player, int day, int target)
    {
        validatePlayer(player, "技能使用者");
        validateDay(day);
        validateEncodedTarget(target);
        skillTargets[player][day] = target;
    }

    int getPreliminaryDivinationTarget(int player)
    {
        validatePlayer(player, "预备占文使用者");
        return preliminaryDivinationTargets[player];
    }

    void recordPreliminaryDivinationTarget(int player, int target)
    {
        validatePlayer(player, "预备占文使用者");
        validatePlayer(target, "预备占文目标");
        preliminaryDivinationTargets[player] = target;
    }

    int getPlannedDivinationResult(int player)
    {
        validatePlayer(player, "计划占文使用者");
        return plannedDivinationResults[player];
    }

    void recordPlannedDivinationResult(int player, int target)
    {
        validatePlayer(player, "计划占文使用者");
        validateEncodedTarget(target);
        plannedDivinationResults[player] = target;
    }

    boolean isClaimedRoleScheduled(int player, int target, int day)
    {
        validateSchedule(player, target, day);
        return claimedRoleSchedules[player][target][day];
    }

    void setClaimedRoleScheduled(int player, int target, int day, boolean scheduled)
    {
        validateSchedule(player, target, day);
        claimedRoleSchedules[player][target][day] = scheduled;
    }

    boolean isHiddenSeerScheduled(int target, int day)
    {
        validateHiddenSchedule(target, day);
        return hiddenSeerSchedule[target][day];
    }

    void setHiddenSeerScheduled(int target, int day, boolean scheduled)
    {
        validateHiddenSchedule(target, day);
        hiddenSeerSchedule[target][day] = scheduled;
    }

    boolean isHiddenHunterScheduled(int target, int day)
    {
        validateHiddenSchedule(target, day);
        return hiddenHunterSchedule[target][day];
    }

    void setHiddenHunterScheduled(int target, int day, boolean scheduled)
    {
        validateHiddenSchedule(target, day);
        hiddenHunterSchedule[target][day] = scheduled;
    }

    int[] getSkillTargetsSnapshot(int player)
    {
        validatePlayer(player, "技能使用者");
        return skillTargets[player].clone();
    }

    int[] getPreliminaryDivinationTargetsSnapshot() { return preliminaryDivinationTargets.clone(); }
    int[] getPlannedDivinationResultsSnapshot() { return plannedDivinationResults.clone(); }
    boolean[][] getClaimedRoleScheduleSnapshot(int player)
    {
        validatePlayer(player, "CO 技能使用者");
        return copyMatrix(claimedRoleSchedules[player]);
    }
    boolean[][] getHiddenSeerScheduleSnapshot() { return copyMatrix(hiddenSeerSchedule); }
    boolean[][] getHiddenHunterScheduleSnapshot() { return copyMatrix(hiddenHunterSchedule); }

    private void validateSchedule(int player, int target, int day)
    {
        validatePlayer(player, "CO 技能使用者");
        validateHiddenSchedule(target, day);
    }

    private void validateHiddenSchedule(int target, int day)
    {
        validatePlayer(target, "预定技能目标");
        validateDay(day);
    }

    private void validatePlayer(int player, String stateName)
    {
        if (player < 1 || player > playerSum)
            throw new IllegalArgumentException(stateName + "索引越界: " + player);
    }

    private void validateDay(int day)
    {
        if (day < 1 || day > GameConstants.MAX_GAME_DAYS)
            throw new IllegalArgumentException("技能日期越界: " + day);
    }

    private void validateEncodedTarget(int target)
    {
        if (target < 0 || target > playerSum * 2)
            throw new IllegalArgumentException("技能目标编码越界: " + target);
    }

    private static boolean[][] copyMatrix(boolean[][] source)
    {
        boolean[][] snapshot = new boolean[source.length][];
        for (int i = 0; i < source.length; i++)
            snapshot[i] = source[i].clone();
        return snapshot;
    }
}
