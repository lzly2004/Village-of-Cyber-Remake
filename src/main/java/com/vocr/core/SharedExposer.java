package com.vocr.core;

import com.vocr.domain.EventName;
import com.vocr.domain.peiyi;
import com.vocr.domain.whyDie;

import com.vocr.config.GameConstants;

public class SharedExposer
{
    private final GameContext ctx;
    private final SuspicionSystem suspicion;
    private final ResultEventGenerator eventGenerator;
    private int exposureProgress;

    public SharedExposer(GameContext ctx, SuspicionSystem suspicion)
    {
        this.ctx = ctx;
        this.suspicion = suspicion;
        this.eventGenerator = new ResultEventGenerator(ctx);
        this.exposureProgress = 0;
    }

    public int getExposureProgress() { return exposureProgress; }

    private void applyGuardianCO(int player)
    {
        ctx.revealClaim(player, 4, ctx.getGameDay());
        ctx.addLazySuspicionValue(player, -GameConstants.INF);
    }

    public void expose()
    {
        if (ctx.getPeiyi() == peiyi.jianyi) return;
        if (ctx.isDead(ctx.getGyIndex(1)) && ctx.isDead(ctx.getGyIndex(2))) return;

        DebugLogger.log("peiyi:" + ctx.getPeiyi());
        if (ctx.getGyIndex(1) < 1) return;
        if (ctx.getClaimedRole(ctx.getGyIndex(1)) == 4 && ctx.getClaimedRole(ctx.getGyIndex(2)) == 4) return;
        if (ctx.getClaimedRole(ctx.getGyIndex(1)) == 0 && ctx.getClaimedRole(ctx.getGyIndex(2)) == 0)
        {
            if (ctx.isDead(ctx.getGyIndex(1)) || ctx.isDead(ctx.getGyIndex(2)))
            {
                int gy = 1;
                if (ctx.isDead(ctx.getGyIndex(1))) gy = 2;
                eventGenerator.addEvent(EventName.gkgsw4, ctx.getGyIndex(gy), ctx.getGyIndex(3 - gy));
                applyGuardianCO(ctx.getGyIndex(1));
                applyGuardianCO(ctx.getGyIndex(2));
                return;
            }
            if (GameLogicUtils.probabilityJudge(GameConstants.PROB_SHARED_EXPOSURE_DECISION))
            {
                if (GameLogicUtils.probabilityJudge(GameConstants.PROB_SHARED_EXPOSURE_DECISION))
                {
                    eventGenerator.addEvent(EventName.gyfo1, ctx.getGyIndex(1), ctx.getGyIndex(2));
                    eventGenerator.addEvent(EventName.gyfo1r, ctx.getGyIndex(2), ctx.getGyIndex(1));
                }
                else
                {
                    eventGenerator.addEvent(EventName.gyfo1, ctx.getGyIndex(2), ctx.getGyIndex(1));
                    eventGenerator.addEvent(EventName.gyfo1r, ctx.getGyIndex(1), ctx.getGyIndex(2));
                }
                applyGuardianCO(ctx.getGyIndex(1));
                applyGuardianCO(ctx.getGyIndex(2));
            }
            else
            {
                exposureProgress = ConstNum.randomInt(1, 90);
                if (GameLogicUtils.probabilityJudge(GameConstants.PROB_SHARED_EXPOSURE_DECISION))
                {
                    eventGenerator.addEvent(EventName.gyho2, ctx.getGyIndex(1));
                    applyGuardianCO(ctx.getGyIndex(1));
                }
                else
                {
                    eventGenerator.addEvent(EventName.gyho2, ctx.getGyIndex(2));
                    applyGuardianCO(ctx.getGyIndex(2));
                }
            }
            return;
        }
        int qfnum = 1;
        if (ctx.getClaimedRole(ctx.getGyIndex(1)) == 4) qfnum = 2;
        if (exposureProgress == 0)
            exposureProgress = ConstNum.randomInt(1, 90);
        else
            exposureProgress += ConstNum.randomInt(5, 15);
        if (ctx.isDead(ctx.getGyIndex(3 - qfnum))
                || ctx.isDead(ctx.getGyIndex(qfnum)))
            exposureProgress = GameConstants.INF;
        if (exposureProgress > 99)
        {
            applyGuardianCO(ctx.getGyIndex(qfnum));
            if (ctx.getDeathReason(ctx.getGyIndex(qfnum)) == whyDie.chuxing)
                eventGenerator.addEvent(EventName.gycx6, ctx.getGyIndex(3 - qfnum), ctx.getGyIndex(qfnum));
            else if (ctx.isDead(ctx.getGyIndex(3 - qfnum)))
                eventGenerator.addEvent(EventName.gkgsw4, ctx.getGyIndex(qfnum), ctx.getGyIndex(3 - qfnum));
            else if (ctx.isDead(ctx.getGyIndex(qfnum)))
                eventGenerator.addEvent(EventName.qfgsw3, ctx.getGyIndex(3 - qfnum), ctx.getGyIndex(qfnum));
            else
            {
                eventGenerator.addEvent(EventName.qfjc5, ctx.getGyIndex(qfnum), ctx.getGyIndex(3 - qfnum));
                eventGenerator.addEvent(EventName.qfjcqr5r, ctx.getGyIndex(3 - qfnum), ctx.getGyIndex(qfnum));
            }
        }
    }
}
