import java.util.ArrayList;
import java.util.List;

public class ExecutionManager
{
    private final GameModule module;
    private final GameContext ctx;
    private final SuspicionSystem suspicion;
    private final COManager coManager;
    private final ProbabilityCalculator probabilityCalculator;
    private final VoteSelector voteSelector;
    private final GameEndChecker gameEndChecker;
    private final ResultEventGenerator eventGenerator;
    private final Runnable gylogic;
    private final Runnable deliverEvents;
    private final java.util.function.BiConsumer<Integer, whyDie> dieaux;
    private final Runnable nightaction;
    private final GameRecordManager gameRecordManager;

    public ExecutionManager(GameModule module)
    {
        this.module = module;
        this.ctx = module.getCtx();
        this.suspicion = module.getSuspicion();
        this.coManager = module.getCoManager();
        this.probabilityCalculator = module.getProbabilityCalculator();
        this.voteSelector = module.getVoteSelector();
        this.gameEndChecker = module.getGameEndChecker();
        this.eventGenerator = new ResultEventGenerator(ctx);
        this.gylogic = module.getGylogic();
        this.deliverEvents = module.getDeliverEvents();
        this.dieaux = module.getDieaux();
        this.nightaction = module.getNightaction();
        this.gameRecordManager = module.getGameRecordManager();
    }

    public boolean execute(int dailyVotingRule, List<Integer> chuxingList, boolean huibi)
    {
        int n = ctx.getPlayerSum();
        int gd = ctx.getGameDay();
        
        logSkillSchedule(n, gd);
        
        ctx.eventarray.clear();
        if(dailyVotingRule == 2) handleAvoidanceCO(chuxingList, huibi);
        if(ctx.eventarray.size() > 0)
        {
            deliverEvents.run();
            return false;
        }
        
        boolean[] votable = prepareVoting(n, gd, dailyVotingRule, chuxingList);
        
        int topnum = executeVoting(n, gd, votable, dailyVotingRule, chuxingList);
        
        deliverEvents.run();
        processExecution(n, gd, topnum);
        
        deliverEvents.run();
        return checkAndHandleGameEnd(gd);
    }
    
    private void logSkillSchedule(int n, int gd)
    {
        for(int i=0;i<ctx.zhans.size();i++)
        {
            int zhan = ctx.zhans.get(i);
            DebugLogger.log(buildSkillScheduleLog("占卜师候补编号：" + ctx.getClaimedRoleOrder(zhan) + "预告情况：",
                    n, gd, ctx.getClaimedRoleScheduledSkillTargets(zhan)));
        }
        for (int i = 0; i < ctx.lies.size(); i++)
        {
            int lie = ctx.lies.get(i);
            DebugLogger.log(buildSkillScheduleLog("猎人候补编号：" + ctx.getClaimedRoleOrder(lie) + "指定护卫情况：",
                    n, gd, ctx.getClaimedRoleScheduledSkillTargets(lie)));
        }
        DebugLogger.log(buildSkillScheduleLog("潜伏占预告情况：", n, gd, ctx.getHiddenSeerScheduledSkillTargets()));
        DebugLogger.log(buildSkillScheduleLog("潜伏猎人指定护卫情况：", n, gd, ctx.getHiddenHunterScheduledSkillTargets()));
    }

    private String buildSkillScheduleLog(String title, int n, int gd, boolean[][] schedule)
    {
        StringBuilder sb = new StringBuilder(title);
        for(int i=1;i<=n;i++)
            if(schedule[i][gd])
                sb.append(CharacterKanjiName.values()[ctx.getCharacterNumber(i)]).append(" ");
        return sb.toString();
    }
    
    private boolean[] prepareVoting(int n, int gd, int dailyVotingRule, List<Integer> chuxingList)
    {
        boolean[] votable = new boolean[n+1];
        boolean haveVotable = false;
        if(dailyVotingRule == 1)
        {
            for(int i=1;i<=n;i++)
            {
                if(ctx.isAlive(i) && (ctx.getClaimedRole(i) == 6 || ctx.getClaimedRole(i) == 0))
                {
                    votable[i] = true;
                }
            }
            for(int i=0;i<ctx.zhans.size();i++)
            {
                int zhan = ctx.zhans.get(i);
                for(int j=1;j<gd;j++)
                {
                    int target = ctx.getSkillTarget(zhan, j);
                    if(target > 0)
                    {
                        if(ctx.isBlackResult(target)) votable[ctx.getActualTarget(target)] = false;
                        else votable[target] = false;
                    }
                }
            }
            for(int i=1;i<=n;i++)
            {
                if(votable[i])haveVotable = true;
            }
        }
        else if(dailyVotingRule == 2)
        {
            for(int i=0;i<chuxingList.size();i++)
            {
                int num = chuxingList.get(i);
                if(ctx.isAlive(num))
                {
                    votable[num] = true;
                    haveVotable = true;
                }
            }
        }
        else if(!haveVotable)
        {
            for(int i=1;i<=n;i++)
            {
                if(ctx.isAlive(i))
                {
                    votable[i] = true;
                    haveVotable = true;
                }
            }
        }
        return votable;
    }
    
