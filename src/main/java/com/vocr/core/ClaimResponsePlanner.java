package com.vocr.core;

import com.vocr.logging.DebugLogger;

import com.vocr.config.ProbabilityCalculator;
import com.vocr.domain.IntPair;
import com.vocr.domain.Role;
import com.vocr.domain.peiyi;
import com.vocr.domain.whyDie;

import java.util.ArrayList;
import java.util.List;

/** 根据职业询问或指定玩家列表规划 CO 响应，不执行实际 CO。 */
final class ClaimResponsePlanner
{
    private final GameContext ctx;
    private final ProbabilityCalculator probabilityCalculator;

    ClaimResponsePlanner(GameContext ctx, ProbabilityCalculator probabilityCalculator)
    {
        this.ctx = ctx;
        this.probabilityCalculator = probabilityCalculator;
    }

    /** 猫又 CO 概率参数 p1：狼队中已 CO 且非默认 CO（claimedRole=6）的人数。 */
    int getP1(int role)
    {
        if (role != 7) return 0;
        int claimedWolfCount = 0;
        for (int wolfOrder = 1; wolfOrder <= ctx.getInitialWolfCount(); wolfOrder++)
        {
            int claimedRole = ctx.getClaimedRole(ctx.getRlIndex(wolfOrder));
            if (claimedRole != 0 && claimedRole != 6)
                claimedWolfCount++;
        }
        return claimedWolfCount;
    }

    /** 猫又 CO 概率参数 p2：职业 7/9 返回狼队生存人数减一，其余返回死亡进度档位。 */
    int getP2(int role)
    {
        if (role == 7 || role == 9)
        {
            int livingWolfCount = 0;
            for (int wolfOrder = 1; wolfOrder <= ctx.getInitialWolfCount(); wolfOrder++)
                if (ctx.isAlive(ctx.getRlIndex(wolfOrder)))
                    livingWolfCount++;
            return livingWolfCount - 1;
        }
        return deathCounterP2();
    }

    List<IntPair> planResponsesForRole(Role claimedRole)
    {
        int playerSum = ctx.getPlayerSum();
        int gameDay = ctx.getGameDay();
        DebugLogger.log("询问职业：" + claimedRole);
        int role = claimedRole.ordinal();
        if (!ctx.tryRecordClaimRequest(role, gameDay))
            return List.of();
        if (role > 3 && role < 6)
        {
            if (ctx.getPeiyi() == peiyi.jianyi) return List.of();
            if (role == 5 && ctx.getPeiyi() != peiyi.maoyou && ctx.getPeiyi() != peiyi.daxing)
                return List.of();
        }
        if (role == 5 && !canRequestCatClaim(playerSum, gameDay))
            return List.of();

        ArrayList<IntPair> responses = new ArrayList<>();
        for (int player = 1; player <= playerSum; player++)
        {
            if (ctx.isDead(player) || ctx.getClaimedRole(player) != 0) continue;
            if (ctx.getActualRole(player) < 7)
            {
                if (ctx.getActualRole(player) == role)
                    responses.add(new IntPair(player, role));
                continue;
            }
            NonHumanPlan plan = ctx.getNonHumanPlan(player);
            if (plan.claimsRole(role))
            {
                if (gameDay == 2)
                    ctx.recordSkillTarget(player, 1, ctx.getPlannedDivinationResult(player));
                responses.add(new IntPair(player, role));
                continue;
            }
            if (role < 3 || role == 4 || plan != NonHumanPlan.NONE) continue;
            switch (ctx.getActualRole(player))
            {
                case 7:
                    if (role == 3 && ctx.isWolfClaimedHunter()) break;
                    if (role == 5 && ctx.isWolfClaimedCat()) break;
                    int probability = probabilityCalculator.maolieco(0, 3, getP1(7), getP2(7));
                    if (GameLogicUtils.probabilityJudge(probability))
                    {
                        responses.add(new IntPair(player, role));
                        ctx.markWolfClaimedRole(role);
                    }
                    break;
                default:
                    if (GameLogicUtils.probabilityJudge(probabilityCalculator.maolieco(
                            ctx.getActualRole(player) - 7, 3,
                            getP1(ctx.getActualRole(player)), getP2(ctx.getActualRole(player)))))
                    {
                        responses.add(new IntPair(player, role));
                    }
                    break;
            }
        }
        responses = GameLogicUtils.shuffleList(responses);
        if (claimedRole == Role.gong && responses.size() > 1)
            responses.remove(1);
        return responses;
    }

