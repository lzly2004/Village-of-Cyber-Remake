package com.vocr.core;

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
    private final Runnable gylogic;

    RegularClaimCoordinator(GameContext ctx, COManager coManager, Runnable gylogic)
    {
        this.ctx = ctx;
        this.coManager = coManager;
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
            handleBlackResponses(context.playerCount, context.blackResponses);
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
        DebugLogger.log("非人的初始工作：占卜co");
        buildSeerCOList();
        DebugLogger.log("非人的初始工作：灵能co");
        buildMediumCOList();
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

    private void buildSeerCOList()
    {
        DebugLogger.log("非人的初始工作：狼占");
        for (int i=1;i<=ctx.getInitialWolfCount();i++)
        {
            if(ctx.isDead(ctx.getRlIndex(i))) continue;
            if(ctx.getNonHumanPlan(ctx.getRlIndex(i)) == NonHumanPlan.CLAIM_SEER)
            {
                ctx.addClaimCandidate(1, ctx.getRlIndex(i));
                ctx.recordSkillTarget(ctx.getRlIndex(i), 1, ctx.getPlannedDivinationResult(ctx.getRlIndex(i)));
            }
        }
        DebugLogger.log("非人的初始工作：狂占信占");
        if(ctx.getNonHumanLeader() > 0
                && ctx.getNonHumanPlan(ctx.getNonHumanLeader()) == NonHumanPlan.CLAIM_SEER)
        {
            ctx.addClaimCandidate(1, ctx.getNonHumanLeader());
            ctx.recordSkillTarget(ctx.getNonHumanLeader(), 1, ctx.getPlannedDivinationResult(ctx.getNonHumanLeader()));
        }
        DebugLogger.log("非人的初始工作：狐占");
        if(ctx.getFox() > 0 && ctx.getNonHumanPlan(ctx.getFox()) == NonHumanPlan.CLAIM_SEER)
        {
            ctx.addClaimCandidate(1, ctx.getFox());
            ctx.recordSkillTarget(ctx.getFox(), 1, ctx.getPlannedDivinationResult(ctx.getFox()));
        }
        DebugLogger.log("非人的初始工作：背占");
        if(ctx.getDeviant() > 0 && ctx.getNonHumanPlan(ctx.getDeviant()) == NonHumanPlan.CLAIM_SEER)
        {
            ctx.addClaimCandidate(1, ctx.getDeviant());
            ctx.recordSkillTarget(ctx.getDeviant(), 1, ctx.getPlannedDivinationResult(ctx.getDeviant()));
        }
        DebugLogger.log("非人的初始工作：真占");
        if(ctx.isAlive(ctx.getActualRoleIndex(1)))
        {
            ctx.addClaimCandidate(1, ctx.getActualRoleIndex(1));
        }
    }

    private void buildMediumCOList()
    {
        DebugLogger.log("非人的初始工作：狼灵");
        for(int i=1;i<=ctx.getInitialWolfCount();i++)
        {
            if(ctx.isDead(ctx.getRlIndex(i))) continue;
            if(ctx.getNonHumanPlan(ctx.getRlIndex(i)) == NonHumanPlan.CLAIM_MEDIUM)
            {
                ctx.addClaimCandidate(2, ctx.getRlIndex(i));
                break;
            }
        }
        DebugLogger.log("非人的初始工作：狂灵信灵");
        if(ctx.getNonHumanLeader() > 0 && ctx.isAlive(ctx.getNonHumanLeader())
                && ctx.getNonHumanPlan(ctx.getNonHumanLeader()) == NonHumanPlan.CLAIM_MEDIUM)
        {
            ctx.addClaimCandidate(2, ctx.getNonHumanLeader());
        }
        DebugLogger.log("非人的初始工作：狐灵");
        if(ctx.getFox() > 0 && ctx.getNonHumanPlan(ctx.getFox()) == NonHumanPlan.CLAIM_MEDIUM)
        {
            ctx.addClaimCandidate(2, ctx.getFox());
        }
        DebugLogger.log("非人的初始工作：背灵");
        if(ctx.getDeviant() > 0 && ctx.getNonHumanPlan(ctx.getDeviant()) == NonHumanPlan.CLAIM_MEDIUM)
        {
            ctx.addClaimCandidate(2, ctx.getDeviant());
        }
        DebugLogger.log("非人的初始工作：真灵");
        if(ctx.isAlive(ctx.getActualRoleIndex(2)))
        {
            ctx.addClaimCandidate(2, ctx.getActualRoleIndex(2));
        }
    }

    private void handleBlackResponses(int n, ArrayList<IntPair> response)
    {
        boolean morelzll = false,morelmll = false;
        ArrayList<Integer> bplayers = new ArrayList<>();
        for(int i=0;i<ctx.getZhans().size();i++)
        {
            int zhan = ctx.getZhans().get(i);
            if(ctx.getSkillTarget(zhan,1) <= n) continue;
            int bplayer = ctx.getSkillTarget(zhan,1) - n;
            if(!bplayers.contains(bplayer))
                bplayers.add(bplayer);
        }
        for(int j=0;j<bplayers.size();j++)
        {
            int bplayer = bplayers.get(j);
            if(ctx.isDead(bplayer)) continue;
            if(ctx.getActualRole(bplayer) < 7)
            {
                response.add(new IntPair(bplayer,ctx.getActualRole(bplayer)));
            }
            else if(ctx.getActualRole(bplayer) == 7)
            {
                NonHumanPlan plan = ctx.getNonHumanPlan(bplayer);
                if(plan.isClaimPlan())
                {
                    response.add(new IntPair(bplayer, plan.claimedRole()));
                }
                else
                {
                    int lz = 0,ll = 0;
                    int lzweight = 10,llweight = 10,lmlweight = 10,lwcoweight = 10;
        for(int i=1;i<=ctx.getInitialWolfCount();i++)
                        if(ctx.getClaimedRole(ctx.getRlIndex(i)) == 1)
                            lz++;
                        else if(ctx.getClaimedRole(ctx.getRlIndex(i)) == 2)
                            ll++;
                    if(lz > 1)  lzweight = 0;
                    if(ll > 0)  llweight = 0;
                    if(morelzll)
                    {
                        lzweight = 0;
                        llweight = 0;
                    }
                    if(morelmll)
                    {
                        lmlweight = 0;
                    }
                    if(ctx.getPeiyi() == peiyi.jianyi && (ctx.getZhans().contains(ctx.getRlIndex(1)) || ctx.getZhans().contains(ctx.getRlIndex(2))))
                    {
                        lzweight = 0;
                        llweight = 0;
                        lmlweight = 0;
                    }
                    int option = GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(lzweight,llweight,lmlweight,lwcoweight)));
                    switch(option)
                    {
                        case 0:
                            ctx.recordSkillTarget(bplayer, 1, ctx.getPreliminaryDivinationTarget(bplayer));
                            response.add(new IntPair(bplayer,1));
                            morelzll = true;
                            break;
                        case 1:
                            response.add(new IntPair(bplayer,2));
                            morelzll = true;
                            for(int k=1;k<=ctx.getInitialWolfCount();k++)
                            {
                                NonHumanPlan wolfPlan = ctx.getNonHumanPlan(ctx.getRlIndex(k));
                                if(wolfPlan == NonHumanPlan.CLAIM_MEDIUM
                                        && !bplayers.contains(wolfPlan.legacyCode()))
                                    ctx.setNonHumanPlan(ctx.getRlIndex(k), NonHumanPlan.STAY_HIDDEN);
                            }
                            break;
                        case 2:
                            int role = 5 - GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(50 + 50 * ctx.getLies().size(),50+50 * ctx.getMaos().size()))) * 2;
                        if(ctx.getCat() == 0 || ctx.isCatDefinitivelyOut()) role = 3;
                        NonHumanPlan responsePlan = NonHumanPlan.claimRole(role);
                        ctx.setNonHumanPlan(bplayer, responsePlan);
                        if(responsePlan == NonHumanPlan.CLAIM_CAT)
                            response.add(new IntPair(bplayer,5));
                        morelmll = true;
                        break;
                        case 3:
                            ctx.clearNonHumanPlan(bplayer);
                            break;
                    }
                }
            }
            else
            {
                DebugLogger.log("白非接黒并且没有之前的计划->狂狐背");
                int option = 0;
                switch(bplayer)
                {
                    case 8: case 9:
                    option = GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(95,5,0,0)));break;
                    case 10:option = GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(80,5,10,5)));break;
                    case 11:
                    {
                        int aggression = ctx.getNonHumanAggression();
                        option = GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(
                                45 + aggression, 30, 20, 5 - aggression)));
                        break;
                    }
                }
                switch(option)
                {
                    case 0:
                        ctx.recordSkillTarget(bplayer, 1, ctx.getPreliminaryDivinationTarget(bplayer));
                        response.add(new IntPair(bplayer,1));
                        break;
                    case 1:
                        response.add(new IntPair(bplayer,2));
                        break;
                    case 2:
                        int role = 5 - GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(50 + 50 * ctx.getLies().size(),50+50 * ctx.getMaos().size()))) * 2;
                        if(ctx.getCat() == 0 || ctx.isCatDefinitivelyOut()) role = 3;
                        NonHumanPlan responsePlan = NonHumanPlan.claimRole(role);
                        ctx.setNonHumanPlan(bplayer, responsePlan);
                        if(responsePlan == NonHumanPlan.CLAIM_CAT)
                            response.add(new IntPair(bplayer,5));
                        break;
                    case 3:
                        ctx.clearNonHumanPlan(bplayer);
                        break;
                }
            }
        }
    }
}