    private int executeVoting(int n, int gd, boolean[] votable, int dailyVotingRule, List<Integer> chuxingList)
    {
        int shokeicnt = 1, topnum = 0;
        while(true)
        {
            topnum = voteSelector.select(shokeicnt,votable);
            if(topnum != 0) break;
            if(shokeicnt < 3)
                shokeicnt++;
        }
        DebugLogger.log("投票结束，一共投票了" + shokeicnt + "轮" + "\n票型：\n" );
        for(int k=1;k<=shokeicnt;k++)
        {
            DebugLogger.log("第" + k + "轮投票结果：");
            int[] beitou = new int[n+1];
            for(int i=1;i<=n;i++)
            {
                beitou[ctx.getVoteTarget(i, gd, k)]++;
            }
            for(int i=1;i<=n;i++)
            {
                if(ctx.isDead(i))continue;
                DebugLogger.log(CharacterKanjiName.values()[ctx.getCharacterNumber(i)]+""+beitou[i]+"票 投票先 "
                        +CharacterKanjiName.values()[ctx.getCharacterNumber(ctx.getVoteTarget(i, gd, k))]);
            }
        }
        
        try {
            MainLogic ml = (MainLogic) Game.getInstance().getMainLogic();
            if (ml != null && ml.getRecorder() != null) {
                ml.getRecorder().recordVoteData(gd, ctx, dailyVotingRule, chuxingList);
                DebugLogger.info("[Replay] 投票数据已记录: day=" + gd + ", rounds=" + shokeicnt);
            }
        } catch (Exception e) {
            DebugLogger.warn("[Replay] 记录投票数据失败: " + e.getMessage());
        }
        
        return topnum;
    }
    
    private void processExecution(int n, int gd, int topnum)
    {
        dieaux.accept(topnum,whyDie.chuxing);
        if(topnum == ctx.getCat())
        {
            if(ctx.getClaimedRole(topnum) != 5)
            {
                ctx.setClaimedRole(topnum, 5);
                ctx.setComingOutDay(topnum, gd + 1);
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
            DebugLogger.info("[战绩] 游戏结束(ExecutionManager): peiyi=" + ctx.getPeiyi() + ", end=" + ctx.getEndResult());
            gameRecordManager.updateRecord(ctx.getPeiyi().ordinal(), ctx.getEndResult().getValue());
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
                ctx.setClaimedRole(num, 4);
                continue;
            }
            if(ctx.getClaimedRole(num) != 0) continue;
            if(huibi)
            {
                if(ctx.getActualRole(num) < 7)
                {
                    coManager.processActualCo(num,ctx.getActualRole(num),ctx.diebody);
                    continue;
                }
                if(ctx.nonHumanPlan[num] != 0 && ctx.nonHumanPlan[num] != 4 && ctx.claimedRoleaskday[ctx.nonHumanPlan[num]] == 0)
                {
                    if(ctx.claimedRoleaskday[ctx.nonHumanPlan[num]] == 0)
                        coManager.processActualCo(num,ctx.nonHumanPlan[num],ctx.diebody);
                    else
                        ctx.nonHumanPlan[num] = 0;
                    continue;
                }
                if(ctx.claimedRoleaskday[3] == 0  || ctx.claimedRoleaskday[5] == 0)
                    switch (ctx.getActualRole(num))
                    {
                        case 7:
                            if (GameLogicUtils.probabilityJudge(probabilityCalculator.maolieco(0, 0, coManager.getp1(7), coManager.getp2(7))))
                                coManager.processMaoLieCo(num, true, ctx.diebody);
                            break;
                        default:
                            if (GameLogicUtils.probabilityJudge(probabilityCalculator.maolieco
                                    (ctx.getActualRole(num) - 7, 0, coManager.getp1(ctx.getActualRole(num)), coManager.getp2(ctx.getActualRole(num)))))
                                coManager.processMaoLieCo(num, false, ctx.diebody);
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
                ctx.eventarray.add(new Event(EventName.crsl, player, null));
                break;
            case WOLF_WIN:
                if ((ctx.getNonHumanLeader() > 0 && ctx.isAlive(ctx.getNonHumanLeader())))
                    ctx.eventarray.add(new Event(EventName.krsl, ctx.getCharacterName(ctx.getNonHumanLeader()), null));
                else
                    ctx.eventarray.add(new Event(EventName.rlsl, player, null));
                break;
            case FOX_WIN:
                ctx.eventarray.add(new Event(EventName.yhsl, player, null));
                break;
        }
        deliverEvents.run();
    }
    
    private void recordReplayEnd(int endResult, int gameDay) {
        try {
            MainLogic mainLogic = (MainLogic) Game.getInstance().getMainLogic();
            if (mainLogic != null && mainLogic.getRecorder() != null) {
                mainLogic.getRecorder().endGame(endResult, gameDay);
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
            MainLogic mainLogic = (MainLogic) Game.getInstance().getMainLogic();
            if (mainLogic != null && mainLogic.getRecorder() != null) {
                mainLogic.getRecorder().recordDailySnapshot(day, ctx);
                DebugLogger.info("[Replay] 每日快照已记录: day=" + day);
            }
        } catch (Exception e) {
            DebugLogger.warn("[Replay] 记录每日快照失败: " + e.getMessage());
        }
    }
}