import java.util.ArrayList;
import java.util.List;

public class DayActionCoordinator
{
    private final GameContext ctx;
    private final SuspicionSystem suspicion;
    private final COManager coManager;
    private final ResultPresenter resultPresenter;
    private final ProbabilityCalculator probabilityCalculator;
    private final Runnable gylogic;
    private final Runnable deliverEvents;

    public DayActionCoordinator(GameContext ctx, SuspicionSystem suspicion,
                                COManager coManager, ResultPresenter resultPresenter,
                                ProbabilityCalculator probabilityCalculator,
                                Runnable gylogic, Runnable deliverEvents)
    {
        this.ctx = ctx;
        this.suspicion = suspicion;
        this.coManager = coManager;
        this.resultPresenter = resultPresenter;
        this.probabilityCalculator = probabilityCalculator;
        this.gylogic = gylogic;
        this.deliverEvents = deliverEvents;
    }

    public void coordinate(ArrayList<Integer> diebody)
    {
        int n = ctx.getPlayerSum();
        int gd = ctx.getGameDay();
        DebugLogger.log("现在是白天。当前存活人数" + ctx.getAliveCounter() + ",存活名单：\n");
        String alivemans = "";
        for (int i = 1; i <= n; i++)
            if (ctx.isAlive(i))
                alivemans += CharacterKanjiName.values()[ctx.getCharacterNumber(i)];
        DebugLogger.log(alivemans);
        DebugLogger.log("\n");
        // 1,占灵发球逻辑
        for(int i=0;i<ctx.zhans.size();i++)
        {
            resultPresenter.presentZhan(ctx.zhans.get(i), diebody);
        }
        for(int i=0;i<ctx.lings.size();i++)
        {
            resultPresenter.presentLing(ctx.lings.get(i));
        }
        // 2,潜伏占灵co逻辑
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
        // 3,非人co猫猎逻辑,真猫猎co逻辑
        // 4共有全灭co猫
        if(ctx.getPeiyi() != peiyi.jianyi && ctx.gyindex[1] > 0 && ctx.isDead(ctx.gyindex[1]) && ctx.isDead(ctx.gyindex[2]))
            coManager.askCoByRole(Role.mao);
        // 5猫村双死co猫
        if(diebody.size() == 2)
            coManager.askCoByRole(Role.mao);
        // 1接黒
        ArrayList<IntPair> response = new ArrayList<>();
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
                        response.add(new IntPair(target,5));havecatco = true;continue;
                    }
                    if(ctx.getActualRole(target) < 7 || ctx.nonHumanPlan[target] == 0 || ctx.nonHumanPlan[target] == 3) continue;
                    int zhi = ctx.getActualRole(target);
                    switch(zhi)
                    {
                        case  7:
                            if(GameLogicUtils.probabilityJudge(probabilityCalculator.maolieco(0,1,ctx.getp1(7),ctx.getp2(7))))
                            {
                                int lweight = 50+50*ctx.maos.size(),mweight = 50 + 50*ctx.lies.size();
                                if(ctx.rlsl || ctx.claimedRoleaskday[3] != 0) lweight = 0;
                                if(ctx.rlsm || ctx.getCat() == 0 || ctx.claimedRoleaskday[5] != 0) mweight = 0;
                                if(lweight + mweight == 0) ctx.nonHumanPlan[target] = 0;
                                else
                                {
                                    ctx.nonHumanPlan[target] = 3 + 2 * GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(lweight,mweight)));
                                    if(ctx.nonHumanPlan[target] == 5)
                                    {
                                        response.add(new IntPair(target,5));havecatco = true;ctx.rlsm = true;
                                    }
                                    else ctx.rlsl = true;
                                }
                            }
                            else
                                ctx.nonHumanPlan[target] = 0;
                            break;
                        default:
                            if(GameLogicUtils.probabilityJudge(probabilityCalculator.maolieco(zhi-7,1,ctx.getp1(zhi),ctx.getp2(zhi))))
                            {
                                int lweight = 50+50*ctx.maos.size(),mweight = 50 + 50*ctx.lies.size();
                                if(ctx.claimedRoleaskday[3] != 0) lweight = 0;
                                if(ctx.getCat() == 0 || ctx.claimedRoleaskday[5] != 0) mweight = 0;
                                if(lweight + mweight == 0) ctx.nonHumanPlan[target] = 0;
                                else
                                {
                                    ctx.nonHumanPlan[target] = 3 + 2 * GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(lweight,mweight)));
                                    if(ctx.nonHumanPlan[target] == 5)
                                    {
                                        response.add(new IntPair(target,5));havecatco = true;
                                    }
                                }
                            }
                            break;
                    }
                }
            }
        }
        if(havecatco)
            coManager.askCoByRole(Role.mao);
        // 4,共有者相关逻辑
        gylogic.run();
        deliverEvents.run();
    }
}