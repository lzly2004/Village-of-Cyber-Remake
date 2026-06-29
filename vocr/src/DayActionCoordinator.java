import java.util.ArrayList;
import java.util.List;

public class DayActionCoordinator
{
    private final GameModule module;
    private final GameContext ctx;
    private final SuspicionSystem suspicion;
    private final COManager coManager;
    private final ResultPresenter resultPresenter;
    private final ProbabilityCalculator probabilityCalculator;
    private final Runnable gylogic;
    private final Runnable deliverEvents;

    public DayActionCoordinator(GameModule module)
    {
        this.module = module;
        this.ctx = module.getCtx();
        this.suspicion = module.getSuspicion();
        this.coManager = module.getCoManager();
        this.resultPresenter = module.getResultPresenter();
        this.probabilityCalculator = module.getProbabilityCalculator();
        this.gylogic = module.getGylogic();
        this.deliverEvents = module.getDeliverEvents();
    }

    public void coordinate(ArrayList<Integer> diebody)
    {
        logDayStart();

        presentDivinationResults(diebody);

        handleLatentSeerMediumCO(diebody);

        handleCatHunterCO(diebody);

        gylogic.run();
        deliverEvents.run();
    }

    // ==================== 子方法 ====================

    private void logDayStart()
    {
        int n = ctx.getPlayerSum();
        DebugLogger.log("现在是白天。当前存活人数" + ctx.getAliveCounter() + ",存活名单：\n");
        String alivemans = "";
        for (int i = 1; i <= n; i++)
            if (ctx.isAlive(i))
                alivemans += CharacterKanjiName.values()[ctx.getCharacterNumber(i)];
        DebugLogger.log(alivemans);
        DebugLogger.log("\n");
    }

    private void presentDivinationResults(ArrayList<Integer> diebody)
    {
        for (int zhan : ctx.zhans) resultPresenter.presentZhan(zhan, diebody);
        for (int ling : ctx.lings) resultPresenter.presentLing(ling);
    }

    private void handleLatentSeerMediumCO(ArrayList<Integer> diebody)
    {
        int n = ctx.getPlayerSum();
        int gd = ctx.getGameDay();
        if(gd == 3)
            for(int i=1;i<=n;i++)
            {
                if(ctx.getClaimedRole(i) != 0) continue;
                if(ctx.getActualRole(i) == 2 || ctx.nonHumanPlan[i] == 2)
                {
                    coManager.processActualCo(i,2,diebody);
                    continue;
                }
                if(ctx.getActualRole(i) == 1 || ctx.nonHumanPlan[i] == 1)
                {
                    coManager.processActualCo(i,1,diebody);
                }
                if(ctx.claimedRoleaskday[1] == 0)
                    ctx.claimedRoleaskday[1] = 3;
                if(ctx.claimedRoleaskday[2] == 0)
                    ctx.claimedRoleaskday[2] = 3;
            }
    }

    private void handleCatHunterCO(ArrayList<Integer> diebody)
    {
        if(ctx.getPeiyi() != peiyi.jianyi && ctx.gyindex[1] > 0 && ctx.isDead(ctx.gyindex[1]) && ctx.isDead(ctx.gyindex[2]))
            coManager.askCoByRole(Role.mao);

        if(diebody.size() == 2)
            coManager.askCoByRole(Role.mao);

        ArrayList<IntPair> response = new ArrayList<>();
        boolean havecatco = handleBlackResponses(response);

        if(havecatco)
            coManager.askCoByRole(Role.mao);
    }

    private boolean handleBlackResponses(ArrayList<IntPair> response)
    {
        int n = ctx.getPlayerSum();
        int gd = ctx.getGameDay();
        boolean havecatco = false;

        if(ctx.claimedRoleaskday[3] == 0 || (ctx.getCat() != 0 && ctx.claimedRoleaskday[5] == 0))
        {
            for(int i=0;i<ctx.zhans.size();i++)
            {
                int zhan = ctx.zhans.get(i);
                if(ctx.isBlackResult(ctx.getSkillTarget(zhan, gd - 1)))
                {
                    int target = ctx.getSkillTarget(zhan, gd - 1) - n;
                    if(ctx.isDead(target) || ctx.getClaimedRole(target) != 0)
                        continue;
                    if(ctx.getActualRole(target) == 5 || ctx.nonHumanPlan[target] == 5)
                    {
                        // 如果猫又已明确出局，非人不能CO猫
                        if(ctx.getCat() == 0 || ctx.isCatDefinitivelyOut())
                            ctx.nonHumanPlan[target] = 0;
                        else
                        {
                            response.add(new IntPair(target,5));havecatco = true;continue;
                        }
                    }
                    if(ctx.getActualRole(target) < 7 || ctx.nonHumanPlan[target] == 0 || ctx.nonHumanPlan[target] == 3) continue;
                    int zhi = ctx.getActualRole(target);
                    int maolieIdx = zhi == 7 ? 0 : zhi - 7;
                    if(GameLogicUtils.probabilityJudge(probabilityCalculator.maolieco(maolieIdx,1,coManager.getp1(zhi),coManager.getp2(zhi))))
                    {
                        int lweight = 50+50*ctx.maos.size(),mweight = 50 + 50*ctx.lies.size();
                        if((zhi == 7 && ctx.rlsl) || ctx.claimedRoleaskday[3] != 0) lweight = 0;
                        if((zhi == 7 && ctx.rlsm) || ctx.getCat() == 0 || ctx.isCatDefinitivelyOut() || ctx.claimedRoleaskday[5] != 0) mweight = 0;
                        if(lweight + mweight == 0) ctx.nonHumanPlan[target] = 0;
                        else
                        {
                            ctx.nonHumanPlan[target] = 3 + 2 * GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(lweight,mweight)));
                            if(ctx.nonHumanPlan[target] == 5)
                            {
                                response.add(new IntPair(target,5));havecatco = true;
                                if(zhi == 7) ctx.rlsm = true;
                            }
                            else if(zhi == 7) ctx.rlsl = true;
                        }
                    }
                    else if(zhi == 7)
                        ctx.nonHumanPlan[target] = 0;
                }
            }
        }
        return havecatco;
    }
}