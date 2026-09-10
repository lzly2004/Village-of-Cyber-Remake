package com.vocr.core;

import com.vocr.domain.EventName;
import com.vocr.domain.IntPair;
import com.vocr.domain.peiyi;
import com.vocr.domain.Role;
import com.vocr.domain.whyDie;

import com.vocr.config.GameConstants;
import com.vocr.config.ProbabilityCalculator;

import java.util.ArrayList;
import java.util.List;

public class COManager
{
    private final GameContext ctx;
    private final SuspicionSystem suspicion;
    private final ResultPresenter resultPresenter;
    private final ClaimResponsePlanner responsePlanner;
    private final ResultEventGenerator eventGenerator;
    private final HunterGuarder hunterGuarder;

    public COManager(GameContext ctx, SuspicionSystem suspicion,
                     ResultPresenter resultPresenter, ProbabilityCalculator probabilityCalculator,
                     HunterGuarder hunterGuarder)
    {
        this.ctx = ctx;
        this.suspicion = suspicion;
        this.resultPresenter = resultPresenter;
        this.responsePlanner = new ClaimResponsePlanner(ctx, probabilityCalculator);
        this.eventGenerator = new ResultEventGenerator(ctx);
        this.hunterGuarder = hunterGuarder;
    }

    /** 猫又CO概率参数p1：狼队中已CO且非默认CO（claimedRole=6）的人数 */
    public int getp1(int zhi)
    {
        return responsePlanner.getP1(zhi);
    }

    /** 猫又CO概率参数p2：case 7/9返回狼队生存人数-1，其余返回死亡计数器映射值 */
    public int getp2(int zhi)
    {
        return responsePlanner.getP2(zhi);
    }

    public void processActualCo(int num, int zhi, List<Integer> diebody)
    {
        int gd = ctx.getGameDay();
        if (ctx.getDeathReason(num) != whyDie.NONE) return;

        DebugLogger.log("进入役职co:num,zhi:" + num + "," + zhi + ",初日技能情况：" + ctx.getSkillTarget(num, 1));
        if (ctx.getPeiyi() != peiyi.daxing && ctx.getPeiyi() != peiyi.maoyou && zhi == 5) return;
        switch (zhi)
        {
            case 1:
            {
                for (int i = 0; i < ctx.getZhans().size(); i++)
                {
                    suspicion.adjustMutualSuspicion(num, ctx.getZhans().get(i), GameConstants.INF, GameConstants.INF);
                }
                if (gd == 3 && ctx.getActualRole(num) > 6 && ctx.isBlackResult(ctx.getSkillTarget(num, 1))
                        && ctx.getDiePlayerNum(whyDie.beiyao, 2) == ctx.getSkillTarget(num, 1) - ctx.getPlayerSum())
                {
                    if (ctx.getPlannedDivinationResult(num) == ctx.getSkillTarget(num, 1)
                            && ctx.getPreliminaryDivinationTarget(num) > 0)
                        ctx.recordSkillTarget(num, 1, ctx.getPreliminaryDivinationTarget(num));
                    else if (ctx.getPreliminaryDivinationTarget(num) == ctx.getSkillTarget(num, 1)
                            && ctx.getPlannedDivinationResult(num) > 0)
                        ctx.recordSkillTarget(num, 1, ctx.getPlannedDivinationResult(num));
                }
                for (int i = 0; i < ctx.getLings().size(); i++)
                {
                    for (int j = 1; j <= gd; j++)
                        for (int k = 2; k <= gd; k++)
                        {
                            if (ctx.getSuspicionValue(num, ctx.getLings().get(i)) <= GameConstants.INFJ
                                    && Math.abs(ctx.getSkillTarget(ctx.getLings().get(i), k)
                                    - ctx.getSkillTarget(num, j)) == gd)
                            {
                                ctx.addLazySuspicionValue(num, GameConstants.SUSPICION_INCREASE_CONFLICT);
                                ctx.addLazySuspicionValue(ctx.getLings().get(i), GameConstants.SUSPICION_INCREASE_CONFLICT);
                                suspicion.adjustMutualSuspicion(num, ctx.getLings().get(i), GameConstants.INF, GameConstants.INF);
                                ctx.clearSeerMediumLink(num, ctx.getLings().get(i));
                            }
                            else if (ctx.getSuspicionValue(num, ctx.getLings().get(i)) <= GameConstants.INFJ
                                    && ctx.getSkillTarget(ctx.getLings().get(i), k) == ctx.getSkillTarget(num, j)
                                    && ctx.isBlackResult(ctx.getSkillTarget(num, j)))
                            {
                                suspicion.adjustMutualSuspicion(num, ctx.getLings().get(i), -10, -10);
                                ctx.establishSeerMediumLink(num, ctx.getLings().get(i));
                            }
                        }
                }
                ctx.registerClaim(num, 1);
                ctx.addLazySuspicionValue(num, GameConstants.SUSPICION_DECREASE_SEER_CO);
                if (gd > 2)
                    ctx.addLazySuspicionValue(num, GameConstants.SUSPICION_INCREASE_LATE_CO);
                int target = ctx.getSkillTarget(num, 1);
                if (ctx.isBlackResult(target))
                {
                    target -= ctx.getPlayerSum();
                    ctx.addLazySuspicionValue(target, GameConstants.SUSPICION_INCREASE_BLACK_BALL_TARGET);
                    ctx.addLazySuspicionValue(num, GameConstants.SUSPICION_INCREASE_BLACK_BALL_CASTER);
                    suspicion.adjustMutualSuspicion(num, target, GameConstants.INF, GameConstants.INF);
                    eventGenerator.addEvent(EventName.zjgh8b, num, target);
                    DebugLogger.log("添加事件占结果黑，占卜师编号：" + num + "黑球姓名:"
                            + ctx.getCharacterName(target).toString());
                    resultPresenter.presentBlackResult(num, target, true);
                }
                else if (target > 0)
                {
                    ctx.addLazySuspicionValue(target, GameConstants.SUSPICION_DECREASE_WHITE_BALL_TARGET);
                    suspicion.adjustMutualSuspicion(num, target, -5, -10);
                    eventGenerator.addEvent(EventName.zjgb8, num, target);
                    if (ctx.getDeathReason(target) == whyDie.NONE)
                        eventGenerator.addEvent(EventName.jbdh8r, target, num);
                }
                if (gd == 3)
                {
                    ctx.addLazySuspicionValue(num, GameConstants.SUSPICION_INCREASE_LATE_CO);
                    resultPresenter.presentZhan(num, diebody);
                }
                break;
            }
            case 2:
            {
                for (int i = 0; i < ctx.getLings().size(); i++)
                {
                    suspicion.adjustMutualSuspicion(num, ctx.getLings().get(i), GameConstants.INF, GameConstants.INF);
                }
                ctx.registerClaim(num, 2);
                ctx.addLazySuspicionValue(num, GameConstants.SUSPICION_DECREASE_MEDIUM_CO);
                if (gd == 2)
                {
                    eventGenerator.addEvent(EventName.lnco18, num);
                    break;
                }
                else
                {
                    ctx.addLazySuspicionValue(num, GameConstants.SUSPICION_INCREASE_LATE_CO);
                    resultPresenter.presentLing(num);
                }
                break;
            }
            case 3:
                for (int i = 0; i < ctx.getLies().size(); i++)
                    suspicion.adjustMutualSuspicion(ctx.getLies().get(i), num, GameConstants.INF, GameConstants.INF);
                ctx.registerClaim(num, 3);
                ctx.addLazySuspicionValue(num, GameConstants.SUSPICION_DECREASE_MEDIUM_CO);
                eventGenerator.addEvent(EventName.lrco, num);
                if (ctx.getActualRole(num) != 3)
                {
                    for (int j = 2; j < gd; j++)
                    {
                        if (ctx.getSkillTarget(num, j) == 0)
                        {
                            int target = hunterGuarder.guard(num);
                            ctx.recordSkillTarget(num, j, target);
                        }
                    }
                }
                break;
            case 4:
            {
                ctx.registerClaim(num, 4);
                int gy = 1;
                if (ctx.getGyIndex(2) == num) gy = 2;
                ctx.addLazySuspicionValue(num, -GameConstants.INF);
                eventGenerator.addEvent(EventName.qfjc5, num, ctx.getGyIndex(3 - gy));
                eventGenerator.addEvent(EventName.qfjcqr5r, ctx.getGyIndex(3 - gy), num);
                if (ctx.getClaimedRole(ctx.getGyIndex(3 - gy)) != zhi)
                    ctx.revealClaim(ctx.getGyIndex(3 - gy), zhi, gd);
                break;
            }
            case 5:
                for (int i = 0; i < ctx.getMaos().size(); i++)
                    suspicion.adjustMutualSuspicion(ctx.getMaos().get(i), num, GameConstants.INF, GameConstants.INF);
                ctx.registerClaim(num, 5);
                ctx.addLazySuspicionValue(num, GameConstants.SUSPICION_DECREASE_MEDIUM_CO);
                eventGenerator.addEvent(EventName.mco, num);
                break;
            case 6:
                ctx.registerClaim(num, 6);
                break;
        }
        DebugLogger.log("退出役职co");
    }

