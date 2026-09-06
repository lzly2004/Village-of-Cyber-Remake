package com.vocr.core;

import com.vocr.domain.IntPair;
import com.vocr.domain.peiyi;
import com.vocr.domain.Role;

import com.vocr.config.GameConstants;

import java.util.ArrayList;
import java.util.List;

public class NonHumanCoordinator
{
    private final GameContext ctx;
    private final NonHumanStrategyPlanner strategyPlanner;
    private final CurseKillClaimCoordinator curseKillClaimCoordinator;
    private final COManager coManager;
    private final Runnable gylogic;

    public NonHumanCoordinator(GameModule module)
    {
        this.ctx = module.getCtx();
        SuspicionSystem suspicion = module.getSuspicion();
        this.strategyPlanner = new NonHumanStrategyPlanner(ctx, suspicion);
        this.coManager = module.getCoManager();
        this.gylogic = module.getGylogic();
        this.curseKillClaimCoordinator = new CurseKillClaimCoordinator(ctx, suspicion, gylogic);
    }

    public void coordinate(List<Integer> diebody)
    {
        int gd = ctx.getGameDay();
        strategyPlanner.plan(diebody);
        handleDaytimeCO(diebody, gd);
    }

    private void handleDaytimeCO(List<Integer> diebody, int gd)
    {
        DebugLogger.log("白天临时改变策略,以及占灵共的行动");
        if(diebody.size() > 1)
        {
            curseKillClaimCoordinator.coordinate(diebody);
        }
        else
        {
            handleNormalScenario(diebody, gd);
        }
    }

    // ============================================================
    // 普通场景（非咒杀日）协调者方法
    // ============================================================

    /**
     * 普通场景上下文 — 封装 handleNormalScenario 各阶段的中间状态
     */
    private static class NormalScenarioContext {
        int n;
        int gd;
        boolean gqian;                  // 是否有共欠CO时机
        ArrayList<IntPair> response;     // 接黒回复列表
        boolean havecatco;              // 是否有猫又CO
        int jhzhan;                    // 计划上占的人数
        int jhling;                    // 计划上灵的人数
    }

