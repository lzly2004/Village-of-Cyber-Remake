import java.util.ArrayList;
import java.util.List;

public class LingFabricator
{
    private final GameContext ctx;
    private final SuspicionSystem suspicion;

    public LingFabricator(GameContext ctx, SuspicionSystem suspicion)
    {
        this.ctx = ctx;
        this.suspicion = suspicion;
    }

    private int getLingProbability(int actualRole, boolean isTrusted)
    {
        return switch (actualRole) {
            case 7 -> isTrusted ? GameConstants.PROB_LING_TRUSTED_SEER_WOLF
                    : GameConstants.PROB_LING_SUSPICIOUS_SEER_WOLF;
            case 8 -> isTrusted ? GameConstants.PROB_LING_TRUSTED_SEER_MADMAN
                    : GameConstants.PROB_LING_SUSPICIOUS_SEER_MADMAN;
            case 9 -> isTrusted ? GameConstants.PROB_LING_TRUSTED_SEER_FANATIC
                    : GameConstants.PROB_LING_SUSPICIOUS_SEER_FANATIC;
            case 10 -> isTrusted ? GameConstants.PROB_LING_TRUSTED_SEER_FOX
                    : GameConstants.PROB_LING_SUSPICIOUS_SEER_FOX;
            case 11 -> isTrusted ? GameConstants.PROB_LING_TRUSTED_SEER_DEVIANT
                    : GameConstants.PROB_LING_SUSPICIOUS_SEER_DEVIANT;
            default -> isTrusted ? GameConstants.PROB_LING_TRUSTED_SEER_MADMAN
                    : GameConstants.PROB_LING_SUSPICIOUS_SEER_MADMAN;
        };
    }

    public void fabricate(int num)
    {
        if (ctx.isNonHumanMarked(num)) return;

        int n = ctx.getPlayerSum();
        int gd = ctx.getGameDay();
        int shokei = ctx.getDiePlayerNum(whyDie.chuxing, gd);
        ctx.setSkillTarget(num, gd, shokei);
        if (shokei == ctx.getActualRoleIndex(10) && ctx.getActualRoleIndex(11) > 0
                && ctx.getDeathReason(ctx.getActualRoleIndex(11)) == whyDie.dayhouzhui)
        {
            return;
        }
        if (shokei == ctx.getActualRoleIndex(5) && ctx.getActualRoleIndex(5) > 0
                && ctx.getDeathReason(ctx.getActualRoleIndex(5)) == whyDie.chuxing)
        {
            return;
        }
        int heisum = 0, feisum = 0, heip = ctx.initialWolfCount, baip = n - ctx.initialWolfCount;
        int[] blackzhi = new int[7];
        boolean[] linged = new boolean[n * 3];
        for (int i = 2; i < gd; i++)
        {
            int t = ctx.getSkillTarget(num, i);
            linged[t] = true;
            if (ctx.isBlackResult(t))
            {
                t -= n;
                heisum++;
                heip--;
                if (Math.abs(ctx.getClaimedRole(t) - 3) < 3)
                {
                    blackzhi[ctx.getClaimedRole(t)] += 1;
                    feisum--;
                }
            }
            else
            {
                baip--;
            }
        }
        baip -= ctx.computeNightDeathDecrement();
        for (int i = 1; i <= n; i++)
            if (suspicion.isAckWhite(i, ctx.maos, ctx.isDoubleDeathOccurred, ctx.claimedRoleaskday) && ctx.isAlive(i))
            {
                suspicion.markAckWhite(i);
                baip--;
            }
        feisum += ctx.computeFeisum();
        if (feisum + heisum >= ctx.initialNonHumanCount
                && (ctx.getClaimedRole(shokei) == 0 || ctx.getClaimedRole(shokei) == 6))
        {
            return;
        }
        if (heisum >= ctx.initialWolfCount - 1)
        {
            return;
        }
        if (Math.abs(ctx.getClaimedRole(shokei) - 3) < 3)
        {
            if (blackzhi[ctx.getClaimedRole(shokei)] > 0)
            {
                if (ctx.getClaimedRole(shokei) != 1) return;
                if (blackzhi[ctx.getClaimedRole(shokei)] > 1) return;
            }
        }
        ArrayList<Integer> option = new ArrayList<>();
        if (ctx.getActualRole(num) == 7 || ctx.getActualRole(num) == 9)
        {
            if (ctx.getActualRole(shokei) == 7)
                option.add(0);
            collectLingOptions(option, num, shokei, n, gd,
                    zhan -> ctx.getActualRole(zhan) == 7 && ctx.getSuspicionValue(zhan, num) <= GameConstants.INFJ,
                    true);
            if (option.size() > 0)
            {
                int p = GameConstants.PROB_LING_WOLF_FACTION_DEFAULT;
                if (ctx.getActualRole(num) == 9) p = GameConstants.PROB_LING_FANATIC_SPECIAL;
                processLingOption(option, num, p);
                return;
            }
        }
        collectLingOptions(option, num, shokei, n, gd,
                zhan -> ctx.getSuspicionValue(zhan, num) < GameConstants.INFJ
                        && ctx.getSuspicionValue(num, zhan) < GameConstants.INFJ && ctx.lined[zhan][num] == 1,
                true);
        if (option.size() > 0)
        {
            processLingOption(option, num, getLingProbability(ctx.getActualRole(num), true));
            return;
        }
        collectLingOptions(option, num, shokei, n, gd,
                zhan -> ctx.getSuspicionValue(zhan, num) > GameConstants.INFJ
                        && ctx.getSuspicionValue(num, zhan) > GameConstants.INFJ,
                false);
        if (option.size() > 0)
        {
            processLingOption(option, num, getLingProbability(ctx.getActualRole(num), false));
            return;
        }
        int op = GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(baip, heip)));
        if (op == 1)
            ctx.setSkillTarget(num, gd, ctx.getSkillTarget(num, gd) + n);
    }

    private void collectLingOptions(ArrayList<Integer> option, int num, int shokei, int n, int gd,
                                    java.util.function.Predicate<Integer> filter, boolean includeWhite)
    {
        for (int i = 0; i < ctx.zhans.size(); i++)
        {
            int zhan = ctx.zhans.get(i);
            if (!filter.test(zhan)) continue;
            for (int j = 1; j < gd; j++)
            {
                int skillTarget = ctx.getSkillTarget(zhan, j);
                if (skillTarget == shokei)
                    option.add(0);
                else if (skillTarget - shokei == n)
                    option.add(includeWhite ? 1 : 0);
            }
        }
    }

    private void processLingOption(ArrayList<Integer> option, int num, int p)
    {
        int op = option.get(ConstNum.randomInt(0, option.size() - 1));
        if (!GameLogicUtils.probabilityJudge(p)) op = 1 - op;
        if (op == 1)
            ctx.setSkillTarget(num, ctx.getGameDay(), ctx.getSkillTarget(num, ctx.getGameDay()) + ctx.getPlayerSum());
    }
}