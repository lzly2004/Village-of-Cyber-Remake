import java.util.ArrayList;

public class ResultPresenter
{
    private final GameContext ctx;
    private final ResultEventGenerator eventGenerator;
    private final ResultEffectApplicator effectApplicator;

    public ResultPresenter(GameContext ctx, SuspicionSystem suspicion)
    {
        this.ctx = ctx;
        this.eventGenerator = new ResultEventGenerator(ctx);
        this.effectApplicator = new ResultEffectApplicator(ctx, suspicion);
    }

    public void presentZhan(int num, ArrayList<Integer> diebody)
    {
        if (ctx.getDeathReason(num) != whyDie.NONE || ctx.isNonHumanMarked(num)) return;
        int n = ctx.getPlayerSum();
        int target = ctx.getSkillTarget(num, ctx.getGameDay() - 1);
        if (target < 1 || target == num)
        {
            eventGenerator.addEvent(EventName.wz17, num);
            return;
        }
        if (diebody.size() < 2)
        {
            if (ctx.isBlackResult(target))
            {
                target -= n;
                effectApplicator.applyBlackBallSuspicion(num, target);
                eventGenerator.addEvent(EventName.zjgh8b, num, target);
                presentBlackResult(num, target, false);
            }
            else if (ctx.getDeathReason(target) == whyDie.NONE)
            {
                effectApplicator.applyWhiteBallSuspicion(num, target);
                eventGenerator.addEvent(EventName.zjgb8, num, target);
                eventGenerator.addEvent(EventName.jbdh8r, target, num);
            }
            else
            {
                eventGenerator.addEvent(EventName.zbdxsw10, num, target);
            }
        }
        else
        {
            boolean ackWhiteCat = effectApplicator.checkAndMarkAckWhiteCat();
            if (ctx.getCat() > 0 && ackWhiteCat && !diebody.contains(ctx.getCat())
                    && !diebody.contains(target))
            {
                eventGenerator.addEvent(EventName.zspz15, num);
                effectApplicator.applyContradictionMark(num);
            }
            else if ((ctx.getPeiyi() != peiyi.daxing && ctx.getPeiyi() != peiyi.maoyou) || diebody.size() > 2)
            {
                if (diebody.contains(target))
                {
                    eventGenerator.addEvent(EventName.zs14, num, target);
                }
                else
                {
                    eventGenerator.addEventWithEnglishName(EventName.zspz15, num);
                    effectApplicator.applyContradictionMark(num);
                }
            }
            else
            {
                if (diebody.contains(target))
                {
                    eventGenerator.addEvent(EventName.szsm16, num, target);
                }
                else if (target <= n)
                {
                    effectApplicator.applyWhiteBallSuspicion(num, target);
                    eventGenerator.addEvent(EventName.zjgb8, num, target);
                    eventGenerator.addEvent(EventName.jbdh8r, target, num);
                }
                else
                {
                    target -= n;
                    effectApplicator.applyDoubleDeathBlackBallSuspicion(num, target);
                    eventGenerator.addEvent(EventName.zjgh8b, num, target);
                    if (ctx.getDeathReason(target) == whyDie.NONE)
                        eventGenerator.addEvent(EventName.jhdh8b, target, num);
                }
            }
        }
    }

    public void presentBlackResult(int num, int target, boolean checkClaimedRole)
    {
        effectApplicator.applyBlackResultEffects(num, target, checkClaimedRole);
        generateBlackResultEvents(num, target, checkClaimedRole);
    }

    private void generateBlackResultEvents(int num, int target, boolean checkClaimedRole)
    {
        if (ctx.getDeathReason(target) == whyDie.NONE)
        {
            boolean isGong = checkClaimedRole ?
                    ctx.getClaimedRole(target) == 4 : ctx.getActualRole(target) == 4;
            if (!isGong)
            {
                eventGenerator.addEvent(EventName.jhdh8b, target, num);
            }
            else
            {
                eventGenerator.addEvent(EventName.gprz11r, target, num);
                int gy = 1;
                if (ctx.gyindex[2] == target) gy = 2;
                if (ctx.getDeathReason(ctx.gyindex[3 - gy]) == whyDie.NONE)
                    eventGenerator.addEvent(EventName.gprz11p, ctx.gyindex[3 - gy], target);
            }
        }
    }

    public void presentLing(int num)
    {
        effectApplicator.applyLingCoEffects(num);
        generateLingEvents(num);
        effectApplicator.applyLingConflictDetection(num);
    }

    private void generateLingEvents(int num)
    {
        int gd = ctx.getGameDay();
        if (ctx.getDeathReason(num) != whyDie.NONE) return;
        int diePlayer = ctx.getDiePlayerNum(whyDie.chuxing, gd - 1);
        if (ctx.isBlackResult(ctx.getSkillTarget(num, gd - 1)))
        {
            eventGenerator.addEvent(EventName.ljgh19b, num, diePlayer);
        }
        else
        {
            eventGenerator.addEvent(EventName.ljgb19, num, diePlayer);
        }
    }
}