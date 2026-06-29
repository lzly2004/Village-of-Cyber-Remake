import java.util.ArrayList;

public class SeerDiviner
{
    private final GameContext ctx;
    private final SuspicionSystem suspicion;

    public SeerDiviner(GameContext ctx, SuspicionSystem suspicion)
    {
        this.ctx = ctx;
        this.suspicion = suspicion;
    }

    public int divine(int znum)
    {
        if (ctx.getDeathReason(znum) != whyDie.NONE) return -1;

        int n = ctx.getPlayerSum();
        int gd = ctx.getGameDay();

        DebugLogger.log("占卜师编号：" + znum + " 当前游戏日期：" + gd);
        DebugLogger.log(GameLogicUtils.buildSkillScheduleLog("这个占的预告：", n, gd, ctx.getClaimedRoleScheduledSkillTargets(znum)));
        int[] weight = new int[n + 1];
        ArrayList<Integer> target = new ArrayList<>();
        boolean[] istarget = new boolean[n + 1];
        for (int i = 1; i <= n; i++)
        {
            istarget[i] = true;
        }
        for (int i = 1; i < gd; i++)
        {
            int ztarget = ctx.getSkillTarget(znum, i);
            if (ztarget < 1) continue;
            if (ctx.isBlackResult(ztarget)) ztarget = ctx.getActualTarget(ztarget);
            istarget[ztarget] = false;
        }
        int claimedRoleScheduledSkillTargetssum = 0;
        boolean[][] schedule = (ctx.getClaimedRole(znum) == 1)
            ? ctx.getClaimedRoleScheduledSkillTargets(znum)
            : ctx.getHiddenSeerScheduledSkillTargets();

        for (int i = 1; i <= n; i++)
        {
            if (!istarget[i]) continue;
            if (!schedule[i][gd])
            {
                istarget[i] = false;
                continue;
            }
            boolean ackW = ctx.isAckWhite(i);
            if (ackW) suspicion.markAckWhite(i);
            if ((ctx.getDeathReason(i) != whyDie.NONE &&
                    (ctx.getDeathDay(i) != gd || GameLogicUtils.isDayDie(ctx.getDeathReason(i))))
                    || i == znum || ackW)
            {
                DebugLogger.log("排除玩家：" + CharacterKanjiName.values()[i]);
                istarget[i] = false;
                continue;
            }
            istarget[i] = true;
            claimedRoleScheduledSkillTargetssum++;
            target.add(i);
            weight[i] = 50;
        }
        if (claimedRoleScheduledSkillTargetssum == 1)
            return assignDivineResult(znum, (int) target.get(0));
        if (claimedRoleScheduledSkillTargetssum == 0)
        {
            for (int i = 1; i <= n; i++)
            {
                boolean ackW3 = ctx.isAckWhite(i);
                if (ackW3) suspicion.markAckWhite(i);
                if ((ctx.getDeathReason(i) != whyDie.NONE && (ctx.getDeathDay(i) != gd || GameLogicUtils.isDayDie(ctx.getDeathReason(i)))) || i == znum || ackW3)
                {
                    istarget[i] = false;
                    continue;
                }
                istarget[i] = true;
                for (int j = 1; j <= gd; j++)
                {
                    if (ctx.getSkillTarget(znum, j) == i || ctx.getSkillTarget(znum, j) == i + n)
                    {
                        istarget[i] = false;
                        break;
                    }
                }
                if (istarget[i])
                {
                    target.add(i);
                    weight[i] = 50;
                }
            }
            if (target.size() == 0) return -1;
        }
        // ============================================================
        // Phase 2: 规则引擎 —— 所有权重修改
        // ============================================================
        final boolean[] istargetFinal = istarget;
        RuleEngine engine = new RuleEngine();

        // R1: 非目标排除
        engine.addSimpleRule("R01_非目标排除",
                i -> !istargetFinal[i],
                i -> -GameConstants.INF);

        // R2: 其他占卜师白球目标减权
        engine.addComplexRule("R02_其他占白球目标减权", weights -> {
            for (int i = 1; i <= n; i++)
            {
                if (ctx.getClaimedRole(i) == 1 && !ctx.isNonHumanMarked(i))
                {
                    for (int j = 1; j <= gd; j++)
                    {
                        int t = ctx.getSkillTarget(i, j);
                        if (t < n && istargetFinal[t])
                            weights[t] -= 35;
                        if (t - n > 0 && istargetFinal[t - n])
                            weights[t - n] -= 45;
                    }
                }
            }
        });

        // R3: top3怀疑度归一化修正
        engine.addComplexRule("R03_top3怀疑度归一化", weights -> {
            int[] top3cnt = new int[n + 1];
            for (int i = 1; i <= n; i++)
                for (int j = 1; j <= 3; j++)
                    if (istargetFinal[ctx.getTop3SuspectedPlayer(i, j, gd)])
                        top3cnt[ctx.getTop3SuspectedPlayer(i, j, gd)]++;
            int mincnt = Integer.MAX_VALUE;
            for (int i = 1; i <= n; i++)
                if (istargetFinal[i]) mincnt = Math.min(mincnt, top3cnt[i]);
            for (int i = 1; i <= n; i++)
                if (istargetFinal[i]) weights[i] += top3cnt[i] - mincnt;
        });

        // R4: CO角色修正 —— 占CO/灵CO减权
        engine.addSimpleRule("R04_占CO目标减权",
                i -> istargetFinal[i] && ctx.getClaimedRole(i) == 1,
                i -> -45);

        engine.addSimpleRule("R05_灵CO目标减权",
                i -> istargetFinal[i] && ctx.getClaimedRole(i) == 2,
                i -> -45);

        // R6: 猫猎CO目标减权
        engine.addSimpleRule("R06_猫猎CO目标减权",
                i -> istargetFinal[i] && (ctx.getClaimedRole(i) == 3 || ctx.getClaimedRole(i) == 5),
                i -> -35);

        // R7: 最小权重归一化 —— 确保所有目标权重 >= 5
        engine.addComplexRule("R07_最小权重归一化", weights -> {
            int minw = Integer.MAX_VALUE;
            for (int i = 1; i <= n; i++)
                if (istargetFinal[i]) minw = Math.min(minw, weights[i]);
            if (minw < 5)
            {
                int diff = 5 - minw;
                for (int i = 1; i <= n; i++)
                    if (istargetFinal[i]) weights[i] += diff;
            }
        });

        engine.apply(weight);

        // ============================================================
        // Phase 3: 选择目标
        // ============================================================
        return assignDivineResult(znum, suspicion.getOne(weight));
    }

    private int assignDivineResult(int znum, int ztarget)
    {
        int target = (ctx.getActualRole(ztarget) != 7) ? ztarget : ztarget + ctx.getPlayerSum();
        ctx.setSkillTarget(znum, ctx.getGameDay(), target);
        return target;
    }
}