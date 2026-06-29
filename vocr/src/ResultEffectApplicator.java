public class ResultEffectApplicator
{
    private final GameContext ctx;
    private final SuspicionSystem suspicion;

    public ResultEffectApplicator(GameContext ctx, SuspicionSystem suspicion)
    {
        this.ctx = ctx;
        this.suspicion = suspicion;
    }

    public void applyBlackBallSuspicion(int num, int target)
    {
        ctx.addLazySuspicionValue(target, GameConstants.SUSPICION_INCREASE_BLACK_BALL_TARGET);
        ctx.addLazySuspicionValue(num, GameConstants.SUSPICION_INCREASE_BLACK_BALL_CASTER);
        suspicion.updateTop3Aux2(num, target, GameConstants.INF, GameConstants.INF);
    }

    public void applyWhiteBallSuspicion(int num, int target)
    {
        ctx.addLazySuspicionValue(target, GameConstants.SUSPICION_DECREASE_WHITE_BALL_TARGET);
        suspicion.updateTop3Aux2(num, target, -5, -10);
    }

    public void applyContradictionMark(int num)
    {
        ctx.addLazySuspicionValue(num, GameConstants.INF);
        ctx.markNonHuman(num);
    }

    public boolean checkAndMarkAckWhiteCat()
    {
        boolean ackWhiteCat = suspicion.isAckWhite(ctx.getCat(), ctx.maos,
                ctx.isDoubleDeathOccurred, ctx.claimedRoleaskday);
        if (ackWhiteCat)
            suspicion.markAckWhite(ctx.getCat());
        return ackWhiteCat;
    }

    public void applyBlackResultEffects(int num, int target, boolean checkClaimedRole)
    {
        int gd = ctx.getGameDay();
        if (ctx.getDeathReason(target) == whyDie.NONE)
        {
            boolean isGong = checkClaimedRole ?
                    ctx.getClaimedRole(target) == 4 : ctx.getActualRole(target) == 4;
            if (isGong)
            {
                ctx.addLazySuspicionValue(num, GameConstants.INF);
                ctx.addLazySuspicionValue(target, -GameConstants.INF);
                ctx.setClaimedRole(target, 4);
                ctx.setComingOutDay(target, gd);
                ctx.markNonHuman(num);
                int gy = 1;
                if (ctx.gyindex[2] == target) gy = 2;
                if (ctx.getClaimedRole(ctx.gyindex[3 - gy]) != 4)
                {
                    ctx.setClaimedRole(ctx.gyindex[3 - gy], 4);
                    ctx.setComingOutDay(ctx.gyindex[3 - gy], gd);
                }
            }
        }
    }

    public void applyLingCoEffects(int num)
    {
        int gd = ctx.getGameDay();
        if (ctx.getClaimedRole(num) != 2)
        {
            ctx.setClaimedRole(num, 2);
            ctx.setComingOutDay(num, gd);
            if (ctx.getClaimedRoleOrder(num) == 0)
                ctx.setClaimedRoleOrder(num, ctx.incrementClaimedRoleOrder(2));
            for (int i = 0; i < ctx.lings.size(); i++)
            {
                suspicion.updateTop3Aux2(num, ctx.lings.get(i), GameConstants.INF, GameConstants.INF);
            }
            if (!ctx.lings.contains(num))
                ctx.lings.add(num);
            ctx.addLazySuspicionValue(num, GameConstants.SUSPICION_DECREASE_MEDIUM_CO);
            if (gd > 2)
                ctx.addLazySuspicionValue(num, GameConstants.SUSPICION_INCREASE_LATE_CO);
        }
    }

    public void applyLingConflictDetection(int num)
    {
        int gd = ctx.getGameDay();
        for (int i = 0; i < ctx.zhans.size(); i++)
        {
            for (int j = 1; j <= gd; j++)
            {
                if (ctx.getSuspicionValue(num, ctx.zhans.get(i)) <= GameConstants.INFJ
                        && Math.abs(ctx.getSkillTarget(num, gd - 1)
                        - ctx.getSkillTarget(ctx.zhans.get(i), j)) == gd)
                {
                    ctx.addLazySuspicionValue(num, GameConstants.SUSPICION_INCREASE_CONFLICT);
                    ctx.addLazySuspicionValue(ctx.zhans.get(i), GameConstants.SUSPICION_INCREASE_CONFLICT);
                    suspicion.updateTop3Aux2(num, ctx.zhans.get(i), GameConstants.INF, GameConstants.INF);
                }
                if (ctx.getSuspicionValue(num, ctx.zhans.get(i)) <= GameConstants.INFJ
                        && ctx.getSkillTarget(num, gd - 1) == ctx.getSkillTarget(ctx.zhans.get(i), j)
                        && ctx.isBlackResult(ctx.getSkillTarget(ctx.zhans.get(i), j)))
                {
                    suspicion.updateTop3Aux2(num, ctx.zhans.get(i), -10, -10);
                    ctx.lined[ctx.zhans.get(i)][num] = 1;
                }
            }
        }
    }

    public void applyDoubleDeathBlackBallSuspicion(int num, int target)
    {
        ctx.addLazySuspicionValue(target, 10);
        ctx.addLazySuspicionValue(num, 2);
        suspicion.updateTop3Aux2(num, target, GameConstants.INF, GameConstants.INF);
    }
}