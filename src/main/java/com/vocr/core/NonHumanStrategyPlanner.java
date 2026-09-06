package com.vocr.core;

import com.vocr.config.GameConstants;

import java.util.ArrayList;
import java.util.List;

/** 负责首个白天前的非人公开身份计划、预备占文和伪造占文。 */
final class NonHumanStrategyPlanner
{
    private final GameContext ctx;
    private final SuspicionSystem suspicion;

    NonHumanStrategyPlanner(GameContext ctx, SuspicionSystem suspicion)
    {
        this.ctx = ctx;
        this.suspicion = suspicion;
    }

    void plan(List<Integer> nightDeaths)
    {
        int playerSum = ctx.getPlayerSum();
        generatePreliminaryDivinations(playerSum);
        initNonHumanPlans();
        int wolfSeerCount = assignWolfSeerTargets(playerSum, nightDeaths);
        assignWolfMediumTargets(wolfSeerCount);
        assignMadmanFanaticStrategy(playerSum, nightDeaths);
        assignFoxStrategy(playerSum, nightDeaths);
        assignDeviantStrategy(playerSum, nightDeaths);
    }

    private void generatePreliminaryDivinations(int playerSum)
    {
        DebugLogger.log("非人的初始工作：所有非人都生成一份预备占文");
        for(int i=1;i<=playerSum;i++)
        {
            if(GameLogicUtils.feiren(ctx.getActualRole(i)) == 0) continue;
            int[] weight = new int[playerSum+1];
            for(int j=1;j<=playerSum;j++)
            {
                if(i == j)
                {
                    weight[j] = -GameConstants.INF;
                    continue;
                }
                if(ctx.getActualRole(i) != 7 && ctx.getActualRole(i) != 9)
                {
                    weight[j] = 1;
                    continue;
                }
                if(ctx.getActualRole(j) != 7) weight[j] = 1;
                else weight[j] = -GameConstants.INF;
            }
            ctx.recordPreliminaryDivinationTarget(i, suspicion.getOne(weight));
            DebugLogger.log("非人的预备占文，非人:"+i+",预备占文:"+ctx.getPreliminaryDivinationTarget(i));
        }
    }

    private void initNonHumanPlans()
    {
        DebugLogger.log("非人的初始工作：非人决定自己的计划倾向");
        for(int i=1;i<=ctx.getInitialWolfCount();i++)
        {
            DebugLogger.log("rlindex[i]:" + ctx.getRlIndex(i));
            if(ctx.isDead(ctx.getRlIndex(i))) continue;
            ctx.setNonHumanPlan(ctx.getRlIndex(i), NonHumanPlan.STAY_HIDDEN);
        }
    }

