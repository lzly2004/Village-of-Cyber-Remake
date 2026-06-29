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
        ctx.setClaimedRole(player, 4);
        ctx.setComingOutDay(player, ctx.getGameDay());
        ctx.addLazySuspicionValue(player, -GameConstants.INF);
    }

    public void expose()
    {
        if (ctx.getPeiyi() == peiyi.jianyi) return;
        if (ctx.isDead(ctx.gyindex[1]) && ctx.isDead(ctx.gyindex[2])) return;

        DebugLogger.log("peiyi:" + ctx.getPeiyi());
        if (ctx.gyindex[1] < 1) return;
        if (ctx.getClaimedRole(ctx.gyindex[1]) == 4 && ctx.getClaimedRole(ctx.gyindex[2]) == 4) return;
        if (ctx.getClaimedRole(ctx.gyindex[1]) == 0 && ctx.getClaimedRole(ctx.gyindex[2]) == 0)
        {
            if (ctx.isDead(ctx.gyindex[1]) || ctx.isDead(ctx.gyindex[2]))
            {
                int gy = 1;
                if (ctx.isDead(ctx.gyindex[1])) gy = 2;
                eventGenerator.addEvent(EventName.gkgsw4, ctx.gyindex[gy], ctx.gyindex[3 - gy]);
                applyGuardianCO(ctx.gyindex[1]);
                applyGuardianCO(ctx.gyindex[2]);
                return;
            }
            if (GameLogicUtils.probabilityJudge(GameConstants.PROB_SHARED_EXPOSURE_DECISION))
            {
                if (GameLogicUtils.probabilityJudge(GameConstants.PROB_SHARED_EXPOSURE_DECISION))
                {
                    eventGenerator.addEvent(EventName.gyfo1, ctx.gyindex[1], ctx.gyindex[2]);
                    eventGenerator.addEvent(EventName.gyfo1r, ctx.gyindex[2], ctx.gyindex[1]);
                }
                else
                {
                    eventGenerator.addEvent(EventName.gyfo1, ctx.gyindex[2], ctx.gyindex[1]);
                    eventGenerator.addEvent(EventName.gyfo1r, ctx.gyindex[1], ctx.gyindex[2]);
                }
                applyGuardianCO(ctx.gyindex[1]);
                applyGuardianCO(ctx.gyindex[2]);
            }
            else
            {
                exposureProgress = ConstNum.randomInt(1, 90);
                if (GameLogicUtils.probabilityJudge(GameConstants.PROB_SHARED_EXPOSURE_DECISION))
                {
                    eventGenerator.addEvent(EventName.gyho2, ctx.gyindex[1]);
                    applyGuardianCO(ctx.gyindex[1]);
                }
                else
                {
                    eventGenerator.addEvent(EventName.gyho2, ctx.gyindex[2]);
                    applyGuardianCO(ctx.gyindex[2]);
                }
            }
            return;
        }
        int qfnum = 1;
        if (ctx.getClaimedRole(ctx.gyindex[1]) == 4) qfnum = 2;
        if (exposureProgress == 0)
            exposureProgress = ConstNum.randomInt(1, 90);
        else
            exposureProgress += ConstNum.randomInt(5, 15);
        if (ctx.isDead(ctx.gyindex[3 - qfnum])
                || ctx.isDead(ctx.gyindex[qfnum]))
            exposureProgress = GameConstants.INF;
        if (exposureProgress > 99)
        {
            applyGuardianCO(ctx.gyindex[qfnum]);
            if (ctx.getDeathReason(ctx.gyindex[qfnum]) == whyDie.chuxing)
                eventGenerator.addEvent(EventName.gycx6, ctx.gyindex[3 - qfnum], ctx.gyindex[qfnum]);
            else if (ctx.isDead(ctx.gyindex[3 - qfnum]))
                eventGenerator.addEvent(EventName.gkgsw4, ctx.gyindex[qfnum], ctx.gyindex[3 - qfnum]);
            else if (ctx.isDead(ctx.gyindex[qfnum]))
                eventGenerator.addEvent(EventName.qfgsw3, ctx.gyindex[3 - qfnum], ctx.gyindex[qfnum]);
            else
            {
                eventGenerator.addEvent(EventName.qfjc5, ctx.gyindex[qfnum], ctx.gyindex[3 - qfnum]);
                eventGenerator.addEvent(EventName.qfjcqr5r, ctx.gyindex[3 - qfnum], ctx.gyindex[qfnum]);
            }
        }
    }
}