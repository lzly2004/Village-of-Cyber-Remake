import java.util.ArrayList;
import java.util.List;

public class ZhanFabricator
{
    private final GameContext ctx;
    private final SuspicionSystem suspicion;
    private final SeerDiviner seerDiviner;

    public ZhanFabricator(GameContext ctx, SuspicionSystem suspicion, SeerDiviner seerDiviner)
    {
        this.ctx = ctx;
        this.suspicion = suspicion;
        this.seerDiviner = seerDiviner;
    }

    public void fabricate(ArrayList<Integer> diebody, int num)
    {
        if (ctx.isNonHumanMarked(num)) return;

        int n = ctx.getPlayerSum();
        int gd = ctx.getGameDay();
        if (ctx.getSkillTarget(num, 1) < 1)
        {
            ctx.setSkillTarget(num, 1, ctx.zw[num]);
        }
        int[] target = new int[3];
        boolean[] zhaned = new boolean[n + 1];
        int[] blackzhi = new int[7];
        int heip = ctx.initialWolfCount, baip = n - ctx.initialWolfCount;
        baip -= ctx.computeNightDeathDecrement();
        for (int i = 1; i <= n; i++)
            if (suspicion.isAckWhite(i, ctx.maos, ctx.isDoubleDeathOccurred, ctx.claimedRoleaskday) && ctx.isAlive(i))
            {
                suspicion.markAckWhite(i);
                baip--;
            }
        for (int i = 0; i < 2; i++)
        {
            target[i] = seerDiviner.divine(num);
            if (target[i] < 1) continue;
            if (ctx.isBlackResult(target[i])) target[i] = ctx.getActualTarget(target[i]);
            int heisum = 0, feisum = 0, waiheisum = 0;
            feisum += ctx.computeFeisum();
            for (int j = 1; j < gd; j++)
                if (ctx.isBlackResult(ctx.getSkillTarget(num, j)))
                {
                    heisum++;
                    if (Math.abs(ctx.getClaimedRole(ctx.getSkillTarget(num, j) - n) - 3) < 3)
                    {
                        feisum--;
                    }
                }
            if (heisum >= ctx.initialWolfCount) continue;
            if ((ctx.getClaimedRole(target[i]) == 0 || ctx.getClaimedRole(target[i]) == 6)
                    && feisum + heisum >= ctx.initialNonHumanCount)
                continue;
            if (Math.abs(ctx.getClaimedRole(target[i]) - 3) < 3)
            {
                if (blackzhi[ctx.getClaimedRole(target[i])] > 0 && ctx.getClaimedRole(target[i]) != 1) continue;
                if (ctx.getClaimedRole(target[i]) == 1 && blackzhi[ctx.getClaimedRole(target[i])] > 1) continue;
            }
            int option = GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(baip, heip)));
            target[i] += option * n;
            int targetRole = (option == 1) ? ctx.getActualRole(target[i] - n) : 0;
            DivinationFabricationStrategy strategy = DivinationFabricationStrategy.forRole(ctx.getActualRole(num));
            if (strategy != null)
            {
                target[i] = strategy.adjustTarget(option, target[i], targetRole, n);
            }
        }
        if (target[0] < 1 && target[1] < 1) return;
        if (target[0] < 1) target[0] = target[1];
        if (target[1] < 1) target[1] = target[0];
        int[] weight = new int[3];
        for (int i = 0; i < 2; i++)
        {
            weight[i] = 50;
            if (diebody.contains(target[i]))
                weight[i] += 900;
            else if (diebody.contains(target[i] - n) && diebody.size() < 2)
                weight[i] -= 49;
        }
        int option = GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(weight[0], weight[1])));
        ctx.setSkillTarget(num, gd, target[option]);
    }
}