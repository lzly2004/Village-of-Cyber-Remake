import java.util.ArrayList;
import java.util.List;

public class WolfBiter
{
    private final GameContext ctx;
    private final SuspicionSystem suspicion;

    public WolfBiter(GameContext ctx, SuspicionSystem suspicion)
    {
        this.ctx = ctx;
        this.suspicion = suspicion;
    }

    public int[] decideBite()
    {
        int n = ctx.getPlayerSum();
        int gd = ctx.getGameDay();
        int[] biteWeight = new int[n + 1];

        // ============================================================
        // Phase 1: 派生值计算（非规则，纯分析）
        // ============================================================

        // 1a. 末狼判定
        int aliverl = 0;
        for (int i = 1; i <= ctx.initialWolfCount; i++)
            if (ctx.getDeathReason(ctx.rlindex[i]) == whyDie.NONE) aliverl++;

        // 1b. 初日占被咬判定 (y=1表示初日被咬占卜师是"真占"或"咬中非狼")
        int y = 0, z = 0;
        int zzhb = ctx.getDiePlayerNum(whyDie.beiyao, 2);
        if (zzhb != -1 && ctx.getClaimedRole(zzhb) == 1)
        {
            if (ctx.getSkillTarget(zzhb, 1) > n)
            {
                if (ctx.getActualRole(ctx.getSkillTarget(zzhb, 1) - n) == 7) y = 1;
            }
            else
            {
                if (ctx.getActualRole(ctx.getSkillTarget(zzhb, 1)) != 7) y = 1;
            }
        }
        if (y == 0) z = 1;

        // 1c. 真占/真灵/真猎识别
        int zhenZhanCnt = 0, zhenLieCnt = 0, zhenLingCnt = 0;
        boolean[] isZhan = new boolean[n + 1];
        boolean[] isLie = new boolean[n + 1];
        boolean[] isLing = new boolean[n + 1];
        int[] baiqiu = new int[n + 1];

        for (int i = 1; i <= n; i++)
        {
            if (ctx.getClaimedRole(i) == 2 && ctx.getActualRole(i) != 7 && !ctx.isNonHumanMarked(i))
            {
                boolean ok = true;
                for (int j = 1; j <= gd; j++)
                {
                    if (ctx.getSkillTarget(i, j) > n)
                    { if (ctx.getActualRole(i) != 7) { ok = false; break; } }
                    else
                    { if (ctx.getActualRole(i) == 7) { ok = false; break; } }
                }
                if (ok) { isLing[i] = true; zhenLingCnt++; }
            }
            if (ctx.getDeathDay(i) != 0) continue;
            if (ctx.getClaimedRole(i) == 3 && ctx.getActualRole(i) != 7 && !ctx.isNonHumanMarked(i))
            { isLie[i] = true; zhenLieCnt++; }
            if (ctx.getClaimedRole(i) == 1 && ctx.getActualRole(i) != 7 && !ctx.isNonHumanMarked(i))
            {
                boolean ok = true;
                for (int j = 1; j <= gd; j++)
                {
                    if (ctx.getSkillTarget(i, j) > n)
                    { if (ctx.getActualRole(i) != 7) { ok = false; break; } }
                    else
                    { if (ctx.getActualRole(i) == 7) { ok = false; break; } }
                }
                if (ok) { isZhan[i] = true; zhenZhanCnt++; }
            }
        }

        // 1d. 白球计数
        for (int i = 1; i <= n; i++)
        {
            if (ctx.getClaimedRole(i) != 1) continue;
            for (int j = 1; j < gd; j++)
            {
                int t = ctx.getSkillTarget(i, j);
                if (t <= n && t > 0 && !ctx.isNonHumanMarked(t)
                        && ctx.getClaimedRole(t) != 1
                        && !suspicion.isAckWhite(t, ctx.maos, ctx.isDoubleDeathOccurred, ctx.claimedRoleaskday))
                    baiqiu[i]++;
            }
        }

        // 1e. 猎人护卫系数 k
        double k;
        switch (zhenLieCnt)
        {
            case 0: k = 0.8; break;
            case 1: k = 0.5; break;
            case 2: k = 0.3; break;
            case 3: k = 0.1; break;
            default: k = 0; break;
        }
        int wuco = 0, alivewuco = 0;
        for (int i = 1; i <= n; i++)
        {
            if (ctx.getActualRole(i) == 7 || Math.abs(ctx.getClaimedRole(i) - 3) < 3) continue;
            wuco++;
            if (ctx.getDeathReason(i) == whyDie.NONE) alivewuco++;
        }
        k *= alivewuco;
        k /= Math.max(wuco, 1);
        if (ctx.claimedRoleaskday[3] > 0) k = 0;

        int hiddenHunterCnt = 0;
        for (int i = 1; i <= n; i++)
            if (ctx.getHiddenHunterScheduledSkillTargets()[i][n]) hiddenHunterCnt++;

        // 1f. 白球全局计数
        int[] getWhite = new int[n + 1];
        for (int i = 0; i < ctx.zhans.size(); i++)
        {
            int zhan = ctx.zhans.get(i);
            for (int j = 1; j <= gd; j++)
            {
                int target = ctx.getSkillTarget(zhan, j);
                if (target >= 1 && target <= n) getWhite[target]++;
            }
        }

        final double kFinal = k;
        final int hiddenHunterCntFinal = hiddenHunterCnt;
        final int zhenZhanCntFinal = zhenZhanCnt;
        final int zhenLieCntFinal = zhenLieCnt;
        final int zhenLingCntFinal = zhenLingCnt;
        final int yFinal = y;
        final int zFinal = z;
        final int aliverlFinal = aliverl;

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
                i -> ctx.getKyojin() == 0 && i == ctx.getActualRoleIndex(9),
                i -> -45);

