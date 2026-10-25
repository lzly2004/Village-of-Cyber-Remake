package com.vocr.core;

import com.vocr.logging.DebugLogger;

import com.vocr.domain.CharacterKanjiName;
import com.vocr.domain.IntPair;
import com.vocr.domain.peiyi;
import com.vocr.domain.Role;

import com.vocr.config.ProbabilityCalculator;

import java.util.ArrayList;
import java.util.List;

class DayActionCoordinator
{
    private final GameModule module;
    private final GameContext ctx;
    private final SuspicionSystem suspicion;
    private final COManager coManager;
    private final ResultPresenter resultPresenter;
    private final ProbabilityCalculator probabilityCalculator;
    private final Runnable gylogic;
    private final Runnable deliverEvents;

    public DayActionCoordinator(GameModule module)
    {
        this.module = module;
        this.ctx = module.getCtx();
        this.suspicion = module.getSuspicion();
        this.coManager = module.getCoManager();
        this.resultPresenter = module.getResultPresenter();
        this.probabilityCalculator = module.getProbabilityCalculator();
        this.gylogic = module.getGylogic();
        this.deliverEvents = module.getDeliverEvents();
    }

    public void coordinate(List<Integer> diebody)
    {
        logDayStart();

        presentDivinationResults(diebody);

        handleLatentSeerMediumCO(diebody);

        handleCatHunterCO(diebody);

        gylogic.run();
        deliverEvents.run();
    }

    // ==================== 子方法 ====================

    private void logDayStart()
    {
        int n = ctx.getPlayerSum();
        DebugLogger.log("现在是白天。当前存活人数" + ctx.getAliveCounter() + ",存活名单：\n");
        String alivemans = "";
        for (int i = 1; i <= n; i++)
            if (ctx.isAlive(i))
                alivemans += CharacterKanjiName.values()[ctx.getCharacterNumber(i)];
        DebugLogger.log(alivemans);
        DebugLogger.log("\n");
    }

    private void presentDivinationResults(List<Integer> diebody)
    {
        for (int zhan : ctx.getZhans()) resultPresenter.presentZhan(zhan, diebody);
        for (int ling : ctx.getLings()) resultPresenter.presentLing(ling);
    }

    private void handleLatentSeerMediumCO(List<Integer> diebody)
    {
        int n = ctx.getPlayerSum();
        int gd = ctx.getGameDay();
        if(gd == 3)
            for(int i=1;i<=n;i++)
            {
                if(ctx.getClaimedRole(i) != 0) continue;
                if(ctx.getActualRole(i) == 2 || ctx.getNonHumanPlan(i) == NonHumanPlan.CLAIM_MEDIUM)
                {
                    coManager.processActualCo(i,2,diebody);
                    continue;
                }
                if(ctx.getActualRole(i) == 1 || ctx.getNonHumanPlan(i) == NonHumanPlan.CLAIM_SEER)
                {
                    coManager.processActualCo(i,1,diebody);
                }
                ctx.tryRecordClaimRequest(1, 3);
                ctx.tryRecordClaimRequest(2, 3);
            }
    }

    private void handleCatHunterCO(List<Integer> diebody)
    {
        if(ctx.getPeiyi() != peiyi.jianyi && ctx.getGyIndex(1) > 0 && ctx.isDead(ctx.getGyIndex(1)) && ctx.isDead(ctx.getGyIndex(2)))
            coManager.askCoByRole(Role.mao);

        if(diebody.size() == 2)
            coManager.askCoByRole(Role.mao);

        ArrayList<IntPair> response = new ArrayList<>();
        boolean havecatco = handleBlackResponses(response);

        if(havecatco)
            coManager.askCoByRole(Role.mao);
    }

    private boolean handleBlackResponses(ArrayList<IntPair> response)
    {
        int n = ctx.getPlayerSum();
        int gd = ctx.getGameDay();
        boolean havecatco = false;

        if(ctx.getClaimedRoleAskDay(3) == 0 || (ctx.getCat() != 0 && ctx.getClaimedRoleAskDay(5) == 0))
        {
            for(int i=0;i<ctx.getZhans().size();i++)
            {
                int zhan = ctx.getZhans().get(i);
                if(ctx.isBlackResult(ctx.getSkillTarget(zhan, gd - 1)))
                {
                    int target = ctx.getSkillTarget(zhan, gd - 1) - n;
                    if(ctx.isDead(target) || ctx.getClaimedRole(target) != 0)
                        continue;
                    if(ctx.getActualRole(target) == 5
                            || ctx.getNonHumanPlan(target) == NonHumanPlan.CLAIM_CAT)
                    {
                        // 如果猫又已明确出局，非人不能CO猫
                        if(ctx.getCat() == 0 || ctx.isCatDefinitivelyOut())
                            ctx.clearNonHumanPlan(target);
                        else
                        {
                            response.add(new IntPair(target,5));havecatco = true;continue;
                        }
                    }
                    NonHumanPlan plan = ctx.getNonHumanPlan(target);
                    if(ctx.getActualRole(target) < 7 || plan == NonHumanPlan.NONE
                            || plan == NonHumanPlan.CLAIM_HUNTER) continue;
                    int zhi = ctx.getActualRole(target);
                    int maolieIdx = zhi == 7 ? 0 : zhi - 7;
                    if(ctx.getRandom().probabilityJudge(probabilityCalculator.maolieco(maolieIdx,1,coManager.getp1(zhi),coManager.getp2(zhi))))
                    {
                        int lweight = 50+50*ctx.getMaos().size(),mweight = 50 + 50*ctx.getLies().size();
                        if((zhi == 7 && ctx.isWolfClaimedHunter()) || ctx.getClaimedRoleAskDay(3) != 0) lweight = 0;
                        if((zhi == 7 && ctx.isWolfClaimedCat()) || ctx.getCat() == 0 || ctx.isCatDefinitivelyOut() || ctx.getClaimedRoleAskDay(5) != 0) mweight = 0;
                        if(lweight + mweight == 0) ctx.clearNonHumanPlan(target);
                        else
                        {
                            int role = 3 + 2 * ctx.getRandom().weightedIndex(
                                    new ArrayList<>(List.of(lweight,mweight)));
                            NonHumanPlan responsePlan = NonHumanPlan.claimRole(role);
                            ctx.setNonHumanPlan(target, responsePlan);
                            if(responsePlan == NonHumanPlan.CLAIM_CAT)
                            {
                                response.add(new IntPair(target,5));havecatco = true;
                                if(zhi == 7) ctx.markWolfClaimedRole(5);
                            }
                            else if(zhi == 7) ctx.markWolfClaimedRole(3);
                        }
                    }
                    else if(zhi == 7)
                        ctx.clearNonHumanPlan(target);
                }
            }
        }
        return havecatco;
    }
}
