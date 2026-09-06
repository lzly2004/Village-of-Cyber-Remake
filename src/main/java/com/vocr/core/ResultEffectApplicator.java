package com.vocr.core;

import com.vocr.domain.whyDie;

import com.vocr.config.GameConstants;

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
        suspicion.adjustMutualSuspicion(num, target, GameConstants.INF, GameConstants.INF);
    }

    public void applyWhiteBallSuspicion(int num, int target)
    {
        ctx.addLazySuspicionValue(target, GameConstants.SUSPICION_DECREASE_WHITE_BALL_TARGET);
        suspicion.adjustMutualSuspicion(num, target, -5, -10);
    }

    public void applyContradictionMark(int num)
    {
        ctx.addLazySuspicionValue(num, GameConstants.INF);
        ctx.markNonHuman(num);
    }

    public boolean checkAndMarkAckWhiteCat()
    {
        boolean ackWhiteCat = ctx.isAckWhite(ctx.getCat());
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
                ctx.revealClaim(target, 4, gd);
                ctx.markNonHuman(num);
                int gy = 1;
                if (ctx.gyindex[2] == target) gy = 2;
                if (ctx.getClaimedRole(ctx.gyindex[3 - gy]) != 4)
                    ctx.revealClaim(ctx.gyindex[3 - gy], 4, gd);
            }
        }
    }

    public void applyLingCoEffects(int num)
    {
        int gd = ctx.getGameDay();
        if (ctx.getClaimedRole(num) != 2)
        {
            for (int i = 0; i < ctx.getLings().size(); i++)
            {
                suspicion.adjustMutualSuspicion(num, ctx.getLings().get(i), GameConstants.INF, GameConstants.INF);
            }
            ctx.registerClaim(num, 2);
            ctx.addLazySuspicionValue(num, GameConstants.SUSPICION_DECREASE_MEDIUM_CO);
            if (gd > 2)
                ctx.addLazySuspicionValue(num, GameConstants.SUSPICION_INCREASE_LATE_CO);
        }
    }

    public void applyLingConflictDetection(int num)
    {
        int gd = ctx.getGameDay();
        for (int i = 0; i < ctx.getZhans().size(); i++)
        {
            for (int j = 1; j <= gd; j++)
            {
                if (ctx.getSuspicionValue(num, ctx.getZhans().get(i)) <= GameConstants.INFJ
                        && Math.abs(ctx.getSkillTarget(num, gd - 1)
                        - ctx.getSkillTarget(ctx.getZhans().get(i), j)) == gd)
                {
                    ctx.addLazySuspicionValue(num, GameConstants.SUSPICION_INCREASE_CONFLICT);
                    ctx.addLazySuspicionValue(ctx.getZhans().get(i), GameConstants.SUSPICION_INCREASE_CONFLICT);
                    suspicion.adjustMutualSuspicion(num, ctx.getZhans().get(i), GameConstants.INF, GameConstants.INF);
                }
                if (ctx.getSuspicionValue(num, ctx.getZhans().get(i)) <= GameConstants.INFJ
                        && ctx.getSkillTarget(num, gd - 1) == ctx.getSkillTarget(ctx.getZhans().get(i), j)
                        && ctx.isBlackResult(ctx.getSkillTarget(ctx.getZhans().get(i), j)))
                {
                    suspicion.adjustMutualSuspicion(num, ctx.getZhans().get(i), -10, -10);
                    ctx.lined[ctx.getZhans().get(i)][num] = 1;
                }
            }
        }
    }

    public void applyDoubleDeathBlackBallSuspicion(int num, int target)
    {
        ctx.addLazySuspicionValue(target, 10);
        ctx.addLazySuspicionValue(num, 2);
        suspicion.adjustMutualSuspicion(num, target, GameConstants.INF, GameConstants.INF);
    }
}