    private int assignWolfSeerTargets(int playerSum, List<Integer> nightDeaths)
    {
        DebugLogger.log("非人的初始工作：人狼上占");
        int aggression = ctx.getNonHumanAggression();
        int wolfCount = ctx.getInitialWolfCount();
        int wolfSeerCount = GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(
                10 - aggression, (11 - wolfCount) * 15, aggression + (wolfCount - 2) * 3)));
        int remainingWolfSeers = wolfSeerCount;
        while(remainingWolfSeers != 0)
        {
            int wolfOrder = ConstNum.randomInt(1, wolfCount);
            int wolf = ctx.getRlIndex(wolfOrder);
            if(ctx.isDead(wolf) || ctx.getPlannedDivinationResult(wolf) != 0) continue;
            remainingWolfSeers--;
            ctx.setNonHumanPlan(wolf, NonHumanPlan.CLAIM_SEER);
            int option = GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(
                    80 - 2 * aggression, 15, 5 + aggression, aggression)));
            int[] weight = new int[playerSum+1];
            for(int i=1;i<=playerSum;i++)
            {
                if((option == 1 || option == 2) && ctx.getActualRole(i) == 7 && wolf != i)
                {
                    weight[i] = 1;
                }
                else if((option == 0 || option == 3) && ctx.getActualRole(i) != 7
                        && ctx.getPreliminaryDivinationTarget(wolf) != i)
                {
                    weight[i] = 1;
                }
                else weight[i] = -GameConstants.INF;
            }
            int target = suspicion.getOne(weight);
            ctx.recordPlannedDivinationResult(wolf,
                    option == 0 || option == 2 || nightDeaths.contains(target) ? target : target + playerSum);
            DebugLogger.log("人狼上占的占文，人狼："+wolfOrder+",占文："+ ctx.getPlannedDivinationResult(wolf));
        }
        return wolfSeerCount;
    }

    private void assignWolfMediumTargets(int wolfSeerCount)
    {
        DebugLogger.log("非人的初始工作：人狼上灵");
        int probability;
        int aggression = ctx.getNonHumanAggression();
        if(ctx.getInitialWolfCount() == 2)
        {
            if(wolfSeerCount == 0) probability = aggression + 30;
            else if(wolfSeerCount == 1) probability = 0;
            else probability = 0;
        }
        else if(ctx.getInitialWolfCount() == 3)
        {
            if(wolfSeerCount == 0) probability = aggression + 50;
            else if(wolfSeerCount == 1) probability = aggression + 30;
            else probability = 0;
        }
        else
        {
            if(wolfSeerCount == 0) probability = aggression + 60;
            else if(wolfSeerCount == 1) probability = aggression + 30;
            else probability = aggression + 10;
        }
        if(GameLogicUtils.probabilityJudge(probability))
        {
            while(true)
            {
                int wolfOrder = ConstNum.randomInt(1, ctx.getInitialWolfCount());
                int wolf = ctx.getRlIndex(wolfOrder);
                if(ctx.isDead(wolf) || ctx.getPlannedDivinationResult(wolf) != 0) continue;
                ctx.setNonHumanPlan(wolf, NonHumanPlan.CLAIM_MEDIUM);
                break;
            }
        }
    }

    private void assignMadmanFanaticStrategy(int playerSum, List<Integer> nightDeaths)
    {
        DebugLogger.log("非人的初始工作：狂人狂信策略");
        List<Integer> strategyWeights = ctx.usesFanatic()
                ? List.of(55, 35, 10)
                : List.of(35, 60, 5);
        int planOption = GameLogicUtils.getEventIndexByProbability(new ArrayList<>(strategyWeights));
        int leader = ctx.getNonHumanLeader();
        if(planOption == 1)
            ctx.setNonHumanPlan(leader, NonHumanPlan.CLAIM_MEDIUM);
        else if(planOption == 2)
            ctx.setNonHumanPlan(leader, NonHumanPlan.STAY_HIDDEN);
        else
        {
            ctx.setNonHumanPlan(leader, NonHumanPlan.CLAIM_SEER);
            if(!ctx.usesFanatic())
            {
                int aggression = ctx.getNonHumanAggression();
                int target, option = GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(
                        75 - aggression, 25 + aggression)));
                while(true)
                {
                    target = ConstNum.randomInt(1,playerSum);
                    DebugLogger.log("狂人占文对象：" + target);
                    if(target == ctx.getPreliminaryDivinationTarget(leader) || target == leader) continue;
                    ctx.recordPlannedDivinationResult(leader,
                            option == 0 || nightDeaths.contains(target) ? target : target + playerSum);
                    break;
                }
            }
            else
            {
                int aggression = ctx.getNonHumanAggression();
                int target, option = GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(
                        70 - 2 * aggression, 20, aggression + 10, aggression)));
                while(true)
                {
                    target = ConstNum.randomInt(1,playerSum);
                    if(target == ctx.getPreliminaryDivinationTarget(leader) || target == leader) continue;
                    if(ctx.getActualRole(target) == 7 && option < 2) continue;
                    if(ctx.getActualRole(target) != 7 && option > 1) continue;
                    DebugLogger.log("狂信占文对象：" + target);
                    ctx.recordPlannedDivinationResult(leader,
                            option == 0 || option == 2 || nightDeaths.contains(target) ? target : target + playerSum);
                    DebugLogger.log("狂信占文：" + ctx.getPlannedDivinationResult(leader));
                    break;
                }
            }
        }
    }

    private void assignFoxStrategy(int playerSum, List<Integer> nightDeaths)
    {
        DebugLogger.log("非人的初始工作：妖狐策略");
        if(ctx.getFox() > 0 && ctx.isAlive(ctx.getFox()))
        {
            int aggression = ctx.getNonHumanAggression();
            int planOption = GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(
                    5 + aggression, aggression, 95 - 2 * aggression)));
            if(planOption == 1)
                ctx.setNonHumanPlan(ctx.getFox(), NonHumanPlan.CLAIM_MEDIUM);
            else if(planOption == 2)
                ctx.setNonHumanPlan(ctx.getFox(), NonHumanPlan.STAY_HIDDEN);
            else
            {
                ctx.setNonHumanPlan(ctx.getFox(), NonHumanPlan.CLAIM_SEER);
                int target, option = GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(
                        90 - aggression, 10 + aggression)));
                while (true)
                {
                    target = ConstNum.randomInt(1, playerSum);
                    if (target == ctx.getPreliminaryDivinationTarget(ctx.getFox()) || target == ctx.getFox()) continue;
                    DebugLogger.log("妖狐占文对象：" + target);
                    ctx.recordPlannedDivinationResult(ctx.getFox(),
                            option == 0 || nightDeaths.contains(target) ? target : target + playerSum);
                    DebugLogger.log("妖狐占文：" + ctx.getPlannedDivinationResult(ctx.getFox()));
                    break;
                }
            }
        }
    }

    private void assignDeviantStrategy(int playerSum, List<Integer> nightDeaths)
    {
        DebugLogger.log("非人的初始工作：背德策略");
        if(ctx.getDeviant() > 0 && ctx.isAlive(ctx.getDeviant()))
        {
            int aggression = ctx.getNonHumanAggression();
            int planOption = GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(
                    50 + aggression, 5, 45 - aggression)));
            if(planOption == 1)
                ctx.setNonHumanPlan(ctx.getDeviant(), NonHumanPlan.CLAIM_MEDIUM);
            else if(planOption == 2)
                ctx.setNonHumanPlan(ctx.getDeviant(), NonHumanPlan.STAY_HIDDEN);
            else
            {
                DebugLogger.log("背德开始选择占文");
                ctx.setNonHumanPlan(ctx.getDeviant(), NonHumanPlan.CLAIM_SEER);
                int target, option = GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(
                        60 - aggression, 20, 20 + aggression, 0)));
                if(option == 2)
                {
                    ctx.recordPlannedDivinationResult(ctx.getDeviant(), ctx.getFox());
                    DebugLogger.log("狐狸白球");
                }
                else
                    while (true)
                    {
                        target = ConstNum.randomInt(1, playerSum);
                        DebugLogger.log("背德选择占文:"+target);
                        if (target == ctx.getPreliminaryDivinationTarget(ctx.getDeviant())
                                || target == ctx.getDeviant())
                            continue;
                        if (ctx.getActualRole(target) == 10) continue;
                        ctx.recordPlannedDivinationResult(ctx.getDeviant(),
                                option == 0 || nightDeaths.contains(target) ? target : target + playerSum);
                        DebugLogger.log("背德占文:" + ctx.getPlannedDivinationResult(ctx.getDeviant()));
                        break;
                    }
            }
        }
    }
}