    public void askCoByRole(Role aclaimedRole)
    {
        applyResponses(responsePlanner.planResponsesForRole(aclaimedRole));
    }

    public void askCoByList(List<Integer> askList)
    {
        applyResponses(responsePlanner.planResponsesForPlayers(askList));
    }

    private void applyResponses(List<IntPair> responses)
    {
        for (IntPair response : responses)
            processActualCo(response.first(), response.second(), ctx.getDiebody());
    }

    public void processMaoLieCo(int num, boolean isHumanWolf, List<Integer> diebody)
    {
        if (isHumanWolf)
        {
            if ((ctx.getActualRoleIndex(3) < 1 || ctx.isWolfClaimedHunter() || ctx.getClaimedRoleAskDay(3) > 0)
                    && (ctx.getActualRoleIndex(5) < 1 || ctx.isWolfClaimedCat() || ctx.getClaimedRoleAskDay(5) > 0))
                return;
        }
        // 如果猫又已明确出局，不能CO猫
        if (ctx.getActualRoleIndex(3) < 1 || (isHumanWolf && ctx.isWolfClaimedHunter()) || ctx.getClaimedRoleAskDay(3) > 0
                || ctx.isCatDefinitivelyOut())
            processActualCo(num, 3, diebody);
        else if (ctx.getActualRoleIndex(5) < 1 || (isHumanWolf && ctx.isWolfClaimedCat()) || ctx.getClaimedRoleAskDay(5) > 0)
            processActualCo(num, 3, diebody);
        else
            processActualCo(num, 3 + 2 * GameLogicUtils.getEventIndexByProbability(
                    new ArrayList<>(List.of(50 + 50 * ctx.getLies().size(), 50 + 50 * ctx.getMaos().size()))), diebody);
    }
}
