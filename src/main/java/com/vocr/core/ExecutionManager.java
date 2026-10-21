package com.vocr.core;

import com.vocr.logging.DebugLogger;

import com.vocr.domain.CharacterEnglishName;
import com.vocr.domain.Event;
import com.vocr.domain.EventName;
import com.vocr.domain.GameResult;
import com.vocr.domain.whyDie;

import com.vocr.config.GameConstants;
import com.vocr.config.ProbabilityCalculator;
import java.util.List;

class ExecutionManager
{
    private final GameContext ctx;
    private final SuspicionSystem suspicion;
    private final COManager coManager;
    private final ProbabilityCalculator probabilityCalculator;
    private final DayVoteCoordinator dayVoteCoordinator;
    private final GameEndChecker gameEndChecker;
    private final ResultEventGenerator eventGenerator;
    private final Runnable deliverEvents;
    private final java.util.function.BiConsumer<Integer, whyDie> dieaux;
    private final Runnable nightaction;
    private final GameRecorder recorder;

    public ExecutionManager(GameModule module)
    {
        this.ctx = module.getCtx();
        this.suspicion = module.getSuspicion();
        this.coManager = module.getCoManager();
        this.probabilityCalculator = module.getProbabilityCalculator();
        this.dayVoteCoordinator = new DayVoteCoordinator(ctx, module.getVoteSelector(), module.getRecorder());
        this.gameEndChecker = module.getGameEndChecker();
        this.eventGenerator = new ResultEventGenerator(ctx);
        this.deliverEvents = module.getDeliverEvents();
        this.dieaux = module.getDieaux();
        this.nightaction = module.getNightaction();
        this.recorder = module.getRecorder();
    }

    public boolean execute(int dailyVotingRule, List<Integer> chuxingList, boolean huibi)
    {
        int n = ctx.getPlayerSum();
        int gd = ctx.getGameDay();
        
        dayVoteCoordinator.logSkillSchedule(n, gd);
        
        ctx.clearPendingEvents();
        if(dailyVotingRule == 2) handleAvoidanceCO(chuxingList, huibi);
        if(ctx.hasPendingEvents())
        {
            deliverEvents.run();
            return false;
        }
        
        int topnum = dayVoteCoordinator.conductVoting(n, gd, dailyVotingRule, chuxingList);
        
        deliverEvents.run();
        processExecution(n, gd, topnum);
        
        deliverEvents.run();
        return checkAndHandleGameEnd(gd);
    }
    
    private void processExecution(int n, int gd, int topnum)
    {
        dieaux.accept(topnum,whyDie.chuxing);
        if(topnum == ctx.getCat())
        {
            if(ctx.getClaimedRole(topnum) != 5)
            {
                ctx.revealClaim(topnum, 5, gd + 1);
            }
            int[] mztarget = new int[n + 1];
            for(int i=1;i<=n;i++)
            {
                if(ctx.isAlive(i))
                    mztarget[i] = 1;
            }
            int mz = suspicion.getOne(mztarget);
            dieaux.accept(mz,whyDie.daymaozhou);
            if(mz == ctx.getFox() && ctx.getDeviant() > 0 && ctx.isAlive(ctx.getDeviant()))
            {
                dieaux.accept(ctx.getDeviant(),whyDie.dayhouzhui);
                ctx.markNonHuman(ctx.getFox());
                ctx.markNonHuman(ctx.getDeviant());
            }
        }
        else if(topnum == ctx.getFox() && ctx.getDeviant() > 0 && ctx.isAlive(ctx.getDeviant()))
        {
            dieaux.accept(ctx.getDeviant(),whyDie.dayhouzhui);
            ctx.markNonHuman(ctx.getFox());
            ctx.markNonHuman(ctx.getDeviant());
        }
    }
    
    private boolean checkAndHandleGameEnd(int gd)
    {
        ctx.setEndResult(gameEndChecker.check());
        if (ctx.getEndResult() != GameResult.NONE) {
            recordReplayDailySnapshot(gd + 1);
            presentGameEnd(ctx.getEndResult());
            recordReplayEnd(ctx.getEndResult().getValue(), gd + 1);
            return true;
        }
        nightaction.run();
        return true;
    }