        // --- 末狼猫嫌 ---
        if (aliverlFinal == 1)
        {
            List<Integer> maoSnapshot = new ArrayList<>(ctx.maos);
            engine.addSimpleRule("R05_末狼猫嫌",
                    i -> maoSnapshot.contains(i),
                    i -> -50);
        }

        // --- 潜伏猎人护卫目标修正 ---
        if (hiddenHunterCntFinal > 0 && kFinal > 0)
        {
            engine.addSimpleRule("R06_潜伏猎护卫目标减权",
                    i -> ctx.getHiddenHunterScheduledSkillTargets()[i][n],
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
                i -> isZhan[i],
                i -> 200 - zhenZhanCntFinal * 75 + 50 * yFinal - ctx.zhans.size() * 20);

        engine.addSimpleRule("R09_真灵CO加权",
                i -> isLing[i],
                i -> 175 - zhenLingCntFinal * 100 - ctx.lings.size() * 70);

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

                if (isZhan[i])  // 真占
                {
                    for (int j = 1; j < gd; j++)
                        if (ctx.getDeathReason(j) == whyDie.NONE
                                && ctx.getSkillTarget(i, j) <= n
                                && ctx.getActualRole(i) != 7)
                            weights[ctx.getSkillTarget(i, j)] += 45 + 2 * baiqiu[i] + 10 * yFinal;
                    for (int j = 1; j <= n; j++)
                        if (ctx.getDeathReason(j) == whyDie.NONE
                                && ctx.getClaimedRoleScheduledSkillTargets(i)[j][n]
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
                            weights[t] += 25 + 2 * baiqiu[i];
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
                                && ctx.getClaimedRoleScheduledSkillTargets(i)[j][gd]
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
                            weights[t] += 15 + baiqiu[i];
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
                if (!isLie[i]) continue;
                weights[i] += 350 - zhenLieCntFinal * 100;
                if (ctx.claimedRoleaskday[3] > 0) weights[i] += 50;

                int huweicnt = 0;
                for (int j = 1; j <= n; j++)
                    if (ctx.getClaimedRoleScheduledSkillTargets(i)[j][gd]) huweicnt++;
                if (huweicnt > 0)
                {
                    for (int j = 1; j <= n; j++)
                        if (ctx.getClaimedRoleScheduledSkillTargets(i)[j][gd])
                            weights[j] -= 200 / huweicnt;
                }
            }
        });

        // --- 白球全局修正 ---
        engine.addComplexRule("R15_白球全局修正", weights -> {
            for (int i = 1; i <= n; i++)
                weights[i] += getWhite[i] * 5 + getWhite[i] * getWhite[i] * 2;
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