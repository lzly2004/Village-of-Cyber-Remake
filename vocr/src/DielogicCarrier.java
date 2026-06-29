import java.util.ArrayList;

public class DielogicCarrier
{
    private final GameContext ctx;
    private final SuspicionSystem suspicion;
    private final Runnable deliverEvents;
    private final ResultEventGenerator eventGenerator;

    public DielogicCarrier(GameContext ctx, SuspicionSystem suspicion, Runnable deliverEvents)
    {
        this.ctx = ctx;
        this.suspicion = suspicion;
        this.deliverEvents = deliverEvents;
        this.eventGenerator = new ResultEventGenerator(ctx);
    }

    public ArrayList<Integer> execute(int wolf, int wolfbite, int zhantarget, int lietarget)
    {
        ArrayList<Integer> diebody = new ArrayList<>();
        ctx.eventarray.clear();

        handleWolfBite(wolf, wolfbite, lietarget, diebody);
        handleCurseKill(zhantarget, diebody);
        shuffleAndLogEvents(diebody);
        handleFakeSeerExposure(diebody);

        deliverEvents.run();
        ctx.setNightDeathCount(ctx.getGameDay(), diebody.size());
        return diebody;
    }

    private void handleWolfBite(int wolf, int wolfbite, int lietarget, ArrayList<Integer> diebody)
    {
        if (wolfbite != lietarget && ctx.getFox() != wolfbite)
        {
            diebody.add(wolfbite);
            dieaux(wolfbite, whyDie.beiyao);
            if (ctx.getActualRole(wolfbite) == 5)
            {
                diebody.add(wolf);
                dieaux(wolf, whyDie.nightmaozhou);
            }
        }
    }

    private void handleCurseKill(int zhantarget, ArrayList<Integer> diebody)
    {
        if (zhantarget > 0 && zhantarget <= ctx.getPlayerSum() && ctx.getActualRole(zhantarget) == 10)
        {
            diebody.add(zhantarget);
            dieaux(zhantarget, whyDie.zhousha);
            if (ctx.getDeviant() > 0 && ctx.isAlive(ctx.getDeviant()))
            {
                diebody.add(ctx.getDeviant());
                dieaux(ctx.getDeviant(), whyDie.nighthouzhui);
            }
        }
    }

    private void shuffleAndLogEvents(ArrayList<Integer> diebody)
    {
        ArrayList<Event> shuffled = GameLogicUtils.shuffleList(ctx.eventarray);
        ctx.eventarray.clear();
        ctx.eventarray.addAll(shuffled);
        for (int i = 0; i < diebody.size(); i++)
        {
            DebugLogger.log("夜间死体：" + CharacterKanjiName.values()[ctx.getCharacterNumber(diebody.get(i))]);
        }
    }

    private void handleFakeSeerExposure(ArrayList<Integer> diebody)
    {
        if (diebody.size() < 1)
        {
            ctx.eventarray.add(new Event(EventName.wsw, null, null));
        }
        else if (diebody.size() == 1)
        {
            exposeFakeSeersByDeath(diebody);
        }
        else
        {
            ctx.isDoubleDeathOccurred[ctx.getGameDay()] = true;
            boolean ackWhite = suspicion.isAckWhite(ctx.getActualRoleIndex(5), ctx.maos,
                    ctx.isDoubleDeathOccurred, ctx.claimedRoleaskday);
            if (ackWhite) suspicion.markAckWhite(ctx.getActualRoleIndex(5));
            if (ctx.getActualRoleIndex(5) > 0 && ctx.isAlive(ctx.getActualRoleIndex(5)) && ackWhite
                    && !diebody.contains(ctx.getActualRoleIndex(5)))
            {
                exposeFakeSeersByDeath(diebody);
            }
        }
    }

    private void exposeFakeSeersByDeath(ArrayList<Integer> diebody)
    {
        for (int i = 0; i < ctx.zhans.size(); i++)
        {
            int zhan = ctx.zhans.get(i);
            if (zhan == ctx.getActualRoleIndex(1)) continue;
            for (int j = 1; j <= ctx.getGameDay(); j++)
            {
                int target = ctx.getSkillTarget(zhan, j) - ctx.getPlayerSum();
                if (target < 1) continue;
                if (diebody.contains(target))
                    ctx.markNonHuman(zhan);
            }
        }
    }

    void dieaux(int index, whyDie why)
    {
        ctx.setDeathReason(index, why);
        ctx.decrementAliveCounter();
        ctx.incrementDeathCounter();
        ctx.setDeathDay(index, ctx.getGameDay());
        if (why == whyDie.chuxing)
        {
            eventGenerator.addEvent(EventName.cxs, index);
            if (ctx.getClaimedRole(index) == 5 && ctx.getActualRole(index) != 5)
            {
                ctx.markNonHuman(index);
            }
        }
        else if (why == whyDie.dayhouzhui)
            eventGenerator.addEvent(EventName.hzsw, index);
        else if (why == whyDie.daymaozhou)
            eventGenerator.addEvent(EventName.mzsw, index);
        else
            eventGenerator.addEvent(EventName.yjsw, index);
    }
}