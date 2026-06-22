import java.util.ArrayList;

public class HunterGuarder
{
    private final GameContext ctx;
    private final SuspicionSystem suspicion;

    public HunterGuarder(GameContext ctx, SuspicionSystem suspicion)
    {
        this.ctx = ctx;
        this.suspicion = suspicion;
    }

    public int guard(int num)
    {
        if (ctx.getDeathReason(num) != whyDie.NONE) return 0;

        int n = ctx.getPlayerSum();
        int gd = ctx.getGameDay();
        DebugLogger.log("猎人编号：" + num + " 当前游戏日期：" + gd);
        StringBuilder sb = new StringBuilder("这个猎的预告：");
        for (int i = 1; i <= n; i++)
        {
            if (ctx.getClaimedRoleScheduledSkillTargets(num)[i][gd])
                sb.append(i).append(" ");
        }
        DebugLogger.log(sb.toString());
        sb = new StringBuilder("潜伏猎人的预告：");
        for (int i = 1; i <= n; i++)
        {
            if (ctx.getHiddenHunterScheduledSkillTargets()[i][gd])
                sb.append(i).append(" ");
        }
        DebugLogger.log(sb.toString());
        boolean haveTarget = false;
        boolean qf = false;
        int[] huweiweight = new int[n + 1];
        huweiweight[num] = -GameConstants.INF;
        if (ctx.getClaimedRole(num) != 3) qf = true;
        for (int i = 1; i <= n; i++)
        {
            if (ctx.isDead(i) || num == i)
            {
                continue;
            }
            if ((!qf && !ctx.getClaimedRoleScheduledSkillTargets(num)[i][gd])
                    || (qf && !ctx.getHiddenHunterScheduledSkillTargets()[i][gd]))
            {
                continue;
            }
            haveTarget = true;

            DebugLogger.log("当前猎人编号：" + num + ",指定护卫对象：" + i);
            break;
        }

        if (haveTarget)
        {
            boolean[][] schedule = qf
                ? ctx.getHiddenHunterScheduledSkillTargets()
                : ctx.getClaimedRoleScheduledSkillTargets(num);
            for (int i = 1; i <= n; i++)
            {
                if (ctx.isDead(i) || !schedule[i][gd])
                    huweiweight[i] = -GameConstants.INF;
                else
                    huweiweight[i] = 40;
            }
        }
        else
        {
            for (int i = 1; i <= n; i++)
            {
                if (ctx.isDead(i) || i == num)
                    huweiweight[i] = -GameConstants.INF;
                else
                    huweiweight[i] = 40;
            }
        }

        // ============================================================
        // Phase 2: 规则引擎 —— 所有权重修改
        // ============================================================
        ArrayList<Integer> zhan = new ArrayList<>(ctx.getClaimedRole(1, false));
        ArrayList<Integer> ling = new ArrayList<>(ctx.getClaimedRole(2, false));
        ArrayList<Integer> gong = new ArrayList<>(ctx.getClaimedRole(4, false));
        final ArrayList<Integer> zhanFinal = new ArrayList<>(zhan);
        final ArrayList<Integer> lingFinal = new ArrayList<>(ling);
        final ArrayList<Integer> gongFinal = new ArrayList<>(gong);
        final int numFinal = num;
        RuleEngine engine = new RuleEngine();

        // R1: 占CO加权（占卜师本身是高价值护卫目标）
        engine.addSimpleRule("R01_占CO加权",
                i -> zhanFinal.contains(i),
                i -> 125 - 25 * zhanFinal.size());

        // R2: 占CO黑球目标减权
        engine.addComplexRule("R02_占CO黑白球修正", weights -> {
            for (int j = 0; j < zhanFinal.size(); j++) {
                int z = zhanFinal.get(j);
                for (int k = 1; k < gd; k++) {
                    if (ctx.isBlackResult(ctx.getSkillTarget(z, k))) {
                        weights[ctx.getSkillTarget(z, k) - n] -= 40;
                        if (ctx.getSkillTarget(z, k) - n == numFinal)
                            weights[z] -= 200;
                    } else if (ctx.getSkillTarget(z, k) > 0) {
                        weights[ctx.getSkillTarget(z, k)] += 40;
                    }
                }
            }
        });

        // R3: 灵CO加权
        engine.addSimpleRule("R03_灵CO加权",
                i -> lingFinal.contains(i),
                i -> 75 - 25 * lingFinal.size());

        // R4: 共CO加权
        engine.addSimpleRule("R04_共CO加权",
                i -> gongFinal.contains(i),
                i -> 80);

        // R5: 唯一占CO额外加权
        if (zhanFinal.size() == 1) {
            int soleZhan = zhanFinal.get(0);
            engine.addSimpleRule("R05_唯一占CO额外加权",
                    i -> i == soleZhan,
                    i -> 50);
        }

        // R6: 唯一灵CO额外加权
        if (lingFinal.size() == 1) {
            int soleLing = lingFinal.get(0);
            engine.addSimpleRule("R06_唯一灵CO额外加权",
                    i -> i == soleLing,
                    i -> 50);
        }

        // R7: 非人标记/高怀疑度减权
        engine.addSimpleRule("R07_非人标记高怀疑减权",
                i -> ctx.isNonHumanMarked(i) || ctx.getSuspicionValue(numFinal, i) > GameConstants.INFJ,
                i -> -200);

        // R8: 怀疑度排序加权
        engine.addComplexRule("R08_怀疑度排序加权", weights -> {
            ArrayList<Integer> suspectorder = GameLogicUtils.getPriority(ctx.getSuspicionValueArray(numFinal), true);
            for (int i = 1; i <= n; i++)
                weights[i] += 5 * suspectorder.get(i);
        });

        engine.apply(huweiweight);

        // ============================================================
        // Phase 3: 选择目标
        // ============================================================

        ctx.setSkillTarget(num, gd, suspicion.getOne(huweiweight));

        DebugLogger.log("最终护卫对象：" + CharacterKanjiName.values()[ctx.getCharacterNumber(ctx.getSkillTarget(num, gd))]);
        return ctx.getSkillTarget(num, gd);
    }
}