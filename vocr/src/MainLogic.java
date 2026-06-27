import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainLogic implements MainLogicInterface
{
    GameStatus gs;
    SuspicionSystem suspicion;//怀疑度子系统
    GameContext ctx;//游戏上下文（Phase 2: 封装 GameStatus + 核心数组）
    private ProbabilityCalculator probabilityCalculator;
    private SeerDiviner seerDiviner;
    private HunterGuarder hunterGuarder;
    private WolfBiter wolfBiter;
    private VoteSelector voteSelector;
    private GameEndChecker gameEndChecker;
    private ResultPresenter resultPresenter;
    private SharedExposer sharedExposer;
    private DielogicCarrier dielogicCarrier;
    private Fabricator fabricator;
    private COManager coManager;
    private NonHumanCoordinator nonHumanCoordinator;
    private DayActionCoordinator dayActionCoordinator;
    private ExecutionManager executionManager;
    private GameRecorder recorder;

    /*
     * 参数取值表
     * zhi 0人狼 1狂人 2狂信 3妖狐 4背德
     * situation 0被指定 1接黒 2被询问co 3询问职业 4共有全死co猫 5猫村双死co猫 6平和co猎
     * p1 0完全怂狼（多指定） 1单狼上职（三指定） 2双狼上职（双指定） 3三狼上职（单指定）
     * p2 0孤狼剩余（游戏前期） 1双狼剩余（游戏前中期） 2三狼剩余（游戏中后期） 3四狼俱在（游戏后期）
     * */
    //任何时刻，某一数值在GameConstants.MAXN-GameConstants.INFJ之间时，应该被收束到GameConstants.MAXN。某一数值在GameConstants.INFJ-GameConstants.INF时，应该被收束到GameConstants.INF
    //GameConstants.INF 机制最大值
    //-GameConstants.INF 机制最小值
    //0-GameConstants.MAXN：正常数值
    //GameConstants.MAXN:不涉及机制的最大值。
    //GameConstants.INFJ-GameConstants.INF:均被判定为设计机制的最大值
    //若涉及到游戏结束，则夜间事件都不会显示。在判断游戏不结束之后，将其中的事件依次添加到UI类当中。
    MainLogic()
    {

    }
    public GameContextView getGameContext()
    {//提供给UI类，封装后的游戏状态只读访问
        return ctx;
    }
    public GameRecorder getRecorder()
    {//提供给Replay系统，获取当前对局的录制器
        return recorder;
    }
    public GameStatus start(peiyi p)
    {
        //提供给UI类，开始一局游戏
        //参数：配役p
        //返回值：游戏状态
        //函数体中有可能向UI类添加一系列事件
        gs         = new GameStatus();
        gs.startGame(p);
        if (gs.getPlayerSum() <= 0) {
            throw new IllegalStateException("游戏初始化失败: playerSum=" + gs.getPlayerSum());
        }
        for (int i = 1; i <= gs.getPlayerSum(); i++) {
            if (gs.gc[i] == null || gs.gc[i].actualRole == 0) {
                throw new IllegalStateException("游戏初始化失败: 玩家" + i + "角色未分配");
            }
        }
        ctx = new GameContext(gs, suspicion);
        suspicion = new SuspicionSystem(ctx);
        ctx.setSuspicion(suspicion);
        seerDiviner = new SeerDiviner(ctx, suspicion);
        hunterGuarder = new HunterGuarder(ctx, suspicion);
        wolfBiter = new WolfBiter(ctx, suspicion);
        voteSelector = new VoteSelector(ctx, suspicion);
        gameEndChecker = new GameEndChecker(ctx);
        resultPresenter = new ResultPresenter(ctx, suspicion);
        sharedExposer = new SharedExposer(ctx, suspicion);
        dielogicCarrier = new DielogicCarrier(ctx, suspicion, this::deliverEvents);
        fabricator = new Fabricator(ctx, suspicion, seerDiviner, hunterGuarder);
        // 简化概率计算器初始化
        try {
            File configDir = new File("config");
            if (!configDir.exists()) {
                configDir.mkdirs();
            }
            String configPath = "config/probability.txt";
            probabilityCalculator = new ProbabilityCalculator(configPath);
            if(probabilityCalculator != null)
            {
                DebugLogger.log("概率计算器已初始化");
                probabilityCalculator.printStatistics();
            }
        } catch (Exception e) {
            DebugLogger.error("加载配置文件失败，使用默认值: " + e.getMessage());
            probabilityCalculator = new ProbabilityCalculator();
        }

        coManager = new COManager(ctx, suspicion, resultPresenter, probabilityCalculator);
        nonHumanCoordinator = new NonHumanCoordinator(ctx, suspicion, coManager, this::gylogic, this::deliverEvents);
        dayActionCoordinator = new DayActionCoordinator(ctx, suspicion, coManager, resultPresenter, probabilityCalculator, this::gylogic, this::deliverEvents);
        executionManager = new ExecutionManager(ctx, suspicion, coManager, probabilityCalculator, voteSelector, gameEndChecker, this::gylogic, this::deliverEvents, dielogicCarrier::dieaux, this::nightaction);

        // Phase 3: 统一数组引用 —— 所有数组/集合字段统一在 GameContext 中管理
        ctx.initFromPeiyi(p);
        ctx.kz = ConstNum.randomInt(0,5);//初始化非人狂躁值
        // 得到职业编号
        int nowrl = 1;
        int nowgy = 1;
        DebugLogger.log("玩家数量："+gs.getPlayerSum());
        for(int i=1;i<=gs.getPlayerSum();i++)
        {
            DebugLogger.log("玩家"+i+"的职业："+ctx.getActualRole(i));
            ctx.setActualRoleIndex(ctx.getActualRole(i), i);
            if(ctx.getActualRole(i) == 7)
                ctx.rlindex[nowrl++] = i;
            if(ctx.getActualRole(i) == 4)
                ctx.gyindex[nowgy++] = i;
        }
        // 怀疑度初始化已由 SuspicionSystem 构造自动完成
        // Replay系统: 初始化录制器
        this.recorder = new GameRecorder("replay_" + p.name());
        this.recorder.startGame(p, 0L, gs.getPlayerSum(), gs);
        DebugLogger.info("[Replay] 录制器已初始化: " + p.name());
        //初始职业工作
        initialWork();//首夜和第一个白天开始的行动
        return gs;//返回游戏状态
    }
    private void initialWork()//游戏开始时夜晚的逻辑（1日夜）
    {
        //初始职业工作
        //参数：无
        //无返回值，在函数体运行时添加对应的事件
        //工作：占卜师占卜人，非人确定自己预定的工作，以及非人对应的生成的占文猎文。
        DebugLogger.log("占卜师占卜人");
        int zhantarget = zhenzhan(ctx.getActualRoleIndex(1));//真占占卜逻辑
        DebugLogger.log("占卜师占卜人结果："+zhantarget);
        int[] wolf = wolfwork();//狼咬逻辑,wolf[0]:主咬狼；wolf[1]：被咬玩家
        DebugLogger.log("狼咬逻辑："+wolf[0]+"咬"+wolf[1]);
        //初日死者不能是妖狐10或猫又5
        while(ctx.getActualRole(wolf[1]) %5 == 0)
            wolf = wolfwork();//重新选择狼咬逻辑
        DebugLogger.log("死体逻辑：");
        ctx.diebody.clear();
        ctx.diebody.addAll(dielogic(wolf[0],wolf[1],zhantarget,0));//死体逻辑，并且返回当天夜间死体
        // Replay系统: 第1天在非人初始工作后录制（gs.gameDay已被+1，用-1取回正确的day编号）
        gs.gameDay++;//增加一天时间
        DebugLogger.log("死体逻辑结果：");
        feirenInitial();//非人的初始工作 + 第一天的进行（占灵共co）
        DebugLogger.log("非人的初始工作结果：");
        suspicion.updateTop3SuspectedPlayers(ctx.zhans, ctx.lings, ctx.lies);//更新怀疑度
        executionManager.recordReplayDailySnapshot(gs.gameDay);
        DebugLogger.log("目前总人数：" + gs.getPlayerSum());
    }
    private void feirenInitial()
    {
        nonHumanCoordinator.coordinate(ctx.diebody);
        deliverEvents();
    }

    private void nightaction()//夜间行动函数
    {
        //1,真占占卜逻辑
        int zhantarget = zhenzhan(ctx.getActualRoleIndex(1));
        //2,得到真灵能技能结果
        int num = getDiePlayerNum(whyDie.chuxing,gs.gameDay);
        int mediumResult = num;
        if(ctx.getActualRole(num) == 7)
            mediumResult += gs.getPlayerSum();//黑结果
        ctx.setSkillTarget(ctx.getMedium(), gs.gameDay, mediumResult);
        //3,狼咬逻辑,wolf[0]:主咬狼；wolf[1]：被咬玩家
        int[] wolf = wolfwork();
        //4,猎人工作逻辑
        int lietarget = zhenlie(ctx.getHunter());
        //5,死体逻辑，并且返回当天夜间死体
        ctx.diebody.clear();
        ctx.diebody.addAll(dielogic(wolf[0],wolf[1],zhantarget,lietarget));
        gs.end = judgeend();
        if(gs.end != 0) //游戏结束
        {
            executionManager.recordReplayDailySnapshot(gs.gameDay + 1);
            executionManager.recordReplayGameEnd(gs.end, gs.gameDay);
            executionManager.presentGameEnd(gs.end);
            DebugLogger.info("[战绩] 游戏结束，准备更新记录: peiyi=" + gs.p + "(ordinal=" + gs.p.ordinal() + "), end=" + gs.end);
            GameRecordManager.getInstance().updateRecord(gs.p.ordinal(), gs.end);
            DebugLogger.info("[战绩] 记录更新完成");
            return;//胜负已分
        }
        //6,非人占灵猎编造结果逻辑
        frlying();
        // Replay系统: 记录每日快照（录制下一天的开始状态）
        executionManager.recordReplayDailySnapshot(gs.gameDay + 1);
        //7,增加一天时间
        gs.gameDay++;
        //8，白天起身逻辑
        dayaction();
        //9,更新怀疑度
        suspicion.updateTop3SuspectedPlayers(ctx.zhans, ctx.lings, ctx.lies);
    }

    private void frlying()
    {
        fabricator.fabricate(ctx.diebody);
    }
    private void dayaction()
    {
        dayActionCoordinator.coordinate(ctx.diebody);
    }
    private ArrayList<Integer> dielogic(int wolf, int wolfbite, int zhantarget, int lietarget)
    {
        return dielogicCarrier.execute(wolf, wolfbite, zhantarget, lietarget);
    }
    private int judgeend()
    {
        return gameEndChecker.check();
    }
    private void gylogic()
    {
        sharedExposer.expose();
    }

    private int getDiePlayerNum(whyDie why, int day)
    {
        return ctx.getDiePlayerNum(why, day);
    }
    private int[] wolfwork()
    {
        return wolfBiter.decideBite();
    }
    private int zhenlie(int num)
    {
        return hunterGuarder.guard(num);
    }
    private int zhenzhan(int znum)
    {
        return seerDiviner.divine(znum);
    }
    public boolean shokei(int dailyVotingRule, List<Integer> chuxingList,boolean huibi)
    {
        return executionManager.execute(dailyVotingRule, chuxingList, huibi);
    }
    public void askCo(Role aclaimedRole)
    {
        coManager.askCoByRole(aclaimedRole);
        deliverEvents();
    }
    private void deliverEvents()//发送当前得到的事件并清空事件数组
    {
        for(int i=0;i<ctx.eventarray.size();i++)
        {
            //输出测试信息
            
            {
                DebugLogger.log("添加事件中");
                if(ctx.eventarray.get(i) == null)
                    DebugLogger.log("事件为空");
                else
                {
                    DebugLogger.log("成功添加事件，事件信息：" + ctx.eventarray.get(i).toString());
                }
            }
            //将事件添加到UI类当中
            Game.getInstance().getUI().addEvent(ctx.eventarray.get(i));
        }
        ctx.eventarray.clear();//清空事件数组
    }
    public void askCo(List<Integer> askList)
    {
        coManager.askCoByList(askList);
        deliverEvents();
    }
}