package com.vocr.core;

import com.vocr.domain.whyDie;

/** 汇总人狼选择咬杀目标时需要的场况，不执行权重规则或目标选择。 */
final class WolfBiteAnalyzer
{
    static final class Analysis
    {
        int aliveWolfCount;
        int bittenSeerAccurateBonus;
        int bittenSeerNotAccurateBonus;
        boolean[] plausibleSeers;
        boolean[] plausibleHunters;
        boolean[] plausibleMediums;
        int plausibleSeerCount;
        int plausibleHunterCount;
        int plausibleMediumCount;
        int[] whiteBallsBySeer;
        double hunterGuardFactor;
        int hiddenHunterTargetCount;
        boolean[] hiddenHunterTargets;
        int[] whiteBallsReceived;
    }

    private final GameContext ctx;

    WolfBiteAnalyzer(GameContext ctx)
    {
        this.ctx = ctx;
    }

    Analysis analyze(int playerSum, int gameDay)
    {
        Analysis analysis = new Analysis();
        analysis.aliveWolfCount = countAliveWolves();
        analyzeFirstDaySeerBite(playerSum, analysis);
        analyzeRealRoles(playerSum, gameDay, analysis);
        countWhiteBalls(playerSum, gameDay, analysis);
        calculateHunterGuardFactor(playerSum, gameDay, analysis);
        countGlobalWhiteBalls(playerSum, gameDay, analysis);
        return analysis;
    }

    private int countAliveWolves()
    {
        int aliveWolfCount = 0;
        for (int wolfOrder = 1; wolfOrder <= ctx.getInitialWolfCount(); wolfOrder++)
            if (ctx.getDeathReason(ctx.getRlIndex(wolfOrder)) == whyDie.NONE)
                aliveWolfCount++;
        return aliveWolfCount;
    }

    private void analyzeFirstDaySeerBite(int playerSum, Analysis analysis)
    {
        int y = 0;
        int z = 0;
        int bittenSeer = ctx.getDiePlayerNum(whyDie.beiyao, 2);
        if (bittenSeer != -1 && ctx.getClaimedRole(bittenSeer) == 1)
        {
            if (ctx.getSkillTarget(bittenSeer, 1) > playerSum)
            {
                if (ctx.getActualRole(ctx.getSkillTarget(bittenSeer, 1) - playerSum) == 7) y = 1;
            }
            else
            {
                if (ctx.getActualRole(ctx.getSkillTarget(bittenSeer, 1)) != 7) y = 1;
            }
        }
        if (y == 0) z = 1;
        analysis.bittenSeerAccurateBonus = y;
        analysis.bittenSeerNotAccurateBonus = z;
    }

    private void analyzeRealRoles(int playerSum, int gameDay, Analysis analysis)
    {
        int realSeerCount = 0;
        int realHunterCount = 0;
        int realMediumCount = 0;
        boolean[] isSeer = new boolean[playerSum + 1];
        boolean[] isHunter = new boolean[playerSum + 1];
        boolean[] isMedium = new boolean[playerSum + 1];

        for (int player = 1; player <= playerSum; player++)
        {
            if (ctx.getClaimedRole(player) == 2 && ctx.getActualRole(player) != 7
                    && !ctx.isNonHumanMarked(player)
                    && hasFactionConsistentResults(player, playerSum, gameDay))
            {
                isMedium[player] = true;
                realMediumCount++;
            }
            if (ctx.getDeathDay(player) != 0) continue;
            if (ctx.getClaimedRole(player) == 3 && ctx.getActualRole(player) != 7
                    && !ctx.isNonHumanMarked(player))
            {
                isHunter[player] = true;
                realHunterCount++;
            }
            if (ctx.getClaimedRole(player) == 1 && ctx.getActualRole(player) != 7
                    && !ctx.isNonHumanMarked(player)
                    && hasFactionConsistentResults(player, playerSum, gameDay))
            {
                isSeer[player] = true;
                realSeerCount++;
            }
        }

        analysis.plausibleSeers = isSeer;
        analysis.plausibleHunters = isHunter;
        analysis.plausibleMediums = isMedium;
        analysis.plausibleSeerCount = realSeerCount;
        analysis.plausibleHunterCount = realHunterCount;
        analysis.plausibleMediumCount = realMediumCount;
    }

    private boolean hasFactionConsistentResults(int player, int playerSum, int gameDay)
    {
        for (int day = 1; day <= gameDay; day++)
        {
            boolean blackResult = ctx.getSkillTarget(player, day) > playerSum;
            if ((blackResult && ctx.getActualRole(player) != 7)
                    || (!blackResult && ctx.getActualRole(player) == 7))
                return false;
        }
        return true;
    }

    private void countWhiteBalls(int playerSum, int gameDay, Analysis analysis)
    {
        int[] whiteBallCounts = new int[playerSum + 1];
        for (int player = 1; player <= playerSum; player++)
        {
            if (ctx.getClaimedRole(player) != 1) continue;
            for (int day = 1; day < gameDay; day++)
            {
                int target = ctx.getSkillTarget(player, day);
                if (target <= playerSum && target > 0 && !ctx.isNonHumanMarked(target)
                        && ctx.getClaimedRole(target) != 1 && !ctx.isAckWhite(target))
                    whiteBallCounts[player]++;
            }
        }
        analysis.whiteBallsBySeer = whiteBallCounts;
    }

    private void calculateHunterGuardFactor(int playerSum, int gameDay, Analysis analysis)
    {
        double guardFactor;
        switch (analysis.plausibleHunterCount)
        {
            case 0: guardFactor = 0.8; break;
            case 1: guardFactor = 0.5; break;
            case 2: guardFactor = 0.3; break;
            case 3: guardFactor = 0.1; break;
            default: guardFactor = 0; break;
        }
        int unclaimedCount = 0;
        int livingUnclaimedCount = 0;
        for (int player = 1; player <= playerSum; player++)
        {
            if (ctx.getActualRole(player) == 7 || Math.abs(ctx.getClaimedRole(player) - 3) < 3)
                continue;
            unclaimedCount++;
            if (ctx.getDeathReason(player) == whyDie.NONE) livingUnclaimedCount++;
        }
        guardFactor *= livingUnclaimedCount;
        guardFactor /= Math.max(unclaimedCount, 1);
        if (ctx.getClaimedRoleAskDay(3) > 0) guardFactor = 0;

        int hiddenHunterCount = 0;
        boolean[] hiddenHunterTargets = new boolean[playerSum + 1];
        for (int player = 1; player <= playerSum; player++)
        {
            if (ctx.isHiddenHunterScheduled(player, gameDay))
            {
                hiddenHunterTargets[player] = true;
                hiddenHunterCount++;
            }
        }

        analysis.hunterGuardFactor = guardFactor;
        analysis.hiddenHunterTargetCount = hiddenHunterCount;
        analysis.hiddenHunterTargets = hiddenHunterTargets;
    }

    private void countGlobalWhiteBalls(int playerSum, int gameDay, Analysis analysis)
    {
        int[] receivedWhiteBallCounts = new int[playerSum + 1];
        for (int claimedSeer : ctx.getZhans())
        {
            for (int day = 1; day <= gameDay; day++)
            {
                int target = ctx.getSkillTarget(claimedSeer, day);
                if (target >= 1 && target <= playerSum) receivedWhiteBallCounts[target]++;
            }
        }
        analysis.whiteBallsReceived = receivedWhiteBallCounts;
    }
}
