package com.vocr.core;

import com.vocr.domain.whyDie;

import com.vocr.config.GameConstants;
import com.vocr.rules.RuleEngine;

import java.util.ArrayList;
import java.util.List;

class WolfBiter
{
    private final GameContext ctx;
    private final SuspicionSystem suspicion;
    private final WolfBiteAnalyzer biteAnalyzer;

    public WolfBiter(GameContext ctx, SuspicionSystem suspicion)
    {
        this.ctx = ctx;
        this.suspicion = suspicion;
        this.biteAnalyzer = new WolfBiteAnalyzer(ctx);
    }

    public int[] decideBite()
    {
        int n = ctx.getPlayerSum();
        int gd = ctx.getGameDay();
        int[] biteWeight = new int[n + 1];

        WolfBiteAnalyzer.Analysis analysis = biteAnalyzer.analyze(n, gd);

        final double kFinal = analysis.hunterGuardFactor;
        final int hiddenHunterCntFinal = analysis.hiddenHunterTargetCount;
        final int zhenZhanCntFinal = analysis.plausibleSeerCount;
        final int zhenLieCntFinal = analysis.plausibleHunterCount;
        final int zhenLingCntFinal = analysis.plausibleMediumCount;
        final int yFinal = analysis.bittenSeerAccurateBonus;
        final int zFinal = analysis.bittenSeerNotAccurateBonus;
        final int aliverlFinal = analysis.aliveWolfCount;

        // ============================================================
        // Phase 2: 规则引擎 —— 所有权重修改
        // ============================================================
        RuleEngine engine = new RuleEngine();

        // --- 基础规则 ---
        engine.addSimpleRule("R01_排除非人/死者/标记",
                i -> ctx.getActualRole(i) == 7 || ctx.getDeathDay(i) != 0 || ctx.isNonHumanMarked(i),
                i -> -GameConstants.INF);

        engine.addSimpleRule("R02_基础活人权重",
                i -> ctx.getActualRole(i) != 7 && ctx.getDeathDay(i) == 0 && !ctx.isNonHumanMarked(i),
                i -> 20);

        // --- 角色修正 ---
        engine.addSimpleRule("R03_猎人加权",
                i -> i == ctx.getHunter(),
                i -> gd * 2);

        engine.addSimpleRule("R04_狂信减权",
                i -> ctx.usesFanatic() && i == ctx.getActualRoleIndex(9),
                i -> -45);

        // --- 末狼猫嫌 ---
        if (aliverlFinal == 1)
        {
            List<Integer> maoSnapshot = new ArrayList<>(ctx.getMaos());
            engine.addSimpleRule("R05_末狼猫嫌",
                    i -> maoSnapshot.contains(i),
                    i -> -50);
        }

        // --- 潜伏猎人护卫目标修正 ---
        if (hiddenHunterCntFinal > 0 && kFinal > 0)
        {
            engine.addSimpleRule("R06_潜伏猎护卫目标减权",
                    i -> ctx.isHiddenHunterScheduled(i, n),
                    i -> (int) (-200.0 / hiddenHunterCntFinal * kFinal));
        }

        // --- 无猎CO无职CO加权 ---
        if (zhenLieCntFinal == 0)
        {
            engine.addSimpleRule("R07_无猎CO无职CO加权",
                    i -> ctx.getClaimedRole(i) == 0,
                    i -> 10);
        }

        // --- 占CO/灵CO/共CO/猫CO角色修正 ---
        engine.addSimpleRule("R08_真占CO加权",
                i -> analysis.plausibleSeers[i],
                i -> 200 - zhenZhanCntFinal * 75 + 50 * yFinal - ctx.getZhans().size() * 20);

        engine.addSimpleRule("R09_真灵CO加权",
                i -> analysis.plausibleMediums[i],
                i -> 175 - zhenLingCntFinal * 100 - ctx.getLings().size() * 70);

        engine.addSimpleRule("R10_共有CO加权",
                i -> ctx.getActualRole(i) == 4 && ctx.getClaimedRole(i) == 4,
                i -> 75 + gd * 15);

        engine.addSimpleRule("R11_猫CO减权",
                i -> ctx.getClaimedRole(i) == 5,
                i -> -80);

        // --- 狼捕获修正 ---
        engine.addComplexRule("R12_狼捕获修正", weights -> {
            for (int i = 1; i <= n; i++)
            {
                int wolfCaught = 0;
                for (int j = 1; j <= 3; j++)
                    if (ctx.getTop3SuspectedPlayer(i, j, gd) != 0
                            && ctx.getActualRole(ctx.getTop3SuspectedPlayer(i, j, gd)) == 7)
                        wolfCaught++;
                if (wolfCaught == 1)      weights[i] += 15 + 20 * zFinal;
                else if (wolfCaught == 2) weights[i] += 30 + 20 * zFinal;
                else if (wolfCaught == 3) weights[i] += 50 + 20 * zFinal;
            }
        });

        // --- 占CO相关：白球/预告/黑球修正 ---
        engine.addComplexRule("R13_占CO白球与预告修正", weights -> {
            for (int i = 1; i <= n; i++)
            {
                if (ctx.getClaimedRole(i) != 1) continue;

                if (analysis.plausibleSeers[i])  // 真占
                {
                    for (int j = 1; j < gd; j++)
                        if (ctx.getDeathReason(j) == whyDie.NONE
                                && ctx.getSkillTarget(i, j) <= n
                                && ctx.getActualRole(i) != 7)
                            weights[ctx.getSkillTarget(i, j)] += 45 + 2 * analysis.whiteBallsBySeer[i] + 10 * yFinal;
                    for (int j = 1; j <= n; j++)
                        if (ctx.getDeathReason(j) == whyDie.NONE
                                && ctx.isClaimedRoleScheduled(i, j, n)
                                && ctx.getActualRole(j) != 7)
                            weights[j] += 15;
                }
                else if (ctx.getActualRole(i) == 7)  // 狼占
                {
                    for (int j = 1; j < gd; j++)
                    {
                        int t = ctx.getSkillTarget(i, j);
                        if (t < 1) continue;
                        if (t <= n && ctx.getDeathReason(t) == whyDie.NONE
                                && ctx.getActualRole(i) != 7)
                            weights[t] += 25 + 2 * analysis.whiteBallsBySeer[i];
                        if (t > n && ctx.getDeathReason(t - n) == whyDie.NONE)
                        {
                            if (ctx.getClaimedRole(t - n) == 6)
                                weights[t - n] -= 200;
                            else
                                weights[t - n] -= 100;
                        }
                    }
                    for (int j = 1; j <= n; j++)
                        if (ctx.getDeathReason(j) == whyDie.NONE
                                && ctx.isClaimedRoleScheduled(i, j, gd)
                                && ctx.getActualRole(j) != 7)
                            weights[j] += 15;
                }
                else  // 其他非人占
                {
                    for (int j = 1; j <= gd; j++)
                    {
                        int t = ctx.getSkillTarget(i, j);
                        if (t < 1 || ctx.getDeathReason(j) != whyDie.NONE) continue;
                        if (t <= n && ctx.getActualRole(i) != 7
                                && ctx.getDeathReason(t) == whyDie.NONE)
                            weights[t] += 15 + analysis.whiteBallsBySeer[i];
                        if (t > n && ctx.getDeathReason(t - n) == whyDie.NONE)
                        {
                            if (ctx.getClaimedRole(t - n) == 6)
                                weights[t - n] -= 50;
                            else
                                weights[t - n] -= 25;
                        }
                        if (ctx.getActualRole(j) == 7) weights[i] += 15;
                    }
                }
            }
        });

        // --- 猎CO修正 ---
        engine.addComplexRule("R14_猎CO护卫修正", weights -> {
            for (int i = 1; i <= n; i++)
            {
                if (!analysis.plausibleHunters[i]) continue;
                weights[i] += 350 - zhenLieCntFinal * 100;
                if (ctx.getClaimedRoleAskDay(3) > 0) weights[i] += 50;

                int huweicnt = 0;
                for (int j = 1; j <= n; j++)
                    if (ctx.isClaimedRoleScheduled(i, j, gd)) huweicnt++;
                if (huweicnt > 0)
                {
                    for (int j = 1; j <= n; j++)
                        if (ctx.isClaimedRoleScheduled(i, j, gd))
                            weights[j] -= 200 / huweicnt;
                }
            }
        });

        // --- 白球全局修正 ---
        engine.addComplexRule("R15_白球全局修正", weights -> {
            for (int i = 1; i <= n; i++)
                weights[i] += analysis.whiteBallsReceived[i] * 5
                        + analysis.whiteBallsReceived[i] * analysis.whiteBallsReceived[i] * 2;
        });

        // ============================================================
        // Phase 3: 应用所有规则
        // ============================================================
        engine.apply(biteWeight);

        // ============================================================
        // Phase 4: 选择主咬狼和咬杀目标
        // ============================================================
        int[] wolfbite = new int[n + 1];
        for (int i = 1; i <= n; i++)
        {
            if (ctx.getActualRole(i) != 7 || ctx.getDeathReason(i) != whyDie.NONE)
                wolfbite[i] = -GameConstants.INF;
            else if (ctx.isNonHumanMarked(i))
                wolfbite[i] = GameConstants.INF;
            else
                wolfbite[i] = 1;
        }
        int bitewolf = suspicion.getOne(wolfbite);
        int biteone = suspicion.getOne(biteWeight);
        return new int[] { bitewolf, biteone };
    }

}
