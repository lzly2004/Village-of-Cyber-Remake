package com.vocr.core;

import com.vocr.logging.DebugLogger;

import com.vocr.domain.IntPair;
import com.vocr.domain.peiyi;
import com.vocr.domain.Role;

import com.vocr.config.GameConstants;

import java.util.ArrayList;
import java.util.List;

/** 非咒杀场景下，协调非人、占卜师与灵媒师的首日 CO。 */
final class RegularClaimCoordinator
{
    private final GameContext ctx;
    private final COManager coManager;
    private final OpeningClaimCandidateBuilder claimCandidateBuilder;
    private final OpeningBlackResultResponsePlanner blackResultResponsePlanner;
    private final Runnable gylogic;

    RegularClaimCoordinator(GameContext ctx, COManager coManager, Runnable gylogic)
    {
        this.ctx = ctx;
        this.coManager = coManager;
        this.claimCandidateBuilder = new OpeningClaimCandidateBuilder(ctx);
        this.blackResultResponsePlanner = new OpeningBlackResultResponsePlanner(ctx);
        this.gylogic = gylogic;
    }

    /**
     * 普通场景上下文 — 封装各阶段的中间状态
     */
    private static class RegularClaimContext {
        int playerCount;
        boolean sharedClaimPending;
        ArrayList<IntPair> blackResponses;
        boolean hasCatClaim;
        int pendingSeerClaims;
        int pendingMediumClaims;
    }

    void coordinate(List<Integer> nightDeaths)
    {
        RegularClaimContext context = new RegularClaimContext();
        context.playerCount = ctx.getPlayerSum();
        context.sharedClaimPending = ctx.getPeiyi() != peiyi.jianyi
                && ctx.getActualRole(nightDeaths.get(0)) == 4;
        context.blackResponses = new ArrayList<>();

        // Phase 1: 初始CO设置（占/灵列表构建 + 共逻辑）
        executeInitialCOSetup();

        // Phase 2: 裁剪占CO（最多3人）
        trimSeerCOToMax3();

        // Phase 3: 黑球占/灵CO时机
        processBlackBallCOSecondary(nightDeaths);

        // Phase 4: 处理接黒回复
        if(ctx.getZhans().isEmpty()) {
            DebugLogger.log("本次无占，共逻辑");
            gylogic.run();
        } else {
            blackResultResponsePlanner.collectResponses(context.playerCount, context.blackResponses);
        }

        // Phase 5: 处理猫又CO响应 + 裁剪响应 + 处理剩余响应
        processCatCOResponses(context, nightDeaths);

        // Phase 6: 询问猫又CO
        if(context.hasCatClaim)
            coManager.askCoByRole(Role.mao);

        // Phase 7: 处理未CO玩家的剩余占/灵计划
        handleRemainingCOForUnclaimed(context);

        // Phase 8: 白球占/灵CO时机（条件触发）
        executeWhiteBallCOConditional(context, nightDeaths);
    }

    private void executeInitialCOSetup()
    {
        claimCandidateBuilder.build();
        DebugLogger.log("非人的初始工作：共逻辑");
        gylogic.run();
        DebugLogger.log("非人的初始工作：真占（已在占卜co阶段加入，跳过重复添加）");
    }

    private void trimSeerCOToMax3()
    {
        ctx.shuffleZhans();
        while(ctx.getZhans().size() > 3)
        {
            ctx.shuffleZhans();
            int removed = ctx.getZhans().get(0);
            if(removed == ctx.getActualRoleIndex(1)) continue;
            ctx.recordSkillTarget(removed, 1, 0);
            ctx.clearNonHumanPlan(removed);
            ctx.removeClaimCandidate(1, removed);
        }
    }

    private void processBlackBallCOSecondary(List<Integer> nightDeaths)
    {
        DebugLogger.log("非人的初始工作：黑球占co时机");
        for (int zhan : ctx.getZhans()) coManager.processActualCo(zhan, 1, nightDeaths);
        for (int ling : ctx.getLings()) coManager.processActualCo(ling, 2, nightDeaths);
    }

