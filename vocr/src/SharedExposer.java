public class SharedExposer
{
    private final GameContext ctx;
    private final SuspicionSystem suspicion;
    private int exposureProgress;

    public SharedExposer(GameContext ctx, SuspicionSystem suspicion)
    {
        this.ctx = ctx;
        this.suspicion = suspicion;
        this.exposureProgress = 0;
    }

    public int getExposureProgress() { return exposureProgress; }

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
                ctx.eventarray.add(new Event(EventName.gkgsw4,
                        ctx.getCharacterName(ctx.gyindex[gy]),
                        ctx.getCharacterName(ctx.gyindex[3 - gy])));
                ctx.setClaimedRole(ctx.gyindex[1], 4);
                ctx.setComingOutDay(ctx.gyindex[1], ctx.getGameDay());
                ctx.setClaimedRole(ctx.gyindex[2], 4);
                ctx.setComingOutDay(ctx.gyindex[2], ctx.getGameDay());
                ctx.addLazySuspicionValue(ctx.gyindex[1], -GameConstants.INF);
                ctx.addLazySuspicionValue(ctx.gyindex[2], -GameConstants.INF);
                return;
            }
            if (GameLogicUtils.probabilityJudge(GameConstants.PROB_SHARED_EXPOSURE_DECISION))
            {
                if (GameLogicUtils.probabilityJudge(GameConstants.PROB_SHARED_EXPOSURE_DECISION))
                {
                    ctx.eventarray.add(new Event(EventName.gyfo1,
                            ctx.getCharacterName(ctx.gyindex[1]),
                            ctx.getCharacterName(ctx.gyindex[2])));
                    ctx.eventarray.add(new Event(EventName.gyfo1r,
                            ctx.getCharacterName(ctx.gyindex[2]),
                            ctx.getCharacterName(ctx.gyindex[1])));
                }
                else
                {
                    ctx.eventarray.add(new Event(EventName.gyfo1,
                            ctx.getCharacterName(ctx.gyindex[2]),
                            ctx.getCharacterName(ctx.gyindex[1])));
                    ctx.eventarray.add(new Event(EventName.gyfo1r,
                            ctx.getCharacterName(ctx.gyindex[1]),
                            ctx.getCharacterName(ctx.gyindex[2])));
                }
                ctx.setClaimedRole(ctx.gyindex[1], 4);
                ctx.setComingOutDay(ctx.gyindex[1], ctx.getGameDay());
                ctx.setClaimedRole(ctx.gyindex[2], 4);
                ctx.setComingOutDay(ctx.gyindex[2], ctx.getGameDay());
                ctx.addLazySuspicionValue(ctx.gyindex[1], -GameConstants.INF);
                ctx.addLazySuspicionValue(ctx.gyindex[2], -GameConstants.INF);
            }
            else
            {
                exposureProgress = ConstNum.randomInt(1, 90);
                if (GameLogicUtils.probabilityJudge(GameConstants.PROB_SHARED_EXPOSURE_DECISION))
                {
                    ctx.eventarray.add(new Event(EventName.gyho2,
                            ctx.getCharacterName(ctx.gyindex[1])));
                    ctx.setClaimedRole(ctx.gyindex[1], 4);
                    ctx.setComingOutDay(ctx.gyindex[1], ctx.getGameDay());
                    ctx.addLazySuspicionValue(ctx.gyindex[1], -GameConstants.INF);
                }
                else
                {
                    ctx.eventarray.add(new Event(EventName.gyho2,
                            ctx.getCharacterName(ctx.gyindex[2])));
                    ctx.setClaimedRole(ctx.gyindex[2], 4);
                    ctx.setComingOutDay(ctx.gyindex[2], ctx.getGameDay());
                    ctx.addLazySuspicionValue(ctx.gyindex[2], -GameConstants.INF);
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
            ctx.setClaimedRole(ctx.gyindex[qfnum], 4);
            ctx.setComingOutDay(ctx.gyindex[qfnum], ctx.getGameDay());
            ctx.addLazySuspicionValue(ctx.gyindex[qfnum], -GameConstants.INF);
            if (ctx.getDeathReason(ctx.gyindex[qfnum]) == whyDie.chuxing)
                ctx.eventarray.add(new Event(EventName.gycx6,
                        ctx.getCharacterName(ctx.gyindex[3 - qfnum]),
                        ctx.getCharacterName(ctx.gyindex[qfnum])));
            else if (ctx.isDead(ctx.gyindex[3 - qfnum]))
                ctx.eventarray.add(new Event(EventName.gkgsw4,
                        ctx.getCharacterName(ctx.gyindex[qfnum]),
                        ctx.getCharacterName(ctx.gyindex[3 - qfnum])));
            else if (ctx.isDead(ctx.gyindex[qfnum]))
                ctx.eventarray.add(new Event(EventName.qfgsw3,
                        ctx.getCharacterName(ctx.gyindex[3 - qfnum]),
                        ctx.getCharacterName(ctx.gyindex[qfnum])));
            else
            {
                ctx.eventarray.add(new Event(EventName.qfjc5,
                        ctx.getCharacterName(ctx.gyindex[qfnum]),
                        ctx.getCharacterName(ctx.gyindex[3 - qfnum])));
                ctx.eventarray.add(new Event(EventName.qfjcqr5r,
                        ctx.getCharacterName(ctx.gyindex[3 - qfnum]),
                        ctx.getCharacterName(ctx.gyindex[qfnum])));
            }
        }
    }
}