    private void handleNormalScenario(List<Integer> diebody, int gd)
    {
        NormalScenarioContext c = new NormalScenarioContext();
        c.n = ctx.getPlayerSum();
        c.gd = gd;
        c.gqian = ctx.getPeiyi() != peiyi.jianyi && ctx.getActualRole(diebody.get(0)) == 4;
        c.response = new ArrayList<>();
        c.havecatco = false;
        c.jhzhan = 0;
        c.jhling = 0;

        // Phase 1: 初始CO设置（占/灵列表构建 + 共逻辑）
        executeInitialCOSetup();

        // Phase 2: 裁剪占CO（最多3人）
        trimSeerCOToMax3();

        // Phase 3: 黑球占/灵CO时机
        processBlackBallCOSecondary(diebody);

        // Phase 4: 处理接黒回复
        if(ctx.getZhans().isEmpty()) {
            DebugLogger.log("本次无占，共逻辑");
            gylogic.run();
        } else {
            handleBlackResponses(c.n, c.response);
        }

        // Phase 5: 处理猫又CO响应 + 裁剪响应 + 处理剩余响应
        processCatCOResponses(c, diebody);

        // Phase 6: 询问猫又CO
        if(c.havecatco)
            coManager.askCoByRole(Role.mao);

        // Phase 7: 处理未CO玩家的剩余占/灵计划
        handleRemainingCOForUnclaimed(c);

        // Phase 8: 白球占/灵CO时机（条件触发）
        executeWhiteBallCOConditional(c, diebody);
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

    private void processBlackBallCOSecondary(List<Integer> diebody)
    {
        DebugLogger.log("非人的初始工作：黑球占co时机");
        for (int zhan : ctx.getZhans()) coManager.processActualCo(zhan, 1, diebody);
        for (int ling : ctx.getLings()) coManager.processActualCo(ling, 2, diebody);
    }

    /**
     * Phase 5: 处理猫又CO响应、裁剪响应、处理剩余响应
     */
    private void processCatCOResponses(NormalScenarioContext c, List<Integer> diebody)
    {
        DebugLogger.log("本次有无猫又co：");
        c.response = GameLogicUtils.shuffleList(c.response);

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
        for(int i=0;i<c.response.size();i++)
        {
            if(c.response.get(i).first() != 7) continue;
            if(c.response.get(i).second() == 2)
            {
                if(llsum > 0)
                {
                    int removedPlayer = c.response.get(i).first();
                    c.response.remove(i);
                    ctx.clearNonHumanPlan(removedPlayer);
                    i--;
                    continue;
                }
                llsum++;
            }
            if(c.response.get(i).second() == 1)
            {
                if(lzsum > 1)
                {
                    int removedPlayer = c.response.get(i).first();
                    c.response.remove(i);
                    ctx.clearNonHumanPlan(removedPlayer);
                    i--;
                    continue;
                }
                lzsum++;
            }
        }

        // 统计实际要上占/灵的数量
        int rezhan = 0, reling = 0;
        for(int i=0;i<c.response.size();i++)
        {
            if(c.response.get(i).second() == 1)
                rezhan++;
            else if(c.response.get(i).second() == 2)
                reling++;
        }

        // 裁剪占CO（最多4人）
        while(ctx.getZhans().size() + rezhan > 4 && !c.response.isEmpty())
        {
            c.response = GameLogicUtils.shuffleList(c.response);
            if(c.response.get(0).second() == 1 && c.response.get(0).first() != ctx.getActualRoleIndex(1))
            {
                ctx.clearNonHumanPlan(c.response.get(0).first());
                ctx.recordSkillTarget(c.response.get(0).first(), 1, 0);
                c.response.remove(0);
            }
        }

        // 裁剪灵CO（最多3人）
        while(ctx.getLings().size() + reling > 3 && !c.response.isEmpty())
        {
            c.response = GameLogicUtils.shuffleList(c.response);
            if(c.response.get(0).second() == 2 && c.response.get(0).first() != ctx.getActualRoleIndex(2))
            {
                ctx.clearNonHumanPlan(c.response.get(0).first());
                c.response.remove(0);
            }
        }

        // 处理剩余响应
        for(int i=0;i<c.response.size();i++)
        {
            int respPlayer = c.response.get(i).first();
            int role = c.response.get(i).second();
            boolean alreadyProcessed = false;
            if(role == 1 && ctx.getZhans().contains(respPlayer)) alreadyProcessed = true;
            if(role == 2 && ctx.getLings().contains(respPlayer)) alreadyProcessed = true;

            ctx.addLazySuspicionValue(respPlayer, GameConstants.SUSPICION_INCREASE_BLACK_RESPONSE);
            if(!alreadyProcessed)
                coManager.processActualCo(respPlayer, role, diebody);
            if(role == 5)
            {
                c.havecatco = true;
            }
        }
    }

    private void handleRemainingCOForUnclaimed(NormalScenarioContext c)
    {
        int n = c.n;
        for(int i=1;i<=n;i++)
        {
            if(ctx.isAlive(i) && ctx.getClaimedRole(i) == 0)
            {
                NonHumanPlan plan = ctx.getNonHumanPlan(i);
                if(ctx.getActualRole(i) == 1 || plan == NonHumanPlan.CLAIM_SEER) c.jhzhan++;
                else if(ctx.getActualRole(i) == 2 || plan == NonHumanPlan.CLAIM_MEDIUM) c.jhling++;
            }
        }
        while(ctx.getZhans().size() + c.jhzhan > 5 || ctx.getLings().size() + c.jhling > 4)
        {
            int target = ConstNum.randomInt(1, n);
            NonHumanPlan plan = ctx.getNonHumanPlan(target);
            if(ctx.getClaimedRole(target) == 0 && plan.isSeerOrMediumClaim())
            {
                if(ctx.getZhans().size() + c.jhzhan > 5 && plan == NonHumanPlan.CLAIM_SEER)
                {
                    c.jhzhan--;
                    ctx.clearNonHumanPlan(target);
                    ctx.recordSkillTarget(target, 1, 0);
                }
                else if(ctx.getLings().size() + c.jhling > 4 && plan == NonHumanPlan.CLAIM_MEDIUM)
                {
                    c.jhling--;
                    ctx.clearNonHumanPlan(target);
                }
            }
        }
    }

    private void executeWhiteBallCOConditional(NormalScenarioContext c, List<Integer> diebody)
    {
        if(ctx.getZhans().isEmpty() || c.response.size() > 0 || c.gqian || c.havecatco)
        {
            DebugLogger.log("非人的初始工作：白球占、灵能co");
            for(int i=1;i<=c.n;i++)
            {
                if(ctx.getClaimedRole(i) != 0 || ctx.isDead(i)) continue;
                if(ctx.getActualRole(i) < 3)
                {
                    coManager.processActualCo(i, ctx.getActualRole(i), diebody);
                }
                else if(ctx.getNonHumanPlan(i).isSeerOrMediumClaim())
                {
                    NonHumanPlan plan = ctx.getNonHumanPlan(i);
                    if(plan == NonHumanPlan.CLAIM_SEER)
                    {
                        ctx.recordSkillTarget(i, 1, ctx.getPlannedDivinationResult(i));
                        DebugLogger.log("添加占文：角色" + i + " 占文" + ctx.getSkillTarget(i, 1));
                    }
                    coManager.processActualCo(i, plan.claimedRole(), diebody);
                }
            }
            if((ctx.getGyIndex(1) <= 0 || ctx.getClaimedRole(ctx.getGyIndex(1)) != 4) &&
               (ctx.getGyIndex(2) <= 0 || ctx.getClaimedRole(ctx.getGyIndex(2)) != 4))
                gylogic.run();
        }
        c.response.clear();
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