    /**
     * Phase 5: 处理猫又CO响应、裁剪响应、处理剩余响应
     */
    private void processCatCOResponses(RegularClaimContext context, List<Integer> nightDeaths)
    {
        DebugLogger.log("本次有无猫又co：");
        context.blackResponses = GameLogicUtils.shuffleList(context.blackResponses);

        // 统计已CO的狼队占/灵数量
        int lzsum = 0, llsum = 0;
        for(int i=1;i<=ctx.getInitialWolfCount();i++)
        {
            if(ctx.getClaimedRole(ctx.getRlIndex(i)) == 1)
                lzsum++;
            else if(ctx.getClaimedRole(ctx.getRlIndex(i)) == 2)
                llsum++;
        }

        // 过滤猫又CO响应
        for(int i=0;i<context.blackResponses.size();i++)
        {
            if(context.blackResponses.get(i).first() != 7) continue;
            if(context.blackResponses.get(i).second() == 2)
            {
                if(llsum > 0)
                {
                    int removedPlayer = context.blackResponses.get(i).first();
                    context.blackResponses.remove(i);
                    ctx.clearNonHumanPlan(removedPlayer);
                    i--;
                    continue;
                }
                llsum++;
            }
            if(context.blackResponses.get(i).second() == 1)
            {
                if(lzsum > 1)
                {
                    int removedPlayer = context.blackResponses.get(i).first();
                    context.blackResponses.remove(i);
                    ctx.clearNonHumanPlan(removedPlayer);
                    i--;
                    continue;
                }
                lzsum++;
            }
        }

        // 统计实际要上占/灵的数量
        int rezhan = 0, reling = 0;
        for(int i=0;i<context.blackResponses.size();i++)
        {
            if(context.blackResponses.get(i).second() == 1)
                rezhan++;
            else if(context.blackResponses.get(i).second() == 2)
                reling++;
        }

        // 裁剪占CO（最多4人）
        while(ctx.getZhans().size() + rezhan > 4 && !context.blackResponses.isEmpty())
        {
            context.blackResponses = GameLogicUtils.shuffleList(context.blackResponses);
            if(context.blackResponses.get(0).second() == 1
                    && context.blackResponses.get(0).first() != ctx.getActualRoleIndex(1))
            {
                ctx.clearNonHumanPlan(context.blackResponses.get(0).first());
                ctx.recordSkillTarget(context.blackResponses.get(0).first(), 1, 0);
                context.blackResponses.remove(0);
            }
        }

        // 裁剪灵CO（最多3人）
        while(ctx.getLings().size() + reling > 3 && !context.blackResponses.isEmpty())
        {
            context.blackResponses = GameLogicUtils.shuffleList(context.blackResponses);
            if(context.blackResponses.get(0).second() == 2
                    && context.blackResponses.get(0).first() != ctx.getActualRoleIndex(2))
            {
                ctx.clearNonHumanPlan(context.blackResponses.get(0).first());
                context.blackResponses.remove(0);
            }
        }

        // 处理剩余响应
        for(int i=0;i<context.blackResponses.size();i++)
        {
            int respPlayer = context.blackResponses.get(i).first();
            int role = context.blackResponses.get(i).second();
            boolean alreadyProcessed = false;
            if(role == 1 && ctx.getZhans().contains(respPlayer)) alreadyProcessed = true;
            if(role == 2 && ctx.getLings().contains(respPlayer)) alreadyProcessed = true;

            ctx.addLazySuspicionValue(respPlayer, GameConstants.SUSPICION_INCREASE_BLACK_RESPONSE);
            if(!alreadyProcessed)
                coManager.processActualCo(respPlayer, role, nightDeaths);
            if(role == 5)
            {
                context.hasCatClaim = true;
            }
        }
    }

    private void handleRemainingCOForUnclaimed(RegularClaimContext context)
    {
        int playerCount = context.playerCount;
        for(int i=1;i<=playerCount;i++)
        {
            if(ctx.isAlive(i) && ctx.getClaimedRole(i) == 0)
            {
                NonHumanPlan plan = ctx.getNonHumanPlan(i);
                if(ctx.getActualRole(i) == 1 || plan == NonHumanPlan.CLAIM_SEER)
                    context.pendingSeerClaims++;
                else if(ctx.getActualRole(i) == 2 || plan == NonHumanPlan.CLAIM_MEDIUM)
                    context.pendingMediumClaims++;
            }
        }
        while(ctx.getZhans().size() + context.pendingSeerClaims > 5
                || ctx.getLings().size() + context.pendingMediumClaims > 4)
        {
            int target = ConstNum.randomInt(1, playerCount);
            NonHumanPlan plan = ctx.getNonHumanPlan(target);
            if(ctx.getClaimedRole(target) == 0 && plan.isSeerOrMediumClaim())
            {
                if(ctx.getZhans().size() + context.pendingSeerClaims > 5
                        && plan == NonHumanPlan.CLAIM_SEER)
                {
                    context.pendingSeerClaims--;
                    ctx.clearNonHumanPlan(target);
                    ctx.recordSkillTarget(target, 1, 0);
                }
                else if(ctx.getLings().size() + context.pendingMediumClaims > 4
                        && plan == NonHumanPlan.CLAIM_MEDIUM)
                {
                    context.pendingMediumClaims--;
                    ctx.clearNonHumanPlan(target);
                }
            }
        }
    }

    private void executeWhiteBallCOConditional(RegularClaimContext context, List<Integer> nightDeaths)
    {
        if(ctx.getZhans().isEmpty() || !context.blackResponses.isEmpty()
                || context.sharedClaimPending || context.hasCatClaim)
        {
            DebugLogger.log("非人的初始工作：白球占、灵能co");
            for(int i=1;i<=context.playerCount;i++)
            {
                if(ctx.getClaimedRole(i) != 0 || ctx.isDead(i)) continue;
                if(ctx.getActualRole(i) < 3)
                {
                    coManager.processActualCo(i, ctx.getActualRole(i), nightDeaths);
                }
                else if(ctx.getNonHumanPlan(i).isSeerOrMediumClaim())
                {
                    NonHumanPlan plan = ctx.getNonHumanPlan(i);
                    if(plan == NonHumanPlan.CLAIM_SEER)
                    {
                        ctx.recordSkillTarget(i, 1, ctx.getPlannedDivinationResult(i));
                        DebugLogger.log("添加占文：角色" + i + " 占文" + ctx.getSkillTarget(i, 1));
                    }
                    coManager.processActualCo(i, plan.claimedRole(), nightDeaths);
                }
            }
            if((ctx.getGyIndex(1) <= 0 || ctx.getClaimedRole(ctx.getGyIndex(1)) != 4) &&
               (ctx.getGyIndex(2) <= 0 || ctx.getClaimedRole(ctx.getGyIndex(2)) != 4))
                gylogic.run();
        }
        context.blackResponses.clear();
    }

}