    private void handleAvoidanceCO(List<Integer> chuxingList, boolean huibi) {
        for(int i=0;i<chuxingList.size();i++)
        {
            int num = chuxingList.get(i);
            if(ctx.isDead(num) ) continue;
            if(ctx.getActualRole(num) == 4)
            {
                eventGenerator.addEvent(EventName.hbg, num);
                ctx.revealClaim(num, 4, ctx.getGameDay());
                continue;
            }
            if(ctx.getClaimedRole(num) != 0) continue;
            if(huibi)
            {
                if(ctx.getActualRole(num) < 7)
                {
                    coManager.processActualCo(num,ctx.getActualRole(num),ctx.getDiebody());
                    continue;
                }
                NonHumanPlan plan = ctx.getNonHumanPlan(num);
                if(plan.isClaimPlan() && ctx.getClaimedRoleAskDay(plan.claimedRole()) == 0)
                {
                    coManager.processActualCo(num, plan.claimedRole(), ctx.getDiebody());
                    continue;
                }
                if(ctx.getClaimedRoleAskDay(3) == 0  || ctx.getClaimedRoleAskDay(5) == 0)
                    switch (ctx.getActualRole(num))
                    {
                        case 7:
                            if (ctx.getRandom().probabilityJudge(probabilityCalculator.maolieco(0, 0, coManager.getp1(7), coManager.getp2(7))))
                                coManager.processMaoLieCo(num, true, ctx.getDiebody());
                            break;
                        default:
                            if (ctx.getRandom().probabilityJudge(probabilityCalculator.maolieco
                                    (ctx.getActualRole(num) - 7, 0, coManager.getp1(ctx.getActualRole(num)), coManager.getp2(ctx.getActualRole(num)))))
                                coManager.processMaoLieCo(num, false, ctx.getDiebody());
                            break;
                    }
            }
        }
    }
    void presentGameEnd(GameResult endResult) {
        DebugLogger.log("游戏结束，添加结束事件");
        int[] weight = new int[ctx.getPlayerSum() + 1];
        for (int i = 1; i <= ctx.getPlayerSum(); i++) {
            if (ctx.isDead(i)) continue;
            if (endResult == GameResult.VILLAGE_WIN && ctx.getActualRole(i) < 7) weight[i] = 1;
            else if (endResult == GameResult.WOLF_WIN && ctx.getActualRole(i) == 7) weight[i] = 1;
            else if (endResult == GameResult.FOX_WIN && ctx.getActualRole(i) == 10) weight[i] = 1;
            else weight[i] = -GameConstants.INF;
        }
        CharacterEnglishName player = ctx.getCharacterName(suspicion.getOne(weight));
        switch (endResult) {
            case VILLAGE_WIN:
                ctx.queueEvent(new Event(EventName.crsl, player, null));
                break;
            case WOLF_WIN:
                if ((ctx.getNonHumanLeader() > 0 && ctx.isAlive(ctx.getNonHumanLeader())))
                    ctx.queueEvent(new Event(EventName.krsl, ctx.getCharacterName(ctx.getNonHumanLeader()), null));
                else
                    ctx.queueEvent(new Event(EventName.rlsl, player, null));
                break;
            case FOX_WIN:
                ctx.queueEvent(new Event(EventName.yhsl, player, null));
                break;
        }
        deliverEvents.run();
    }
    
    private void recordReplayEnd(int endResult, int gameDay) {
        try {
            if (recorder != null) {
                recorder.endGame(endResult, gameDay);
                DebugLogger.info("[Replay] 游戏结束已记录: result=" + endResult + " day=" + gameDay);
            }
        } catch (Exception e) {
            DebugLogger.warn("[Replay] 记录游戏结束失败: " + e.getMessage());
        }
    }

    public void recordReplayGameEnd(int endResult, int gameDay) {
        recordReplayEnd(endResult, gameDay);
    }
    
    public void recordReplayDailySnapshot(int day) {
        try {
            if (recorder != null) {
                recorder.recordDailySnapshot(day, ctx);
                DebugLogger.info("[Replay] 每日快照已记录: day=" + day);
            }
        } catch (Exception e) {
            DebugLogger.warn("[Replay] 记录每日快照失败: " + e.getMessage());
        }
    }
}
