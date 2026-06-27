import java.util.ArrayList;

public class ResultPresenter
{
    private final GameContext ctx;
    private final SuspicionSystem suspicion;

    public ResultPresenter(GameContext ctx, SuspicionSystem suspicion)
    {
        this.ctx = ctx;
        this.suspicion = suspicion;
    }

    public void presentZhan(int num, ArrayList<Integer> diebody)
    {
        if (ctx.getDeathReason(num) != whyDie.NONE || ctx.isNonHumanMarked(num)) return;
        int n = ctx.getPlayerSum();
        int gd = ctx.getGameDay();
        int target = ctx.getSkillTarget(num, gd - 1);
        if (target < 1 || target == num)
        {
            ctx.eventarray.add(new Event(EventName.wz17, ctx.getCharacterName(num)));
            return;
        }
        if (diebody.size() < 2)
        {
            if (ctx.isBlackResult(target))
            {
                target -= n;
                ctx.addLazySuspicionValue(target, GameConstants.SUSPICION_INCREASE_BLACK_BALL_TARGET);
                ctx.addLazySuspicionValue(num, GameConstants.SUSPICION_INCREASE_BLACK_BALL_CASTER);
                suspicion.updateTop3Aux2(num, target, GameConstants.INF, GameConstants.INF);
                ctx.eventarray.add(new Event(EventName.zjgh8b, ctx.getCharacterName(num),
                        ctx.getCharacterName(target)));
                presentBlackResult(num, target, false);
            }
            else if (ctx.getDeathReason(target) == whyDie.NONE)
            {
                ctx.addLazySuspicionValue(target, GameConstants.SUSPICION_DECREASE_WHITE_BALL_TARGET);
                suspicion.updateTop3Aux2(num, target, -5, -10);
                ctx.eventarray.add(new Event(EventName.zjgb8, ctx.getCharacterName(num),
                        ctx.getCharacterName(target)));
                ctx.eventarray.add(new Event(EventName.jbdh8r,
                        ctx.getCharacterName(target),
                        ctx.getCharacterName(num)));
            }
            else
            {
                ctx.eventarray.add(new Event(EventName.zbdxsw10,
                        ctx.getCharacterName(num),
                        ctx.getCharacterName(target)));
            }
        }
        else
        {
            boolean ackWhiteCat = suspicion.isAckWhite(ctx.getCat(), ctx.maos,
                    ctx.isDoubleDeathOccurred, ctx.claimedRoleaskday);
            if (ackWhiteCat) suspicion.markAckWhite(ctx.getCat());
            if (ctx.getCat() > 0 && ackWhiteCat && !diebody.contains(ctx.getCat())
                    && !diebody.contains(target))
            {
                ctx.eventarray.add(new Event(EventName.zspz15,
                        ctx.getCharacterName(num)));
                ctx.addLazySuspicionValue(num, GameConstants.INF);
                ctx.markNonHuman(num);
            }
            else if ((ctx.getPeiyi() != peiyi.daxing && ctx.getPeiyi() != peiyi.maoyou) || diebody.size() > 2)
            {
                if (diebody.contains(target))
                {
                    ctx.eventarray.add(new Event(EventName.zs14,
                            ctx.getCharacterName(num),
                            ctx.getCharacterName(target)));
                }
                else
                {
                    ctx.eventarray.add(new Event(EventName.zspz15,
                            CharacterEnglishName.values()[ctx.getCharacterNumber(num)]));
                    ctx.addLazySuspicionValue(num, GameConstants.INF);
                    ctx.markNonHuman(num);
                }
            }
            else
            {
                if (diebody.contains(target))
                {
                    ctx.eventarray.add(new Event(EventName.szsm16,
                            ctx.getCharacterName(num),
                            ctx.getCharacterName(target)));
                }
                else if (target <= n)
                {
                    ctx.addLazySuspicionValue(target, GameConstants.SUSPICION_DECREASE_WHITE_BALL_TARGET);
                    suspicion.updateTop3Aux2(num, target, -5, -10);
                    ctx.eventarray.add(new Event(EventName.zjgb8,
                            ctx.getCharacterName(num),
                            ctx.getCharacterName(target)));
                    ctx.eventarray.add(new Event(EventName.jbdh8r,
                            ctx.getCharacterName(target),
                            ctx.getCharacterName(num)));
                }
                else
                {
                    target -= n;
                    ctx.addLazySuspicionValue(target, 10);
                    ctx.addLazySuspicionValue(num, 2);
                    suspicion.updateTop3Aux2(num, target, GameConstants.INF, GameConstants.INF);
                    ctx.eventarray.add(new Event(EventName.zjgh8b,
                            ctx.getCharacterName(num),
                            ctx.getCharacterName(target)));
                    if (ctx.getDeathReason(target) == whyDie.NONE)
                        ctx.eventarray.add(new Event(EventName.jhdh8b,
                                ctx.getCharacterName(target),
                                ctx.getCharacterName(num)));
                }
            }
        }
    }

    /** 黑球结果事件生成（COManager/presentZhan共用） */
    public void presentBlackResult(int num, int target, boolean checkClaimedRole)
    {
        int gd = ctx.getGameDay();
        if (ctx.getDeathReason(target) == whyDie.NONE)
        {
            boolean isGong = checkClaimedRole ?
                    ctx.getClaimedRole(target) == 4 : ctx.getActualRole(target) == 4;
            if (!isGong)
                ctx.eventarray.add(new Event(EventName.jhdh8b,
                        ctx.getCharacterName(target),
                        ctx.getCharacterName(num)));
            else
            {
                ctx.eventarray.add(new Event(EventName.gprz11r,
                        ctx.getCharacterName(target),
                        ctx.getCharacterName(num)));
                int gy = 1;
                if (ctx.gyindex[2] == target) gy = 2;
                if (ctx.getDeathReason(ctx.gyindex[3 - gy]) == whyDie.NONE)
                    ctx.eventarray.add(new Event(EventName.gprz11p,
                            ctx.getCharacterName(ctx.gyindex[3 - gy]),
                            ctx.getCharacterName(target)));
                ctx.addLazySuspicionValue(num, GameConstants.INF);
                ctx.addLazySuspicionValue(target, -GameConstants.INF);
                ctx.setClaimedRole(target, 4);
                ctx.setComingOutDay(target, gd);
                ctx.markNonHuman(num);
                if (ctx.getClaimedRole(ctx.gyindex[3 - gy]) != 4)
                {
                    ctx.setClaimedRole(ctx.gyindex[3 - gy], 4);
                    ctx.setComingOutDay(ctx.gyindex[3 - gy], gd);
                }
            }
        }
    }

    public void presentLing(int num)
    {
        int gd = ctx.getGameDay();
        if (ctx.getDeathReason(num) != whyDie.NONE) return;
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
        int diePlayer = ctx.getDiePlayerNum(whyDie.chuxing, gd - 1);
        if (ctx.isBlackResult(ctx.getSkillTarget(num, gd - 1)))
        {
            ctx.eventarray.add(new Event(EventName.ljgh19b,
                    ctx.getCharacterName(num),
                    ctx.getCharacterName(diePlayer)));
        }
        else
        {
            ctx.eventarray.add(new Event(EventName.ljgb19,
                    ctx.getCharacterName(num),
                    ctx.getCharacterName(diePlayer)));
        }
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
}