    List<IntPair> planResponsesForPlayers(List<Integer> askedPlayers)
    {
        ArrayList<IntPair> responses = new ArrayList<>();
        int probability;
        for (int player : askedPlayers)
        {
            if (ctx.getClaimedRole(player) != 0) continue;
            if (ctx.getActualRole(player) < 7)
            {
                responses.add(new IntPair(player, ctx.getActualRole(player)));
            }
            else if (ctx.getNonHumanPlan(player).isClaimPlan())
            {
                NonHumanPlan plan = ctx.getNonHumanPlan(player);
                if (plan == NonHumanPlan.CLAIM_SEER)
                    ctx.recordSkillTarget(player, 1, ctx.getPlannedDivinationResult(player));
                responses.add(new IntPair(player, plan.claimedRole()));
            }
            else
            {
                int option = 0;
                switch (ctx.getActualRole(player))
                {
                    case 7:
                        if (ctx.isWolfClaimedHunter() && ctx.isWolfClaimedCat())
                        {
                            responses.add(new IntPair(player, 6));
                            break;
                        }
                        probability = probabilityCalculator.maolieco(0, 2, getP1(7), getP2(7));
                        if (!GameLogicUtils.probabilityJudge(probability))
                        {
                            responses.add(new IntPair(player, 6));
                            break;
                        }
                        if (ctx.isWolfClaimedHunter())
                        {
                            // 如果猫又已明确出局，不能 CO 猫。
                            if (ctx.getActualRoleIndex(5) != 0 && !ctx.isCatDefinitivelyOut())
                                responses.add(new IntPair(player, 5));
                            else
                                responses.add(new IntPair(player, 6));
                            ctx.markWolfClaimedRole(5);
                            break;
                        }
                        if (ctx.isWolfClaimedCat())
                        {
                            responses.add(new IntPair(player, 3));
                            ctx.markWolfClaimedRole(3);
                            break;
                        }
                        option = 0;
                        // 如果猫又已明确出局，不能 CO 猫。
                        if (ctx.getActualRoleIndex(5) != 0 && !ctx.isCatDefinitivelyOut())
                            option = GameLogicUtils.getEventIndexByProbability(
                                    new ArrayList<>(List.of(50 + 50 * ctx.getMaos().size(),
                                            50 + 50 * ctx.getLies().size())));
                        responses.add(new IntPair(player, 3 + 2 * option));
                        break;
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                        probability = probabilityCalculator.maolieco(ctx.getActualRole(player) - 7, 2,
                                getP1(ctx.getActualRole(player)), getP2(ctx.getActualRole(player)));
                        if (!GameLogicUtils.probabilityJudge(probability))
                        {
                            responses.add(new IntPair(player, 6));
                            break;
                        }
                        option = 0;
                        // 如果猫又已明确出局，不能 CO 猫。
                        if (ctx.getActualRoleIndex(5) != 0 && !ctx.isCatDefinitivelyOut())
                            option = GameLogicUtils.getEventIndexByProbability(
                                    new ArrayList<>(List.of(50 + 50 * ctx.getMaos().size(),
                                            50 + 50 * ctx.getLies().size())));
                        responses.add(new IntPair(player, 3 + 2 * option));
                        break;
                }
            }
        }
        return responses;
    }

    private int deathCounterP2()
    {
        if (ctx.getDeathCounter() <= 3) return 0;
        if (ctx.getDeathCounter() <= ctx.getAliveCounter()) return 1;
        if (ctx.getAliveCounter() > 6) return 2;
        return 3;
    }

    private boolean canRequestCatClaim(int playerSum, int gameDay)
    {
        int doubleDeathCount = 0;
        for (int day = 1; day <= gameDay; day++)
        {
            if (ctx.isDoubleDeathOccurred(day))
                doubleDeathCount++;
        }
        if (doubleDeathCount == 2 && ctx.getDeathReason(ctx.getActualRoleIndex(5)) != whyDie.NONE)
            return false;
        if (!ctx.isDoubleDeathOccurred(gameDay - 1)
                || ctx.getDeathReason(ctx.getActualRoleIndex(5)) == whyDie.NONE)
            return true;

        boolean canClaim = false;
        if (gameDay == 3 && ctx.getClaimedRoleAskDay(1) != 2)
        {
            for (int player = 1; player <= playerSum; player++)
            {
                if (ctx.isHiddenSeerScheduled(player, gameDay - 1)
                        && ctx.getDiebody().contains(player))
                    canClaim = true;
            }
        }
        for (int claimedSeer : ctx.getZhans())
        {
            if (ctx.isNonHumanMarked(claimedSeer)) continue;
            if (ctx.isAlive(claimedSeer))
            {
                if (ctx.getDiebody().contains(ctx.getSkillTarget(claimedSeer, gameDay - 1)))
                    canClaim = true;
            }
            else
            {
                boolean hasScheduledTarget = false;
                for (int target = 1; target <= playerSum; target++)
                {
                    if (ctx.isClaimedRoleScheduled(claimedSeer, target, gameDay - 1))
                    {
                        hasScheduledTarget = true;
                        if (ctx.getDiebody().contains(target))
                            canClaim = true;
                    }
                }
                if (!hasScheduledTarget)
                    canClaim = true;
            }
        }
        return canClaim;
    }
}
