package com.vocr.core;

import com.vocr.logging.DebugLogger;

import com.vocr.domain.Event;
import com.vocr.domain.GameContextView;
import com.vocr.domain.GameResult;
import com.vocr.domain.peiyi;
import com.vocr.domain.Role;
import com.vocr.domain.whyDie;

import com.vocr.config.GameConstants;
import com.vocr.config.PathConfig;
import com.vocr.config.ProbabilityCalculator;
import com.vocr.history.GameRecordManager;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class MainLogic implements MainLogicInterface
{
    private GameStatus gs; // 仅用于初始化，运行阶段应通过 ctx 访问状态
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
    private GameRecordManager gameRecordManager;
    private final Consumer<Event> eventSink;

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
    /** 允许测试或其他前端注入事件接收器，避免核心初始化过程依赖Swing单例。 */
    public MainLogic(Consumer<Event> eventSink)
    {
        this(eventSink, null);
    }

    /** 正式运行时显式注入事件接收器与战绩管理器。 */
    public MainLogic(Consumer<Event> eventSink, GameRecordManager gameRecordManager)
    {
        this.eventSink = Objects.requireNonNull(eventSink, "事件接收器不能为空");
        this.gameRecordManager = gameRecordManager;
    }
    public GameContextView getGameContext()
    {//提供给UI类，封装后的游戏状态只读访问
        return ctx;
    }
    public GameRecorder getRecorder()
    {//提供给Replay系统，获取当前对局的录制器
        return recorder;
    }
    // 接口要求的单参数版本
    public GameStatus start(peiyi p)
    {
        if (gameRecordManager == null) {
            throw new IllegalStateException("未配置战绩管理器");
        }
        return start(p, gameRecordManager);
    }
    public GameStatus start(peiyi p, GameRecordManager gameRecordManager)
    {
        //提供给UI类，开始一局游戏
        //参数：配役p
        //返回值：游戏状态
        //函数体中有可能向UI类添加一系列事件
        this.gameRecordManager = Objects.requireNonNull(gameRecordManager, "战绩管理器不能为空");
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
        int nonHumanAggression = ConstNum.randomInt(0, 5);
        ctx = new GameContext(gs, NonHumanConfiguration.from(p, nonHumanAggression));
        suspicion = ctx.getSuspicion();
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
            File configDir = new File(PathConfig.CONFIG_DIR);
            if (!configDir.exists()) {
                configDir.mkdirs();
            }
            probabilityCalculator = new ProbabilityCalculator(PathConfig.PROBABILITY_CONFIG);
            if(probabilityCalculator != null)
            {
                DebugLogger.log("概率计算器已初始化");
                probabilityCalculator.printStatistics();
            }
        } catch (Exception e) {
            DebugLogger.error("加载配置文件失败，使用默认值: " + e.getMessage());
            probabilityCalculator = new ProbabilityCalculator();
        }

        coManager = new COManager(ctx, suspicion, resultPresenter, probabilityCalculator, hunterGuarder);

        // Replay系统: 录制器作为本局依赖显式传递给执行层。
        this.recorder = new GameRecorder("replay_" + p.name(), gameRecordManager);

        // 构建统一的依赖容器
        GameModule gameModule = new GameModule.Builder()
                .ctx(ctx)
                .suspicion(suspicion)
                .coManager(coManager)
                .probabilityCalculator(probabilityCalculator)
                .voteSelector(voteSelector)
                .gameEndChecker(gameEndChecker)
                .resultPresenter(resultPresenter)
                .gylogic(this::gylogic)
                .deliverEvents(this::deliverEvents)
                .dieaux(dielogicCarrier::dieaux)
                .nightaction(this::nightaction)
                .gameRecordManager(gameRecordManager)
                .recorder(recorder)
                .build();

        nonHumanCoordinator = new NonHumanCoordinator(gameModule);
        dayActionCoordinator = new DayActionCoordinator(gameModule);
        executionManager = new ExecutionManager(gameModule);

        ctx.validateRoleRoster();
        // 得到职业编号
        DebugLogger.log("玩家数量："+ctx.getPlayerSum());
        for(int i=1;i<=ctx.getPlayerSum();i++)
        {
            DebugLogger.log("玩家"+i+"的职业："+ctx.getActualRole(i));
        }
        // 怀疑度初始化已由 SuspicionSystem 构造自动完成
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
        dielogic(wolf[0],wolf[1],zhantarget,0);//死体逻辑，并记录当天夜间死体
        // Replay系统: 第1天在非人初始工作后录制（ctx.getGameDay()已被+1，用-1取回正确的day编号）
        ctx.incrementGameDay();//增加一天时间
        DebugLogger.log("死体逻辑结果：");
        feirenInitial();//非人的初始工作 + 第一天的进行（占灵共co）
        DebugLogger.log("非人的初始工作结果：");
        suspicion.updateTop3SuspectedPlayers(ctx.getZhans(), ctx.getLings(), ctx.getLies());//更新怀疑度
        executionManager.recordReplayDailySnapshot(ctx.getGameDay());
        DebugLogger.log("目前总人数：" + ctx.getPlayerSum());
    }
    private void feirenInitial()
    {
        nonHumanCoordinator.coordinate(ctx.getDiebody());
        deliverEvents();
    }

    private void nightaction()//夜间行动函数
    {
        //1,真占占卜逻辑
        int zhantarget = zhenzhan(ctx.getActualRoleIndex(1));
        //2,得到真灵能技能结果
        int num = getDiePlayerNum(whyDie.chuxing,ctx.getGameDay());
        int mediumResult = num;
        if(ctx.getActualRole(num) == 7)
            mediumResult += ctx.getPlayerSum();//黑结果
        ctx.recordSkillTarget(ctx.getMedium(), ctx.getGameDay(), mediumResult);
        //3,狼咬逻辑,wolf[0]:主咬狼；wolf[1]：被咬玩家
        int[] wolf = wolfwork();
        //4,猎人工作逻辑
        int lietarget = zhenlie(ctx.getHunter());
        ctx.recordSkillTarget(ctx.getHunter(), ctx.getGameDay(), lietarget);
        //5,死体逻辑，并且返回当天夜间死体
        dielogic(wolf[0],wolf[1],zhantarget,lietarget);
        ctx.setEndResult(judgeend());
        if(ctx.getEndResult() != GameResult.NONE) //游戏结束
        {
            executionManager.recordReplayDailySnapshot(ctx.getGameDay() + 1);
            executionManager.recordReplayGameEnd(ctx.getEndResult().getValue(), ctx.getGameDay());
            executionManager.presentGameEnd(ctx.getEndResult());
            DebugLogger.info("[战绩] 游戏结束，准备更新记录: peiyi=" + ctx.getPeiyi() + "(ordinal=" + ctx.getPeiyi().ordinal() + "), end=" + ctx.getEndResult());
            gameRecordManager.updateRecord(ctx.getPeiyi().ordinal(), ctx.getEndResult().getValue());
            DebugLogger.info("[战绩] 记录更新完成");
            return;//胜负已分
        }
        //6,非人占灵猎编造结果逻辑
        frlying();
        // Replay系统: 记录每日快照（录制下一天的开始状态）
        executionManager.recordReplayDailySnapshot(ctx.getGameDay() + 1);
        //7,增加一天时间
        ctx.incrementGameDay();
        //8，白天起身逻辑
        dayaction();
        //9,更新怀疑度
        suspicion.updateTop3SuspectedPlayers(ctx.getZhans(), ctx.getLings(), ctx.getLies());
    }

    private void frlying()
    {
        fabricator.fabricate(ctx.getDiebody());
    }
    private void dayaction()
    {
        dayActionCoordinator.coordinate(ctx.getDiebody());
    }
    private void dielogic(int wolf, int wolfbite, int zhantarget, int lietarget)
    {
        dielogicCarrier.execute(wolf, wolfbite, zhantarget, lietarget);
    }
    private GameResult judgeend()
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
    private void deliverEvents()//发送当前得到的事件并清空待交付队列
    {
        for(int i=0;i<ctx.getPendingEventCount();i++)
        {
            Event event = ctx.getPendingEvent(i);
            //输出测试信息

            DebugLogger.log("添加事件中");
            DebugLogger.log("成功添加事件，事件信息：" + event.toString());
            //将事件发送给当前前端（正式运行时为UI，测试时为内存事件列表）
            eventSink.accept(event);
        }
        ctx.clearPendingEvents();//清空待交付队列
    }
    public void askCo(List<Integer> askList)
    {
        coManager.askCoByList(askList);
        deliverEvents();
    }
}
