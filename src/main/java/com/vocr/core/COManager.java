package com.vocr.core;

import com.vocr.domain.CharacterEnglishName;
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
    private final ProbabilityCalculator probabilityCalculator;
    private final ResultEventGenerator eventGenerator;
    private final HunterGuarder hunterGuarder;

    public COManager(GameContext ctx, SuspicionSystem suspicion,
                     ResultPresenter resultPresenter, ProbabilityCalculator probabilityCalculator,
                     HunterGuarder hunterGuarder)
    {
        this.ctx = ctx;
        this.suspicion = suspicion;
        this.resultPresenter = resultPresenter;
        this.probabilityCalculator = probabilityCalculator;
        this.eventGenerator = new ResultEventGenerator(ctx);
        this.hunterGuarder = hunterGuarder;
    }

    /** 猫又CO概率参数p1：狼队中已CO且非默认CO（claimedRole=6）的人数 */
    public int getp1(int zhi)
    {
        if (zhi != 7) return 0;
        int p1 = 0;
        for (int i = 1; i <= ctx.getInitialWolfCount(); i++)
            if (ctx.getClaimedRole(ctx.getRlIndex(i)) != 0 && ctx.getClaimedRole(ctx.getRlIndex(i)) != 6)
                p1++;
        return p1;
    }

    /** 猫又CO概率参数p2：case 7/9返回狼队生存人数-1，其余返回死亡计数器映射值 */
    public int getp2(int zhi)
    {
        if (zhi == 7 || zhi == 9)
        {
            int p2 = 0;
            for (int i = 1; i <= ctx.getInitialWolfCount(); i++)
                if (ctx.isAlive(ctx.getRlIndex(i)))
                    p2++;
            return p2 - 1;
        }
        return deathCounterP2();
    }

    private int deathCounterP2()
    {
        if (ctx.getDeathCounter() <= 3) return 0;
        if (ctx.getDeathCounter() <= ctx.getAliveCounter()) return 1;
        if (ctx.getAliveCounter() > 6) return 2;
        return 3;
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
                    suspicion.updateTop3Aux2(num, ctx.getZhans().get(i), GameConstants.INF, GameConstants.INF);
                }
                if (gd == 3 && ctx.getActualRole(num) > 6 && ctx.isBlackResult(ctx.getSkillTarget(num, 1))
                        && ctx.getDiePlayerNum(whyDie.beiyao, 2) == ctx.getSkillTarget(num, 1) - ctx.getPlayerSum())
                {
                    if (ctx.zw[num] == ctx.getSkillTarget(num, 1) && ctx.getYBZW(num) > 0)
                        ctx.setSkillTarget(num, 1, ctx.getYBZW(num));
                    else if (ctx.getYBZW(num) == ctx.getSkillTarget(num, 1) && ctx.zw[num] > 0)
                        ctx.setSkillTarget(num, 1, ctx.zw[num]);
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
                                suspicion.updateTop3Aux2(num, ctx.getLings().get(i), GameConstants.INF, GameConstants.INF);
                                ctx.lined[num][ctx.getLings().get(i)] = 0;
                            }
                            else if (ctx.getSuspicionValue(num, ctx.getLings().get(i)) <= GameConstants.INFJ
                                    && ctx.getSkillTarget(ctx.getLings().get(i), k) == ctx.getSkillTarget(num, j)
                                    && ctx.isBlackResult(ctx.getSkillTarget(num, j)))
                            {
                                suspicion.updateTop3Aux2(num, ctx.getLings().get(i), -10, -10);
                                ctx.lined[num][ctx.getLings().get(i)] = 1;
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
                    suspicion.updateTop3Aux2(num, target, GameConstants.INF, GameConstants.INF);
                    eventGenerator.addEvent(EventName.zjgh8b, num, target);
                    DebugLogger.log("添加事件占结果黑，占卜师编号：" + num + "黑球姓名:"
                            + ctx.getCharacterName(target).toString());
                    resultPresenter.presentBlackResult(num, target, true);
                }
                else if (target > 0)
                {
                    ctx.addLazySuspicionValue(target, GameConstants.SUSPICION_DECREASE_WHITE_BALL_TARGET);
                    suspicion.updateTop3Aux2(num, target, -5, -10);
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
                    suspicion.updateTop3Aux2(num, ctx.getLings().get(i), GameConstants.INF, GameConstants.INF);
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
                    suspicion.updateTop3Aux2(ctx.getLies().get(i), num, GameConstants.INF, GameConstants.INF);
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
                            ctx.setSkillTarget(num, j, target);
                        }
                    }
                }
                break;
            case 4:
            {
                ctx.registerClaim(num, 4);
                int gy = 1;
                if (ctx.gyindex[2] == num) gy = 2;
                ctx.addLazySuspicionValue(num, -GameConstants.INF);
                eventGenerator.addEvent(EventName.qfjc5, num, ctx.gyindex[3 - gy]);
                eventGenerator.addEvent(EventName.qfjcqr5r, ctx.gyindex[3 - gy], num);
                if (ctx.getClaimedRole(ctx.gyindex[3 - gy]) != zhi)
                    ctx.revealClaim(ctx.gyindex[3 - gy], zhi, gd);
                break;
            }
            case 5:
                for (int i = 0; i < ctx.getMaos().size(); i++)
                    suspicion.updateTop3Aux2(ctx.getMaos().get(i), num, GameConstants.INF, GameConstants.INF);
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
        int n = ctx.getPlayerSum();
        int gd = ctx.getGameDay();
        DebugLogger.log("询问职业：" + aclaimedRole);
        int zhi = aclaimedRole.ordinal();
        if (!ctx.tryRecordClaimRequest(zhi, gd))
            return;
        if (zhi > 3 && zhi < 6)
        {
            if (ctx.getPeiyi() == peiyi.jianyi) return;
            if (zhi == 5 && ctx.getPeiyi() != peiyi.maoyou && ctx.getPeiyi() != peiyi.daxing) return;
        }
        if (zhi == 5)
        {
            int counter = 0;
            for (int i = 1; i <= gd; i++)
            {
                if (ctx.isDoubleDeathOccurred(i))
                    counter++;
            }
            if (counter == 2 && ctx.getDeathReason(ctx.actualRoleindex[5]) != whyDie.NONE)
                return;
            if (ctx.isDoubleDeathOccurred(gd - 1) && ctx.getDeathReason(ctx.actualRoleindex[5]) != whyDie.NONE)
            {
                boolean canclaim = false;
                if (gd == 3 && ctx.getClaimedRoleAskDay(1) != 2)
                {
                    boolean have_yugao = false;
                    for (int i = 1; i <= n; i++)
                    {
                        if (ctx.getHiddenSeerScheduledSkillTargets()[i][gd - 1])
                        {
                            have_yugao = true;
                            if (ctx.getDiebody().contains(i))
                                canclaim = true;
                        }
                    }
                }
                for (int i = 0; i < ctx.getZhans().size(); i++)
                {
                    int zhan = ctx.getZhans().get(i);
                    if (ctx.isNonHumanMarked(zhan)) continue;
                    if (ctx.isAlive(zhan))
                    {
                        if (ctx.getDiebody().contains(ctx.getSkillTarget(zhan, gd - 1)))
                            canclaim = true;
                    }
                    else
                    {
                        boolean have_yugao = false;
                        for (int j = 1; j <= n; j++)
                        {
                            if (ctx.getClaimedRoleScheduledSkillTargets(zhan)[j][gd - 1])
                            {
                                have_yugao = true;
                                if (ctx.getDiebody().contains(j))
                                    canclaim = true;
                            }
                        }
                        if (!have_yugao)
                            canclaim = true;
                    }
                }
                if (!canclaim) return;
            }
        }
        ArrayList<IntPair> response = new ArrayList<>();
        for (int i = 1; i <= n; i++)
        {
            if (ctx.isDead(i) || ctx.getClaimedRole(i) != 0) continue;
            if (ctx.getActualRole(i) < 7)
            {
                if (ctx.getActualRole(i) == zhi)
                    response.add(new IntPair(i, zhi));
                continue;
            }
            if (ctx.nonHumanPlan[i] == zhi && ctx.nonHumanPlan[i] != 4)
            {
                if (gd == 2)
                    ctx.setSkillTarget(i, 1, ctx.zw[i]);
                response.add(new IntPair(i, zhi));
                continue;
            }
            if (zhi < 3 || zhi == 4 || ctx.nonHumanPlan[i] != 0) continue;
            switch (ctx.getActualRole(i))
            {
                case 7:
                    if (zhi == 3 && ctx.isWolfClaimedHunter()) break;
                    if (zhi == 5 && ctx.isWolfClaimedCat()) break;
                    int probability = probabilityCalculator.maolieco(0, 3, getp1(7), getp2(7));
                    if (GameLogicUtils.probabilityJudge(probability))
                    {
                        response.add(new IntPair(i, zhi));
                        ctx.markWolfClaimedRole(zhi);
                    }
                    break;
                default:
                    if (GameLogicUtils.probabilityJudge(probabilityCalculator.maolieco(
                            ctx.getActualRole(i) - 7, 3, getp1(ctx.getActualRole(i)), getp2(ctx.getActualRole(i)))))
                    {
                        response.add(new IntPair(i, zhi));
                    }
                    break;
            }
        }
        response = GameLogicUtils.shuffleList(response);
        if (aclaimedRole == Role.gong && response.size() > 1)
            response.remove(1);
        for (int i = 0; i < response.size(); i++)
        {
            processActualCo(response.get(i).first(), response.get(i).second(), ctx.getDiebody());
        }
    }

    public void askCoByList(List<Integer> askList)
    {
        ArrayList<IntPair> response = new ArrayList<>();
        int probability;
        for (int i = 0; i < askList.size(); i++)
        {
            int num = askList.get(i);
            if (ctx.getClaimedRole(num) != 0) continue;
            if (ctx.getActualRole(num) < 7)
            {
                response.add(new IntPair(num, ctx.getActualRole(num)));
            }
            else if (ctx.nonHumanPlan[num] != 0 && ctx.nonHumanPlan[num] != 4)
            {
                if (ctx.nonHumanPlan[num] == 1)
                    ctx.setSkillTarget(num, 1, ctx.zw[num]);
                response.add(new IntPair(num, ctx.nonHumanPlan[num]));
            }
            else
            {
                int option = 0;
                switch (ctx.getActualRole(num))
                {
                    case 7:
                        if (ctx.isWolfClaimedHunter() && ctx.isWolfClaimedCat())
                        {
                            response.add(new IntPair(num, 6));
                            break;
                        }
                        probability = probabilityCalculator.maolieco(0, 2, getp1(7), getp2(7));
                        if (!GameLogicUtils.probabilityJudge(probability))
                        {
                            response.add(new IntPair(num, 6));
                            break;
                        }
                        if (ctx.isWolfClaimedHunter())
                        {
                            // 如果猫又已明确出局，不能CO猫
                            if (ctx.actualRoleindex[5] != 0 && !ctx.isCatDefinitivelyOut())
                                response.add(new IntPair(num, 5));
                            else
                                response.add(new IntPair(num, 6));
                            ctx.markWolfClaimedRole(5);
                            break;
                        }
                        if (ctx.isWolfClaimedCat())
                        {
                            response.add(new IntPair(num, 3));
                            ctx.markWolfClaimedRole(3);
                            break;
                        }
                        option = 0;
                        // 如果猫又已明确出局，不能CO猫
                        if (ctx.actualRoleindex[5] != 0 && !ctx.isCatDefinitivelyOut())
                            option = GameLogicUtils.getEventIndexByProbability(
                                    new ArrayList<>(List.of(50 + 50 * ctx.getMaos().size(),
                                            50 + 50 * ctx.getLies().size())));
                        response.add(new IntPair(num, 3 + 2 * option));
                        break;
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                        probability = probabilityCalculator.maolieco(ctx.getActualRole(num) - 7, 2,
                                getp1(ctx.getActualRole(num)), getp2(ctx.getActualRole(num)));
                        if (!GameLogicUtils.probabilityJudge(probability))
                        {
                            response.add(new IntPair(num, 6));
                            break;
                        }
                        option = 0;
                        // 如果猫又已明确出局，不能CO猫
                        if (ctx.actualRoleindex[5] != 0 && !ctx.isCatDefinitivelyOut())
                            option = GameLogicUtils.getEventIndexByProbability(
                                    new ArrayList<>(List.of(50 + 50 * ctx.getMaos().size(),
                                            50 + 50 * ctx.getLies().size())));
                        response.add(new IntPair(num, 3 + 2 * option));
                        break;
                }
            }
        }
        for (int i = 0; i < response.size(); i++)
        {
            processActualCo(response.get(i).first(), response.get(i).second(), ctx.getDiebody());
        }
    }

    public void processMaoLieCo(int num, boolean isHumanWolf, List<Integer> diebody)
    {
        if (isHumanWolf)
        {
            if ((ctx.actualRoleindex[3] < 1 || ctx.isWolfClaimedHunter() || ctx.getClaimedRoleAskDay(3) > 0)
                    && (ctx.actualRoleindex[5] < 1 || ctx.isWolfClaimedCat() || ctx.getClaimedRoleAskDay(5) > 0))
                return;
        }
        // 如果猫又已明确出局，不能CO猫
        if (ctx.actualRoleindex[3] < 1 || (isHumanWolf && ctx.isWolfClaimedHunter()) || ctx.getClaimedRoleAskDay(3) > 0
                || ctx.isCatDefinitivelyOut())
            processActualCo(num, 3, diebody);
        else if (ctx.actualRoleindex[5] < 1 || (isHumanWolf && ctx.isWolfClaimedCat()) || ctx.getClaimedRoleAskDay(5) > 0)
            processActualCo(num, 3, diebody);
        else
            processActualCo(num, 3 + 2 * GameLogicUtils.getEventIndexByProbability(
                    new ArrayList<>(List.of(50 + 50 * ctx.getLies().size(), 50 + 50 * ctx.getMaos().size()))), diebody);
    }
}
