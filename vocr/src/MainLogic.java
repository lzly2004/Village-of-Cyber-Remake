import java.io.File;
import java.util.*;

class IntPair
{
    final int first;  // 第一个整数
    final int second; // 第二个整数
    public IntPair(int first, int second)
    {
        this.first = first;
        this.second = second;
    }

    // 获取器（游戏场景按需添加setter，若需可变）
    public int getFirst() {
        return first;
    }

    public int getSecond() {
        return second;
    }

    // 可选：重写toString，方便调试
    @Override
    public String toString() {
        return "(" + first + ", " + second + ")";
    }
}
class logicTools    //逻辑类中的工具方法集合
{
    static boolean istest;
    logicTools()
    {
        istest = true;
    }
    public static void log(String message)
    {
        if(istest)
        {
            System.out.println(message);
        }
    }
    public static int min(int x,int y)
    {
        if(x < y) return x;
        return y;
    }
    public static boolean probabilityJudge(int p)//判断概率为p%的事件是否发生：依据真实概率
    {
        //判断概率为p%的事件是否发生：依据真实概率
        //p为任意整数
        if(p >= 100) return true;//概率大于等于1，必定发生
        if(p <= 0) return false;//概率小于等于0，必定不发生
        int p0 = ConstNum.randomInt(1,100);
        if(p0 <= p) return true;
        else return false;
    }
}
public class MainLogic implements MainLogicInterface
{
    boolean istest;//当前是否在游戏测试阶段。控制输出控制台信息
    GameStatus gs;
    boolean isDoubleDeathOccurred[];//当前日期的夜间是否出现过双死
    boolean rlsl,rlsm;//当前是否有人狼上猎，是否有人狼上猫
    int gongindex;//潜伏共有者的下标
    int exposureProgress;//暴露进度
    int kz;//非人狂躁值
    int kyojin;//本局游戏是否是狂人 是1否0 9-k就是狂人狂信对应的职业编号
    int nonHumanPlan[];//非人计划 0潜伏 1上占 2上灵 3上猎 4未定 5上猫
    int ybzw[];//非人预备的（白）占文
    int zw[];//非人的另一份（正式）占文
    int actualRoleindex[];//单人职业的编号，包括占灵猫猎狐背狂 eg【3】=4：编号为4的玩家是职业3（猎人）
    int rlindex[];//人狼的编号 rlindex[2] = 7:第二位人狼的数组下标为7
    int gyindex[];//共有的编号
    int lasySuspicionValue[];//怀疑度懒惰数组，用于临时保存所有人对某个玩家的统一怀疑度增减
    int claimedRoleaskday[];//每个村职被整体询问的日期，默认值为0
    int lined[][];  //占灵连线情况 1连线
    int initialWolfCount,initialNonHumanCount;//该村的初始人狼数量，非人数量
    ArrayList<Integer> zhans,lings,lies,maos,diebody;//占领猎猫候补数组,当天的死体数组
    int claimedRoleorder[];//当前职业位次数组，0表示未获取
    public static int INF = 999;//理论最大数值和理论最小数值的相反数
    public static int INFJ = 500;//判断是否是理论最大数值的值
    public static int MAXN = 100;//所有合法值的最大值
    ArrayList<IntPair> response;//反应数组
    private ProbabilityCalculator probabilityCalculator;
    /*
     * 参数取值表
     * zhi 0人狼 1狂人 2狂信 3妖狐 4背德
     * situation 0被指定 1接黒 2被询问co 3询问职业 4共有全死co猫 5猫村双死co猫 6平和co猎
     * p1 0完全怂狼（多指定） 1单狼上职（三指定） 2双狼上职（双指定） 3三狼上职（单指定）
     * p2 0孤狼剩余（游戏前期） 1双狼剩余（游戏前中期） 2三狼剩余（游戏中后期） 3四狼俱在（游戏后期）
     * */
    //任何时刻，某一数值在MAXN-INFJ之间时，应该被收束到MAXN。某一数值在INFJ-INF时，应该被收束到INF
    //INF 机制最大值
    //-INF 机制最小值
    //0-MAXN：正常数值
    //MAXN:不涉及机制的最大值。
    //INFJ-INF:均被判定为设计机制的最大值
    ArrayList<Event> eventarray;//当前待处理的事件数组
    //若涉及到游戏结束，则夜间事件都不会显示。在判断游戏不结束之后，将其中的事件依次添加到UI类当中。
    MainLogic()
    {

    }
    boolean isDayDie(whyDie why)
    {
        if(why == whyDie.chuxing || why == whyDie.dayhouzhui || why == whyDie.daymaozhou)
            return true;
        return false;
    }
    public GameStatus getGameStatus()
    {//提供给UI类，让UI类得到当前的游戏状态
        return gs;
    }
    public GameStatus start(peiyi p)
    {
        //提供给UI类，开始一局游戏
        //参数：配役p
        //返回值：游戏状态
        //函数体中有可能向UI类添加一系列事件
        gs         = new GameStatus();
        istest = true;//默认在测试阶段
        gs.startGame(p);
        isDoubleDeathOccurred    = new boolean[ConstNum.N+1];
        ybzw       = new int[gs.getPlayerSum()+1];
        nonHumanPlan      = new int[gs.getPlayerSum()+1];
        claimedRoleorder = new int[12];
        zw         = new int[gs.getPlayerSum()*2+1];//有黑色占文
        gyindex    = new int[3];
        rlindex    = new int[gs.getPlayerSum()+1];//人狼的编号 rlindex[2] = 7:第二位人狼的数组下标为7
        lasySuspicionValue = new int[gs.getPlayerSum()+1];//怀疑度更新懒惰数组
        actualRoleindex = new int[Role.values().length + 1];//按照Role的长度设置数组长
        claimedRoleaskday = new int[12];//每个村职被整体询问的日期。初值0
        lined = new int[gs.getPlayerSum()+1][gs.getPlayerSum()+1];
        zhans = new ArrayList<Integer>();
        lings = new ArrayList<Integer>();
        lies = new ArrayList<Integer>();
        maos = new ArrayList<Integer>();//占领猎猫候补数组初始化
        // 简化概率计算器初始化
        try {
            // 创建配置目录（如果不存在）
            File configDir = new File("config");
            if (!configDir.exists()) {
                configDir.mkdirs();
            }

            String configPath = "config/probability.txt";
            probabilityCalculator = new ProbabilityCalculator(configPath);
            if(istest)
             {
                logicTools.log("概率计算器已初始化");
                probabilityCalculator.printStatistics();
            }
        } catch (Exception e) {
            // 如果配置文件加载失败，使用默认值
            if(istest)
                System.err.println("加载配置文件失败，使用默认值: " + e.getMessage());
            probabilityCalculator = new ProbabilityCalculator();
        }
        response = new ArrayList<IntPair>();
        eventarray = new ArrayList<Event>();
        exposureProgress = 0;
        gongindex = -1;
        kyojin = 1;
        kz = ConstNum.randomInt(0,5);//初始化非人狂躁值

        switch(p)//根据配役设置初始人狼数量
        {
            case peiyi.jianyi :initialWolfCount = 2;initialNonHumanCount = 3;break;
            case peiyi.tongchang:initialWolfCount = 3;initialNonHumanCount = 4;break;
            case peiyi.yaoohu:initialWolfCount = 3;initialNonHumanCount = 5;break;
            case peiyi.kuangxin:initialWolfCount = 3;initialNonHumanCount = 5;kyojin = 0;break;
            case peiyi.beide:initialWolfCount = 3;initialNonHumanCount = 6;break;
            case peiyi.maoyou:initialWolfCount = 4;initialNonHumanCount = 6;break;
            case peiyi.daxing:initialWolfCount = 4;initialNonHumanCount = 7;kyojin = 0;break;
        }
        //得到职业编号
        int nowrl = 1;
        int nowgy = 1;
        Scanner scanner = new Scanner(System.in);
        logicTools.log("玩家数量："+gs.getPlayerSum());
        for(int i=1;i<=gs.getPlayerSum();i++)
        {
            logicTools.log("玩家"+i+"的职业："+gs.gc[i].actualRole);
            actualRoleindex[gs.gc[i].actualRole] = i;//更新对应役职的玩家下标，对于唯一的役职，可以保存对应的玩家编号，复数的役职只能保存最后一位玩家的编号
            //人狼逻辑
            if(gs.gc[i].actualRole == 7)
                rlindex[nowrl++] = i;//专门存储人狼的数组下标
            //共有逻辑
            if(gs.gc[i].actualRole == 4)
                gyindex[nowgy++] = i;//专门存储人狼的数组下标
        }

        //设置怀疑度的初始值为50
        for(int i=1;i<=gs.getPlayerSum();i++)
        {
            for(int j=1;j<=gs.getPlayerSum();j++)
            {
                if(i == j)
                    gs.gc[i].suspicionValue[j] = -INF;
                else
                    gs.gc[i].suspicionValue[j] = 50;
            }
        }
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
        logicTools.log("占卜师占卜人");
        int zhantarget = zhenzhan(actualRoleindex[1]);//真占占卜逻辑
        logicTools.log("占卜师占卜人结果："+zhantarget);
        int wolf[] = wolfwork();//狼咬逻辑,wolf[0]:主咬狼；wolf[1]：被咬玩家
        logicTools.log("狼咬逻辑："+wolf[0]+"咬"+wolf[1]);
        //初日死者不能是妖狐10或猫又5
        while(gs.gc[wolf[1]].actualRole %5 == 0)
            wolf = wolfwork();//重新选择狼咬逻辑
        logicTools.log("死体逻辑：");
        diebody = dielogic(wolf[0],wolf[1],zhantarget,0);//死体逻辑，并且返回当天夜间死体
        gs.gameDay++;//增加一天时间
        logicTools.log("死体逻辑结果：");
        feireninitilal();//非人的初始工作 + 第一天的进行（占灵共co）
        logicTools.log("非人的初始工作结果：");
        updatetop3SuspectedPlayers();//更新怀疑度
        logicTools.log("目前总人数：" + gs.getPlayerSum());
    }
    private void feireninitilal()
    {
        //非人的初始工作
        //决定是否上占上灵，是否co猎人的姿态，准备（白）占文备用
        //参数：当夜死体
        //0,夜间死亡事件添加到事件数组当中->(dielogic当中已经添加，无需处理。)
        //1,所有非人都生成一份预备占文
        logicTools.log("非人的初始工作：所有非人都生成一份预备占文");
        for(int i=1;i<=gs.getPlayerSum();i++)
        {
            if(feiren(gs.gc[i]) == 0) continue;//村侧玩家不生成备用占文
            //调用getOne得到占文目标
            //狼和狂信会排除狼，所有非人会排除自己
            int weight[] = new int[gs.getPlayerSum()+1];//权重数组
            for(int j=1;j<=gs.getPlayerSum();j++)
            {
                if(i == j)
                {
                    weight[j] = -INF;
                    continue;//无法声称占卜自己，设置权重为-INF
                }
                if(gs.gc[i].actualRole != 7 && gs.gc[i].actualRole != 9)
                {
                    //不是人狼或狂信,设置权重为1
                    weight[j] = 1;
                    continue;
                }
                //人狼或狂信上占
                if(gs.gc[j].actualRole != 7) weight[j] = 1;//不是狼队友，权重为1
                else weight[j] = -INF;//是狼队友，权重为-INF
            }
            ybzw[i] = getOne(weight);//得到占卜对象
            logicTools.log("非人的预备占文，非人:"+i+",预备占文:"+ybzw[i]);
        }

        //2,非人决定自己的计划倾向 0潜伏1上占2上灵3上猎4未定5上猫
        //决定了计划之后若上占，生成正式的占文
        logicTools.log("非人的初始工作：非人决定自己的计划倾向");
        //人狼策略
        //初始值
        for(int i=1;i<=initialWolfCount;i++)
        {
            logicTools.log("rlindex[i]:" + rlindex[i]);
            if(gs.gc[rlindex[i]].whyDie != whyDie.NONE) continue;
            nonHumanPlan[rlindex[i]] = 4;//默认是未定计划
        }
        //人狼上占
        logicTools.log("非人的初始工作：人狼上占");
        //双狼上占 y+(x-2)*3 单狼上占 (11-x)*15 怂狼 10-y
        int rlzhan = getEventIndexByProbability(new ArrayList<>(List.of(10-kz, (11-initialWolfCount)*15, kz + (initialWolfCount-2)*3)));
        //获得计划上占人狼的数量
        int rlz = rlzhan;//保存上占人狼数副本
        while(rlzhan != 0)//当前仍有期望上占人狼
        {
            int rl = ConstNum.randomInt(1,initialWolfCount);
            if(gs.gc[rlindex[rl]].whyDie !=  whyDie.NONE || zw[rlindex[rl]] != 0) continue;//若当前人狼已经死亡，或者当前人狼已经预备上占，则重选。
            rlzhan--;//待上占人狼数减一
            nonHumanPlan[rlindex[rl]] = 1;//上占计划
            //生成一份占文
            int op = getEventIndexByProbability(new ArrayList<>(List.of(80-2*kz, 15, 5+kz, kz)));//得到狼占的策略
            // 0 外白 1黒特攻 2围 3身内切
            int weight[] = new int[gs.getPlayerSum()+1];//权重数组
            for(int i=1;i<=gs.getPlayerSum();i++)
            {
                if((op == 1 || op == 2) && gs.gc[i].actualRole == 7 && rlindex[rl] != i)//对狼同伴,不能对自己
                {
                    weight[i] = 1;
                }
                else if((op == 0 || op == 3) && gs.gc[i].actualRole != 7 && ybzw[rlindex[rl]] != i)//对白色玩家,不能写两份一样的占文（不能与备用占文重复）
                {
                    weight[i] = 1;
                }
                else weight[i] = -INF;//非法情况，设为非法值
            }
            int target = getOne(weight);//占卜目标
            if(op == 0 || op == 2 || diebody.contains(target))//白色结果，注意初日死者必须被发白
                zw[rlindex[rl]] = target;
            else                  //黑色结果
                zw[rlindex[rl]] = target + gs.getPlayerSum();
            logicTools.log("人狼上占的占文，人狼："+rl+",占文："+ zw[rlindex[rl]]);
        }
        //人狼上灵
        logicTools.log("非人的初始工作：人狼上灵");
        //根据初始人狼数量以及计划上占人狼数量，给出人狼上灵概率
        int p;
        if(initialWolfCount == 2)
        {
            if(rlz == 0)  p = kz + 30;
            else if(rlz == 1) p = 0;
            else p = 0;//rlz == 2
        }
        else if(initialWolfCount == 3)
        {
            if(rlz == 0) p = kz + 50;
            else if(rlz == 1) p = kz + 30;
            else p = 0;//rlz == 2
        }
        else    //initialWolfCount == 4
        {
            if(rlz == 0) p = kz + 60;
            else if(rlz == 1) p = kz + 30;
            else p = kz + 10;//rlz == 2
        }
        if(logicTools.probabilityJudge(p))//人狼上灵判定成功
        {
            int rl;
            while(true)
            {
                rl = ConstNum.randomInt(1,initialWolfCount);
                if(gs.gc[rlindex[rl]].whyDie != whyDie.NONE || zw[rlindex[rl]] != 0) continue;//当前人狼已死亡或者已经预备上占，则重选
                nonHumanPlan[rlindex[rl]] = 2;//上灵计划
                break;
            }
        }

        //狂人狂信策略
        logicTools.log("非人的初始工作：狂人狂信策略");
        //设置标记
        if(gs.p == peiyi.kuangxin || gs.p == peiyi.daxing) kyojin = 0;//狂信标记 9
        int kop = getEventIndexByProbability(new ArrayList<>(List.of(55-20*kyojin,35+25*kyojin,10-5*kyojin)));//狂信策略分布 0狂占 1狂灵 2潜伏狂
        if(kop == 1)
            nonHumanPlan[actualRoleindex[9-kyojin]] = 2;//狂人狂信的计划:上灵
        else if(kop == 2)
            nonHumanPlan[actualRoleindex[9-kyojin]] = 4;//狂人狂信的计划：潜伏
        else    //狂占或信占
        {
            nonHumanPlan[actualRoleindex[9-kyojin]] = 1;//狂人狂信的计划:上占
            if(kyojin == 1)//狂人
            {
                int target,option = getEventIndexByProbability(new ArrayList<>(List.of(75-kz,25+kz)));//狂占策略分布 0外白 1黑球
                while(true)
                {
                    target = ConstNum.randomInt(1,gs.getPlayerSum());//生成占文对象
                    logicTools.log("狂人占文对象：" + target);
                    if(target == ybzw[actualRoleindex[9-kyojin]] || target == actualRoleindex[9-kyojin]) continue;//占文对象不能是自己，也不能是写过的占文中的对象
                    if(option == 0 || diebody.contains(target))
                        zw[actualRoleindex[9-kyojin]] = target;                    //白色结果，这里简化处理，若目标是初日死者，则强行转为白结果
                    else
                        zw[actualRoleindex[9-kyojin]] = target + gs.getPlayerSum();//黑色结果
                    break;
                }
            }
            else    //狂信
            {
                int target,option = getEventIndexByProbability(new ArrayList<>(List.of(70-2*kz,20,kz+10,kz)));//信占策略分布
                // 0 外白 1黒特攻 2围 3身内切
                while(true)
                {
                    target = ConstNum.randomInt(1,gs.getPlayerSum());//生成占文对象
                    if(target == ybzw[actualRoleindex[9-kyojin]] || target == actualRoleindex[9-kyojin]) continue;//占文对象不能是自己，也不能是写过的占文中的对象
                    if(gs.gc[target].actualRole == 7 && option < 2) continue;//给同伴的选择，但是随机到白色玩家
                    if(gs.gc[target].actualRole != 7 && option > 1) continue;//给白色玩家的选择，但是随机到同伴
                    logicTools.log("狂信占文对象：" + target);
                    if(option == 0 || option == 2 || diebody.contains(target))
                        zw[actualRoleindex[9-kyojin]] = target;                    //白色结果，这里简化处理，若目标是初日死者，则强行转为白结果
                    else
                        zw[actualRoleindex[9-kyojin]] = target + gs.getPlayerSum();//黑色结果
                    logicTools.log("狂信占文：" + zw[actualRoleindex[9-kyojin]]);
                    break;
                }
            }
        }
        //妖狐策略
        logicTools.log("非人的初始工作：妖狐策略");
        if(actualRoleindex[10] > 0 && gs.gc[actualRoleindex[10]].whyDie == whyDie.NONE)//存在妖狐的配役且存活
        {
            int op = getEventIndexByProbability(new ArrayList<>(List.of(5+kz,kz,95-2*kz)));//妖狐策略分布 0狐占 1狐灵 2潜伏狐
            if(op == 1)
                nonHumanPlan[actualRoleindex[10]] = 2;//妖狐的计划:上灵
            else if(op == 2)
                nonHumanPlan[actualRoleindex[10]] = 4;//妖狐的计划：潜伏
            else    //狐占
            {
                nonHumanPlan[actualRoleindex[10]] = 1;//妖狐的计划:上占
                int target, option = getEventIndexByProbability(new ArrayList<>(List.of(90 - kz, 10 + kz)));//狐占策略分布 0外白 1黑球
                while (true)
                {
                    target = ConstNum.randomInt(1, gs.getPlayerSum());//生成占文对象
                    if (target == ybzw[actualRoleindex[10]] || target == actualRoleindex[10]) continue;//占文对象不能是自己，也不能是写过的占文中的对象
                    logicTools.log("妖狐占文对象：" + target);
                    if (option == 0 || diebody.contains(target))
                        zw[actualRoleindex[10]] = target;                    //白色结果，这里简化处理，若目标是初日死者，则强行转为白结果
                    else
                        zw[actualRoleindex[10]] = target + gs.getPlayerSum();//黑色结果
                    logicTools.log("妖狐占文：" + zw[actualRoleindex[10]]);
                    break;
                }
            }
        }
        //背德策略
        logicTools.log("非人的初始工作：背德策略");
        if(actualRoleindex[11] > 0 && gs.gc[actualRoleindex[11]].whyDie == whyDie.NONE)//配役存在背德且存活
        {
            int bop = getEventIndexByProbability(new ArrayList<>(List.of(50+kz,5,45-kz)));//背德策略分布 0背占 1背灵 2潜伏背
            if(bop == 1)
                nonHumanPlan[actualRoleindex[11]] = 2;//背德的计划:上灵
            else if(bop == 2)
                nonHumanPlan[actualRoleindex[11]] = 4;//背德的计划：潜伏
            else    //背占
            {
                logicTools.log("背德开始选择占文");
                nonHumanPlan[actualRoleindex[11]] = 1;//背德的计划:上占
                int target, option = getEventIndexByProbability(new ArrayList<>(List.of(60 - kz, 20, 20+kz, 0)));//背占策略分布 0外白 1黑特攻 2围  3逆围狐
                if(option == 2)
                {
                    zw[actualRoleindex[11]] = actualRoleindex[10];
                    logicTools.log("狐狸白球");
                }
                else
                    while (true)
                    {
                        target = ConstNum.randomInt(1, gs.getPlayerSum());//生成占文对象
                        logicTools.log("背德选择占文:"+target);
                        if (target == ybzw[actualRoleindex[11]] || target == actualRoleindex[11])
                            continue;//占文对象不能是自己，也不能是写过的占文中的对象
                        if (gs.gc[target].actualRole == 10) continue;//给白色玩家的选择，但是随机到狐同伴
                        if (option == 0 || diebody.contains(target))
                            zw[actualRoleindex[11]] = target;                    //白色结果，这里简化处理，若目标是初日死者，则强行转为白结果
                        else
                            zw[actualRoleindex[11]] = target + gs.getPlayerSum();//黑色结果
                        logicTools.log("背德占文:" + zw[actualRoleindex[11]]);
                        break;
                    }
            }
        }
        //白天临时改变策略,以及占灵共的行动
            logicTools.log("白天临时改变策略,以及占灵共的行动");
        //1,咒杀判定
        if(diebody.size() > 1)
        {
            //明确咒杀
                logicTools.log("非人的初始工作：明确咒杀");
            //2，占卜co
            //人狼上占
                logicTools.log("非人的初始工作：人狼上占");
            boolean haverl = false;//是否有咒杀对应的上占人狼
            //遍历每个人狼，判断是否有计划上占并且能够咒杀对应的人狼
            for(int i=1;i<=initialWolfCount;i++)
            {
                if(nonHumanPlan[rlindex[i]] != 1) continue;//只遍历计划上占人狼
                if(diebody.contains(zw[rlindex[i]]))//若计划上占人狼的两篇占文当中含有咒杀对应占文
                {
                    haverl = true;//存在对应人狼
                    zhans.add(rlindex[i]);//添加到占候补数组
                    gs.gc[rlindex[i]].skillTarget[1] = zw[rlindex[i]];//将占文添加到技能数组
                    continue;
                }
                if(diebody.contains(ybzw[rlindex[i]]))//若计划上占人狼的两篇占文当中含有咒杀对应占文
                {
                    haverl = true;//存在对应人狼
                    zhans.add(rlindex[i]);//添加到占候补数组
                    gs.gc[rlindex[i]].skillTarget[1] = ybzw[rlindex[i]];//将占文添加到技能数组
                    continue;
                }
                nonHumanPlan[rlindex[i]] = 0;//修改计划为死心潜伏
            }
            if(!haverl)
            {
                //若不存在计划上占人狼的对应，则所有能够对应的人狼都去上占
                for(int i=1;i<=initialWolfCount;i++)
                {
                    if(nonHumanPlan[rlindex[i]] == 1) continue;//只遍历没有计划上占的人狼
                    if(diebody.contains(ybzw[rlindex[i]]))//若计划上占人狼的两篇占文当中含有咒杀对应占文
                    {
                        haverl = true;//存在对应人狼
                        zhans.add(rlindex[i]);//添加到占候补数组
                        gs.gc[rlindex[i]].skillTarget[1] = ybzw[rlindex[i]];//将占文添加到技能数组
                        nonHumanPlan[rlindex[i]] = 1;//修改计划：上占
                    }
                }
            }

            //狂人狂信上占
                logicTools.log("非人的初始工作：狂人狂信上占");
            if(gs.gc[actualRoleindex[9-kyojin]].whyDie == whyDie.NONE &&  diebody.contains(ybzw[actualRoleindex[9-kyojin]]))//需要保证狂人狂信存活，并且有对应
            {
                //咒杀对应必定上占
                nonHumanPlan[actualRoleindex[9-kyojin]] = 1;//修改计划：上占
                gs.gc[actualRoleindex[9-kyojin]].skillTarget[1] = ybzw[actualRoleindex[9-kyojin]];//将占文添加到技能数组
                zhans.add(actualRoleindex[9-kyojin]);//添加到占候补数组
            }
            else if(nonHumanPlan[actualRoleindex[9-kyojin]] == 1)
                nonHumanPlan[actualRoleindex[9-kyojin]] = 0;//修改计划为死心潜伏
            //真占co
                logicTools.log("非人的初始工作：真占co");
            if(gs.gc[actualRoleindex[1]].whyDie == whyDie.NONE)//需要保证真占存活（有可能开局遗言咒杀）
                zhans.add(actualRoleindex[1]);
            //打乱占候补数组
            //占文随机排序
            
                logicTools.log("非人的初始工作：占文随机排序");
            zhans = shuffleList(zhans);
            for(int i=0;i<zhans.size();i++)
            {
                eventarray.add(new Event(EventName.zs14,CharacterEnglishName.values()[gs.gc[zhans.get(i)].number],
                        CharacterEnglishName.values()[gs.gc[gs.gc[zhans.get(i)].skillTarget[1]].number]));//添加事件：咒杀
                gs.gc[zhans.get(i)].claimedRole = 1;//占卜co
                if(gs.gc[zhans.get(i)].claimedRoleorder == 0)
                    gs.gc[zhans.get(i)].claimedRoleorder = ++claimedRoleorder[1];//职业位次
                gs.gc[zhans.get(i)].comingOutDay = gs.gameDay;//co时机
                lasySuspicionValue[zhans.get(i)] -= 50;
                for(int j=0;j<i;j++)    //更新占候补之间的怀疑度
                    updatetop3SuspectedPlayersaux2(zhans.get(i),zhans.get(j),INF,INF);
            }
            //3,灵能co
            
                logicTools.log("非人的初始工作：灵能co");
            //狼灵
            //遍历每个人狼，判断是否有计划上灵的人狼
            
                logicTools.log("非人的初始工作：遍历每个人狼，判断是否有计划上灵的人狼");
            for(int i=1;i<=initialWolfCount;i++)
            {
                if(nonHumanPlan[rlindex[i]] != 2) continue;//只遍历计划上占人狼
                lings.add(rlindex[i]);//添加到灵候补数组
                break;//不允许复数的人狼上灵
            }

            //狂灵信灵
            
                logicTools.log("非人的初始工作：狂灵信灵");
            if(gs.gc[actualRoleindex[9-kyojin]].whyDie == whyDie.NONE && nonHumanPlan[actualRoleindex[9-kyojin]] == 2)//存活并且有上灵计划
                lings.add(actualRoleindex[9-kyojin]);//添加到灵候补数组

            //真灵
            
                logicTools.log("非人的初始工作：真灵");
            if(gs.gc[actualRoleindex[2]].whyDie == whyDie.NONE)
                lings.add(actualRoleindex[2]);//添加到灵候补数组

            //打乱灵候补数组
            //灵能随机排序
            
                logicTools.log("非人的初始工作：灵能随机排序");
            lings = shuffleList(lings);
            for(int i=0;i<lings.size();i++)
            {
                eventarray.add(new Event(EventName.lnco18,CharacterEnglishName.values()[gs.gc[lings.get(i)].number]));//添加事件：灵co
                gs.gc[lings.get(i)].claimedRole = 2;//灵能co
                if(gs.gc[lings.get(i)].claimedRoleorder == 0)
                    gs.gc[lings.get(i)].claimedRoleorder = ++claimedRoleorder[2];//职业位次
                gs.gc[lings.get(i)].comingOutDay = gs.gameDay;//co时机
                lasySuspicionValue[lings.get(i)] -= 30;
                for(int j=0;j<i;j++)    //更新灵候补之间的怀疑度
                    updatetop3SuspectedPlayersaux2(lings.get(i),lings.get(j),INF,INF);
            }

            //4,共有逻辑（不可能接到黑球）
            
                logicTools.log("非人的初始工作：共逻辑");
            gylogic();
            //5,非人co猫猎逻辑
            //不会接黒，没有这部分逻辑
        }
        else
        {
            //没咒杀
            
                logicTools.log("非人的初始工作：无咒杀");
            //2,占卜co
            
                logicTools.log("非人的初始工作：占卜co");
            //判断进行：是否是黑进行
            //狼占
            
                logicTools.log("非人的初始工作：狼占");
            for (int i=1;i<=initialWolfCount;i++)
            {
                if(gs.gc[rlindex[i]].whyDie != whyDie.NONE || nonHumanPlan[rlindex[i]] != 1 || zw[rlindex[i]] <= gs.getPlayerSum()) continue;//若不是存活计划上占发黑的人狼，则退出
                zhans.add(rlindex[i]);//黑球特攻狼占加入占卜候补数组
                gs.gc[rlindex[i]].skillTarget[1] = zw[rlindex[i]];//将占文添加到技能数组
            }
            //狂占信占
            
                logicTools.log("非人的初始工作：狂占信占");
            if(gs.gc[actualRoleindex[9-kyojin]].whyDie == whyDie.NONE && nonHumanPlan[actualRoleindex[9-kyojin]] == 1
                    && zw[actualRoleindex[9-kyojin]] > gs.getPlayerSum())//狂占信占黒特攻
            {
                zhans.add(actualRoleindex[9-kyojin]);//黒特攻狂占信占加入占卜候补数组
                gs.gc[actualRoleindex[9-kyojin]].skillTarget[1] = zw[actualRoleindex[9-kyojin]];//将占文添加到技能数组
            }
            //狐占
            
                logicTools.log("非人的初始工作：狐占");
            if(actualRoleindex[10] > 0 && gs.gc[actualRoleindex[10]].whyDie == whyDie.NONE && nonHumanPlan[actualRoleindex[10]] == 1
                    && zw[actualRoleindex[10]] > gs.getPlayerSum())//狐占黒特攻
            {
                zhans.add(actualRoleindex[10]);//黒特攻狐占加入占卜候补数组
                gs.gc[actualRoleindex[10]].skillTarget[1] = zw[actualRoleindex[10]];//将占文添加到技能数组
            }
            //背占
            
                logicTools.log("非人的初始工作：背占");
            if(actualRoleindex[11] > 0 && gs.gc[actualRoleindex[11]].whyDie == whyDie.NONE && nonHumanPlan[actualRoleindex[11]] == 1
                    && zw[actualRoleindex[11]] > gs.getPlayerSum())//背占黒特攻
            {
                zhans.add(actualRoleindex[11]);//黒特攻背占加入占卜候补数组
                gs.gc[actualRoleindex[11]].skillTarget[1] = zw[actualRoleindex[11]];//将占文添加到技能数组
            }
            //真占
            
                logicTools.log("非人的初始工作：真占");
            if(gs.gc[actualRoleindex[1]].whyDie == whyDie.NONE && gs.gc[actualRoleindex[1]].skillTarget[1] > gs.getPlayerSum())//真占初夜黒
                zhans.add(actualRoleindex[1]);//真占加入占卜候补数组
            boolean gqian = gs.p != peiyi.jianyi && gs.gc[diebody.get(0)].actualRole == 4;//共欠标记:需要不是简易村
            
                logicTools.log("非人的初始工作：共欠co时机");
            if(gqian)
            {
                int alivegynum = 2;
                if(gs.gc[gyindex[1]].whyDie == whyDie.NONE) alivegynum = 1;
                //共欠co时机
                eventarray.add(new Event(EventName.gkgsw4,CharacterEnglishName.values()[gs.gc[gyindex[alivegynum]].number],
                        CharacterEnglishName.values()[gs.gc[gyindex[3 - alivegynum]].number]));//共co,公开共死亡4
                gs.gc[gyindex[1]].claimedRole = 4;//
                gs.gc[gyindex[2]].claimedRole = 4;//共co
                gs.gc[gyindex[1]].comingOutDay = gs.gameDay;//co时机
                gs.gc[gyindex[2]].comingOutDay = gs.gameDay;//co时机
                lasySuspicionValue[gyindex[1]] -= INF;
                lasySuspicionValue[gyindex[2]] -= INF;
            }
            //打乱占候补数组
            zhans = shuffleList(zhans);
            while(zhans.size() > 3) //处理黑球占过多的情况：强行删除多余的黑球占，改为计划潜伏 极限情况：3黑球占 + 1接黒回复占 + 1白进行占
            {
                zhans = shuffleList(zhans);
                if(zhans.get(0) == actualRoleindex[1]) continue;//真占不删除
                gs.gc[zhans.get(0)].skillTarget[1] = 0;//将占文删除
                nonHumanPlan[zhans.get(0)] = 0;//改为整局潜伏
                zhans.remove(0);//删除数组第一个元素
            }
            //黑球占co时机
            
                logicTools.log("非人的初始工作：黑球占co时机");
            for(int i=0;i<zhans.size();i++)
            {
                //logicTools.log("黑球占卜co，当前玩家：" + CharacterEnglishName.values()[zhans.get(i)]+",占文：" + gs.gc[zhans.get(i)].skillTarget[1]);
                if(gs.gc[zhans.get(i)].claimedRole != 1)//自加判断
                    actualRoleCo(zhans.get(i),1);//占卜co
            }
            boolean black = zhans.size() > 0 ;//黑进行提示变量初始值:有黑球占卜师或者共有第一
            //非人接黒的策略判断，以及真职业接黒反应->若有职业co，转为白进行，black = false
            //统一处理接黒co反应
            boolean morelzll = false,morelmll = false;//狼占狼灵接黒标记，狼猫狼猎接黒标记
            //不允许复数个人狼接黒之后同时上占上灵或者上猫上猎，只能一个上占或上灵，另外一个上猫或上猎
            //不允许接黒上职的人狼太多了

            //提取所有接黒的玩家，要去重。为了防止同一位玩家多次co，甚至co不同身份。
            
                logicTools.log("非人的初始工作：接黒co时机");
            ArrayList<Integer> bplayers = new ArrayList<Integer>();
            for(int i=0;i<zhans.size();i++)
            {
                if(!bplayers.contains(gs.gc[zhans.get(i)].skillTarget[1] - gs.getPlayerSum()))
                    bplayers.add(gs.gc[zhans.get(i)].skillTarget[1] - gs.getPlayerSum());//手动去重
            }
            for(int i=0;i<bplayers.size();i++)
            {
                int bplayer = bplayers.get(i);//黑球玩家编号
                if(bplayer > 0 && gs.gc[bplayer].whyDie == whyDie.NONE && gs.gc[bplayer].claimedRole == 0 && gs.gc[bplayer].actualRole != 6)//接黒玩家存活并且还没co职业,不是村人
                {
                    if(zhenying(gs.gc[bplayer]) == 0)//村侧玩家接黒
                    {
                        //村侧玩家
                        
                            logicTools.log("村侧玩家接黒");
                        if(gs.gc[bplayer].actualRole < 6 && gs.gc[bplayer].actualRole != 3)//白球占,灵能，共有者,猫又
                        {
                            response.add(new IntPair(bplayer,gs.gc[bplayer].actualRole));//添加到反应数组
                        }
                    }
                    else if(nonHumanPlan[bplayer] != 4 && nonHumanPlan[bplayer] != 0)//非人有预定计划，但是接了黑。
                    {
                        
                            logicTools.log("非人有预定计划，但是接了黑：" + bplayer);
                        if(nonHumanPlan[bplayer] == 3) continue;//上猎计划不需要在此处理
                        //按照预定计划来co
                        if(nonHumanPlan[bplayer] == 1)
                        {
                            //随机选择一篇占文进行报告
                            int ybweight = 50,zwweight = 50;
                            if(zw[bplayer] - gs.getPlayerSum() > 0 && gs.gc[zw[bplayer] - gs.getPlayerSum()].claimedRole == 4) zwweight = 0;//不能发共有者黑
                            int option = getEventIndexByProbability(new ArrayList<Integer>(List.of(ybweight,zwweight)));//选择一篇占文
                            if(option == 0)
                                gs.gc[bplayer].skillTarget[1] = ybzw[bplayer];//预备占文
                            else
                                gs.gc[bplayer].skillTarget[1] = zw[bplayer];//占文
                        }
                        response.add(new IntPair(bplayer,nonHumanPlan[bplayer]));//添加到反应数组：占or灵or猫
                    }
                    else//没有事先计划的非人接到黑球
                    {
                        //非人玩家
                        
                            logicTools.log("非人玩家接黒");
                        if(gs.gc[bplayer].actualRole == 7 && (nonHumanPlan[bplayer] == 0 || nonHumanPlan[bplayer] == 4) )
                        {
                            //人狼接黒并且没有之前的计划
                            int lz = 0,ll = 0,lzweight = 30,llweight = 35,lmlweight = 5,lwcoweight = 30;
                                                                        //当前狼占的数量,狼灵的数量,狼各种策略的权重：上占，上灵，上猫猎，无co
                            for(int j=1;j<=initialWolfCount;j++)
                                if(gs.gc[rlindex[j]].claimedRole == 1)
                                    lz++;//累加狼占数量
                                else if(gs.gc[rlindex[j]].claimedRole == 2)
                                    ll++;//累加狼灵的数量
                            if(lz > 1)  lzweight = 0;
                            if(ll > 0)  llweight = 0;//不接受第三个狼占和第二个狼灵
                            if(morelzll)//已经有接黒上占上灵的人狼，不允许其他人狼接黒上占上灵
                            {
                                lzweight = 0;
                                llweight = 0;
                            }
                            if(morelmll)//已经有接黒上猫猎的人狼，不允许其他人狼接黒上猫猎
                            {
                                lmlweight = 0;
                            }
                            if(gs.p == peiyi.jianyi && (zhans.contains(rlindex[1]) || zhans.contains(rlindex[2])))
                            {
                                lzweight = 0;//简易村，已经存在上占人狼，则不能全部露出。
                                llweight = 0;//狼占狼灵狼猎co不能，因为会全露出。
                                lmlweight = 0;
                            }
                            int option = getEventIndexByProbability(new ArrayList<Integer>(List.of(lzweight,llweight,lmlweight,lwcoweight)));
                            switch(option)
                            {
                                case 0:
                                    gs.gc[bplayer].skillTarget[1] = ybzw[bplayer];//添加预备占文到技能数组当中
                                    response.add(new IntPair(bplayer,1));//准备co占
                                    morelzll = true;
                                    break;//狼占
                                case 1:
                                    response.add(new IntPair(bplayer,2));//准备co灵
                                    morelzll = true;
                                    //此处应该处理狼灵复数的情况：若有人狼接黒上灵，则其余人狼缩回去。
                                    for(int j=1;j<=initialWolfCount;j++)
                                    {
                                        if(nonHumanPlan[rlindex[j]] == 2 && !bplayers.contains(nonHumanPlan[rlindex[j]]))
                                            nonHumanPlan[rlindex[j]] = 4;//若有没有接黒的计划上灵的人狼，则缩回去
                                    }
                                    break;//狼灵
                                case 2:
                                    nonHumanPlan[bplayer] = 5 - getEventIndexByProbability(new ArrayList<Integer>(List.of(50 + 50 * lies.size(),50+50 * maos.size()))) * 2;//3 猎 5 猫
                                    if(actualRoleindex[5] == 0)
                                        nonHumanPlan[bplayer] = 3;//这村没猫，所以只能co猎人
                                    if(nonHumanPlan[bplayer] == 5)
                                        response.add(new IntPair(bplayer,5));//准备co猫
                                    morelmll = true;
                                    break;//猫猎
                                case 3:
                                    nonHumanPlan[bplayer] = 0;//死心潜伏
                                    break;//认民
                            }
                        }
                        else
                        {
                            //白非接黒并且没有之前的计划->狂狐背
                            
                                logicTools.log("白非接黒并且没有之前的计划->狂狐背");
                            int option = 0;
                            switch(bplayer)
                            {
                                case 8: case 9:
                                    option = getEventIndexByProbability(new ArrayList<Integer>(List.of(95,5,0,0)));break;//狂占狂灵
                                case 10:option = getEventIndexByProbability(new ArrayList<Integer>(List.of(80,5,10,5)));break;//狐占狐灵狐猫猎狐潜伏
                                case 11:option = getEventIndexByProbability(new ArrayList<Integer>(List.of(45+kz,30,20,5-kz)));break;//背占背灵背猫猎背潜伏
                            }
                            switch(option)
                            {
                                case 0:
                                    gs.gc[bplayer].skillTarget[1] = ybzw[bplayer];//添加预备占文到技能数组当中
                                    response.add(new IntPair(bplayer,1));//准备co占
                                    break;//占
                                case 1:
                                    response.add(new IntPair(bplayer,2));//准备co灵
                                    break;//灵
                                case 2:
                                    nonHumanPlan[bplayer] = 5 - getEventIndexByProbability(new ArrayList<Integer>(List.of(50 + 50 * lies.size(),50+50 * maos.size()))) * 2;//3 猎 5 猫
                                    if(actualRoleindex[5] == 0)
                                        nonHumanPlan[bplayer] = 3;//这村没猫，只能co猎人
                                    if(nonHumanPlan[bplayer] == 5)
                                        response.add(new IntPair(bplayer,5));//准备co猫
                                    break;//猫猎
                                case 3:
                                    nonHumanPlan[bplayer] = 0;//死心潜伏
                                    break;//认民
                            }
                        }
                    }
                }
            }
            
                logicTools.log("本次有无猫又co：" );
            boolean havecatco = false;//本次有无猫又co
            response = shuffleList(response);

            //处理接黒的时候狼占狼灵偶有可能数量超标的情况
            int lzsum = 0,llsum = 0;//狼占狼灵的数量
            for(int i=1;i<=initialWolfCount;i++)
            {
                if(gs.gc[rlindex[i]].claimedRole == 1)
                    lzsum++;
                else if(gs.gc[rlindex[i]].claimedRole == 2)
                    llsum++;
            }
            for(int i=0;i<response.size();i++)
            {
                if(response.get(i).first != 7) continue;
                if(response.get(i).second == 2)
                {
                    if(llsum > 0)
                    {
                        response.remove(i);
                        nonHumanPlan[response.get(i).first] = 0;
                        continue;
                    }
                    llsum++;
                }
                if(response.get(i).second == 1)
                {
                    if(lzsum > 1)
                    {
                        response.remove(i);
                        nonHumanPlan[response.get(i).first] = 0;
                        continue;
                    }
                    lzsum++;
                }
            }

            //处理接黒回复当中占灵候补过多的情况
                //控制占总数<6,灵总数<5
                //若本次回复之后会出现5占或4灵，则必须减少对应的co
                //这一步之后，最多4占3灵。真占真灵若还未co，仍可以co
            int rezhan = 0,reling = 0;//接黒回复的占灵数
            for(int i=0;i<response.size();i++)
            {
                if(response.get(i).second == 1)
                    rezhan++;
                else if(response.get(i).second == 2)
                    reling++;
            }
                //处理过多的占
            while(zhans.size() + rezhan > 4)
            {
                response = shuffleList(response);
                if(response.get(0).second == 1 && response.get(0).first != actualRoleindex[1])//首位是占候补且不是真占
                {
                    nonHumanPlan[response.get(0).first] = 0;//将占候选人设为潜伏
                    gs.gc[response.get(0).first].skillTarget[1] = 0;
                    response.remove(0);
                }
            }
                //处理过多的灵
            while(lings.size() + reling > 3)
            {
                response = shuffleList(response);
                if(response.get(0).second == 2 && response.get(0).first != actualRoleindex[2])//首位是灵候补且不是真灵
                {
                    nonHumanPlan[response.get(0).first] = 0;//将占候选人设为潜伏
                    response.remove(0);
                }
            }
            //接黒上职回复
            for(int i=0;i<response.size();i++)
            {
                lasySuspicionValue[response.get(i).first] += 10;//接黒上职，怀疑度+10.
                actualRoleCo(response.get(i).first,response.get(i).second);//村职co
                if(response.get(i).second == 5)//猫
                {
                    havecatco = true;
                }
            }
            if(havecatco)
                askCo(Role.mao);//若有人co猫，立刻进行一次喊猫
            //处理接黒上职回复之后占灵数过多的情况
            //占数<6,灵数<5
            int jhzhan = 0,jhling = 0;
            for(int i=1;i<=gs.getPlayerSum();i++)
            {
                if(gs.gc[i].whyDie == whyDie.NONE && gs.gc[i].claimedRole == 0)//存活，未上职
                {
                    if(gs.gc[i].actualRole == 1 || nonHumanPlan[i] == 1)jhzhan ++;
                    else if(gs.gc[i].actualRole == 2 || nonHumanPlan[i] == 2)jhling ++;
                }
            }
                //削减过多的占灵候补
            while(zhans.size() + jhzhan > 5 || lings.size() + jhling > 4)
            {
                int target = ConstNum.randomInt(1,gs.getPlayerSum());
                if(gs.gc[target].claimedRole == 0 && nonHumanPlan[target] > 0 && nonHumanPlan[target] < 3)
                {
                    if(zhans.size() + jhzhan > 5 && nonHumanPlan[target] == 1)
                    {
                        jhzhan--;
                        nonHumanPlan[target] = 0;
                        gs.gc[target].skillTarget[1] = 0;
                    }
                    else if(lings.size() +jhling > 4 && nonHumanPlan[target] == 2)
                    {
                        jhling--;
                        nonHumanPlan[target] = 0;
                    }
                }
            }
            if(zhans.size() == 0 || response.size() > 0 || gqian || havecatco)
            {
                //白进行或者共欠转为白进行
                
                    logicTools.log("非人的初始工作：白球占、灵能co");
                //白球占、灵能co
                for(int i = 1;i<=gs.getPlayerSum();i++)
                {
                    if(gs.gc[i].claimedRole != 0 || gs.gc[i].whyDie != whyDie.NONE) continue;//排除死人和有co的人
                    if(gs.gc[i].actualRole < 3 )//真村职co：占灵co （特殊处理共有co）
                    {
                        actualRoleCo(i,gs.gc[i].actualRole);
                    }
                    else if(nonHumanPlan[i] == 1 || nonHumanPlan[i] == 2)
                    {
                        if(nonHumanPlan[i] == 1)
                        {
                            gs.gc[i].skillTarget[1] = zw[i];//添加占文
                            
                                logicTools.log("添加占文：角色"  + i+" 占文"+gs.gc[i].skillTarget[1]);
                        }
                        actualRoleCo(i,nonHumanPlan[i]);
                    }
                }
                if(gyindex[1] != 0 && gyindex[2] != 0 && gs.gc[gyindex[1]].claimedRole != 4 && gs.gc[gyindex[2]].claimedRole != 4)
                    gylogic();
            }
            response.clear();//清空反应数组
        }
        delieverevent();//发送事件，清空数组
    }
    private void nightaction()//夜间行动函数
    {
        //1,真占占卜逻辑
        int zhantarget = zhenzhan(actualRoleindex[1]);
        //2,得到真灵能技能结果
        int num = getDiePlayerNum(whyDie.chuxing,gs.gameDay);
        gs.gc[actualRoleindex[2]].skillTarget[gs.gameDay] = num;//默认白结果
        if(gs.gc[num].actualRole == 7)
            gs.gc[actualRoleindex[2]].skillTarget[gs.gameDay] += gs.getPlayerSum();//黑结果
        //3,狼咬逻辑,wolf[0]:主咬狼；wolf[1]：被咬玩家
        int wolf[] = wolfwork();
        //4,猎人工作逻辑
        int lietarget = zhenlie(actualRoleindex[3]);
        //5,死体逻辑，并且返回当天夜间死体
        diebody = dielogic(wolf[0],wolf[1],zhantarget,lietarget);
        gs.end = judgeend();
        if(gs.end != 0) //游戏结束
        {
            logicTools.log("游戏结束，添加结束事件");
            int weight[] = new int[gs.getPlayerSum()+1];
            for(int i=1;i<=gs.getPlayerSum();i++)
            {
                if(gs.gc[i].whyDie != whyDie.NONE) continue;
                if(gs.end == 1 && gs.gc[i].actualRole < 7) weight[i] = 1;
                else if(gs.end == 2 && gs.gc[i].actualRole == 7) weight[i] = 1;
                else if(gs.end == 3 && gs.gc[i].actualRole == 10) weight[i] = 1;
                else weight[i] = -INF;
            }
            CharacterEnglishName player = CharacterEnglishName.values()[gs.gc[getOne(weight)].number];//获取发表获奖感言的玩家
            switch(gs.end)
            {
                case 1:
                    eventarray.add(new Event(EventName.crsl,player,null));
                    break;//村人胜利
                case 2:
                    if((actualRoleindex[9-kyojin] > 0 && gs.gc[actualRoleindex[9-kyojin]].whyDie == whyDie.NONE))
                        eventarray.add(new Event(EventName.krsl,CharacterEnglishName.values()[gs.gc[actualRoleindex[9-kyojin]].number],null));
                    else
                        eventarray.add(new Event(EventName.rlsl,player,null));
                    break;//人狼或狂人胜利
                case 3:
                    eventarray.add(new Event(EventName.yhsl,player,null));
                    break;//妖狐胜利
            }
            delieverevent();
            return;//胜负已分
        }
        //6,非人占灵猎编造结果逻辑
        frlying();
        //7,增加一天时间
        gs.gameDay++;
        //8，白天起身逻辑
        dayaction();
        //9,更新怀疑度
        updatetop3SuspectedPlayers();
    }
    private void frlyingaux(int num,int zhi)
    {
        //非人撒谎辅助函数，num：玩家编号 zhi：职业类型
        if(gs.gc[num].nonHumanMarker) return;//破绽非人不再更新技能数组
        switch (zhi)
        {
            case 1://假占
            {
                //特判处理第一天假占潜伏的情况
                if(gs.gc[num].skillTarget[1] < 1)
                {
                    gs.gc[num].skillTarget[1] = zw[num];//添加占文
                }
                int target[] = new int[3];//占文集合
                //1,列视角，得到当前视角黑白球理论上的比例
                boolean zhaned[] = new boolean[gs.getPlayerSum()+1];//占卜过的标记
                int blackzhi[] = new int[7];//有黑球的职业，不能发同职业复数黑球。占除外
                int heip = initialWolfCount, baip = gs.getPlayerSum() - initialWolfCount;//初始占卜得到黑球和白球的相对概率大小关系
                //占卜结果排除
                for (int i = 1; i < gs.gameDay; i++)
                {
                    if (gs.gc[num].skillTarget[i] > gs.getPlayerSum())
                    {
                        //黑结果，剩余可占黑球结果-1
                        heip -= 1;
                        zhaned[gs.gc[num].skillTarget[i] - gs.getPlayerSum()] = true;
                        blackzhi[gs.gc[gs.gc[num].skillTarget[i] - gs.getPlayerSum()].claimedRole]++;//黑球对应的职业数组增1
                    }
                    else if(gs.gc[num].skillTarget[i] > 0)
                    {
                        if (isackwhite(gs.gc[num].skillTarget[i])) continue;//排除确定白，避免计数错误
                        baip -= 1;
                        zhaned[gs.gc[num].skillTarget[i]] = true;//白结果，剩余可占白结果-1
                    }
                }
                //夜间死亡排除
                for (int i = 1; i < gs.gameDay; i++)
                {
                    ArrayList<Integer> array = dieatnight(i);
                    if(array.size() < 2)
                        baip -=array.size();
                    else
                        baip -= array.size() - 1;
                }
                //确定白排除
                for(int i=1;i<=gs.getPlayerSum();i++)
                    if(isackwhite(i) && gs.gc[i].whyDie == whyDie.NONE)
                        baip--;
                //2,写两篇占文，内容有可能会重复
                for (int i = 0; i < 2; i++)
                {
                    target[i] = zhenzhan(num);//像真占一样选择占卜对象
                    if(target[i] < 1) continue;//完占，返回
                    if (target[i] > gs.getPlayerSum()) target[i] -= gs.getPlayerSum();//先默认为白色结果
                    int heisum = 0, feisum = 0, waiheisum = 0;//统计目前为止发出去的黑球数,不明正体的非人数，明确黑球数
                    feisum += max(zhans.size() - 1, 0);
                    feisum += max(lings.size() - 1, 0);
                    feisum += max(lies.size() - 1, 0);
                    feisum += max(maos.size() - 1, 0);//占领猎猫当中的非人统计
                    for (int j = 1; j < gs.gameDay; j++)
                        if (gs.gc[num].skillTarget[j] > gs.getPlayerSum())
                        {
                            heisum++;
                            if (Math.abs(gs.gc[gs.gc[num].skillTarget[j] - gs.getPlayerSum()].claimedRole - 3) < 3)//上职的黑球
                            {
                                feisum--;//减少非人统计数，算重复了
                            }
                        }
                    if (heisum >= initialWolfCount) continue;//黑球完占，返回白
                    if ((gs.gc[target[i]].claimedRole == 0 || gs.gc[target[i]].claimedRole == 6) && feisum + heisum >= initialNonHumanCount)
                        continue;//完占，返回白
                    //基于自身视角得到剩余白结果数和黑结果数
                    // 黑+确定白+夜间死体（单死确白，多死一个灰，其余的算确白）
                    //3,对于每篇占文，基于基础概率比例以及自身职业修正得到占卜结果
                    //职业发球修正
                    if(Math.abs(gs.gc[target[i]].claimedRole - 3) < 3) //黑球上职
                    {
                        if(blackzhi[gs.gc[target[i]].claimedRole] > 0 && gs.gc[target[i]].claimedRole != 1) continue;//不能发复数个同职业黑球
                        if(gs.gc[target[i]].claimedRole == 1 && blackzhi[gs.gc[target[i]].claimedRole] > 1) continue;//占候补最多2黑球
                    }
                    int option = getEventIndexByProbability(new ArrayList<Integer>(List.of(baip,heip)));//根据现成比例得到占卜结果
                    target[i] += option * gs.getPlayerSum();//有可能改为黑结果
                    switch (gs.gc[num].actualRole)
                    {
                        case 7://人狼 80%几率包庇队友
                            if(option == 1 && gs.gc[target[i]-gs.getPlayerSum()].actualRole == 7 && !logicTools.probabilityJudge(80))
                                target[i] -= gs.getPlayerSum();//改回白色结果
                            break;
                        case 8://狂人 15%几率放弃发白球
                            if(option == 0 && logicTools.probabilityJudge(15))
                                target[i] += gs.getPlayerSum();//改为黑色结果
                            break;
                        case 9://狂信 90%几率包庇队友
                            if(option == 1 && gs.gc[target[i]-gs.getPlayerSum()].actualRole == 7 && !logicTools.probabilityJudge(90))
                                target[i] -= gs.getPlayerSum();//改回白色结果
                            break;
                        case 10://妖狐 15%几率放弃发黑球
                            if(option == 1 && logicTools.probabilityJudge(15))
                                target[i] -= gs.getPlayerSum();//改为白色结果
                            break;
                        case 11://背德 100%保狐狸 10%几率放弃发白球
                            if(option == 1 && gs.gc[target[i]-gs.getPlayerSum()].actualRole == 10)
                                target[i] -= gs.getPlayerSum();//改回白色结果
                            if(option == 0 && logicTools.probabilityJudge(10))
                                target[i] += gs.getPlayerSum();//改为黑色结果
                            break;
                    }
                }
                //4,根据死体情况，选择占文发送
                int weight[] = new int[3];
                for(int i=0;i<2;i++)
                {
                    weight[i] = 50;//基础权重
                    if(diebody.contains(target[i]))
                        weight[i] += 900;//尽量主动发死体白球
                    else if(diebody.contains(target[i] - gs.getPlayerSum()) && diebody.size() < 2)
                        weight[i] -= 49;//不主动发单死死体黑球
                }
                int option = getEventIndexByProbability(new ArrayList<Integer>(List.of(weight[0],weight[1])));
                gs.gc[num].skillTarget[gs.gameDay] = target[option];//添加占文
                //不能主动发死体黑球。尽量主动发死体白球
                break;
            }
            case 2://假灵
            {
                int shokei = getDiePlayerNum(whyDie.chuxing,gs.gameDay);//处刑对像
                gs.gc[num].skillTarget[gs.gameDay] = shokei;
                //处理明确妖狐和明确猫又的情况
                if(shokei == actualRoleindex[10] && actualRoleindex[11] > 0 && gs.gc[actualRoleindex[11]].whyDie == whyDie.dayhouzhui)
                {
                    //明确后追的妖狐
                    return;
                }
                if(shokei == actualRoleindex[5] && actualRoleindex[5] > 0 && gs.gc[actualRoleindex[5]].whyDie == whyDie.chuxing)
                {
                    //明确吊死的猫又
                    return;
                }
                int heisum = 0,feisum = 0,heip = initialWolfCount,baip = gs.getPlayerSum() - initialWolfCount;
                int blackzhi[] = new int[7];
                boolean linged[] = new boolean [gs.getPlayerSum()*3];
                //1,处理被处刑的玩家
                for(int i=2;i<gs.gameDay;i++)
                {
                    int t = gs.gc[num].skillTarget[i];
                    linged[t] = true;
                    if(t > gs.getPlayerSum())
                    {
                        t -= gs.getPlayerSum();
                        heisum++;
                        heip--;
                        if(Math.abs(gs.gc[t].claimedRole - 3) < 3)
                        {
                            blackzhi[gs.gc[t].claimedRole] += 1;
                            feisum --;//之后会重复计算，此处先减去非人计数
                        }
                    }
                    else
                    {
                        baip--;
                    }
                }
                //2,处理夜间死亡的玩家
                //夜间死亡排除
                for (int i = 1; i < gs.gameDay; i++)
                {
                    ArrayList<Integer> array = dieatnight(i);
                    if(array.size() < 2)
                        baip -=array.size();
                    else
                        baip -= array.size() - 1;
                }
                //3,确定白排除
                for(int i=1;i<=gs.getPlayerSum();i++)
                    if(isackwhite(i) && gs.gc[i].whyDie == whyDie.NONE)
                        baip--;
                //4,处理占灵猫猎候补
                feisum += max(0,zhans.size()-1);
                feisum += max(0,lings.size()-1);
                feisum += max(0,lies.size()-1);
                feisum += max(0,maos.size()-1);
                //5，铁逻辑
                //视角完全，外置位都是白
                if(feisum + heisum >= initialNonHumanCount  && (gs.gc[shokei].claimedRole == 0 || gs.gc[shokei].claimedRole == 6)  )
                {
                    break;
                }
                //黑色够了，返回白
                if(heisum >= initialWolfCount - 1)
                {
                    break;
                }
                //职业上的黑色够了，处刑其他职业返回白球
                if(Math.abs(gs.gc[shokei].claimedRole - 3) < 3)
                {
                    if(blackzhi[gs.gc[shokei].claimedRole] > 0)
                    {
                        if(gs.gc[shokei].claimedRole != 1) break;//灵猫猎有黑
                        if(blackzhi[gs.gc[shokei].claimedRole] > 1)break;//二黑占
                    }
                }
                //6，优先使用其他逻辑：保护队友、忠诚连线、反对切线
                ArrayList<Integer> option = new ArrayList<Integer>();//选择数组
                //保护队友
                if(gs.gc[num].actualRole == 7 || gs.gc[num].actualRole == 9)
                {
                    if(gs.gc[shokei].actualRole == 7) //队友即将被处刑
                        option.add(0);//白色倾向加入
                    for(int i=0;i<zhans.size();i++)//队友上占
                    {
                        if(gs.gc[zhans.get(i)].actualRole != 7 || gs.gc[zhans.get(i)].suspicionValue[num] > INFJ)continue;
                        //必须是还未切线的上占队友
                        int zhan = zhans.get(i);
                        for(int j=1;j<gs.gameDay;j++)
                            if(gs.gc[zhan].skillTarget[j] == shokei)
                                option.add(0);
                            else if(gs.gc[zhan].skillTarget[j] - shokei == gs.getPlayerSum())
                                option.add(1);
                    }
                    if(option.size() > 0)
                    {
                        int op = option.get(ConstNum.randomInt(0,option.size()-1));//最终的操作结果
                        int p = 90;
                        if(gs.gc[num].actualRole == 9) p = 95;//包庇概率：狼90信95
                        if(!logicTools.probabilityJudge(p)) op  = 1 - op;
                        if(op == 1) gs.gc[num].skillTarget[gs.gameDay] += gs.getPlayerSum();//黑球主张
                        break;
                    }
                }
                //忠诚连线判定
                for(int i=0;i<zhans.size();i++)
                {
                    int zhan = zhans.get(i);
                    if(gs.gc[zhan].suspicionValue[num] < INFJ && gs.gc[num].suspicionValue[zhan] < INFJ && lined[zhan][num] == 1)
                    {
                        //连线的占灵关系
                        for(int j=1;j<gs.gameDay;j++)
                            if(gs.gc[zhan].skillTarget[j] == shokei)
                                option.add(0);
                            else if(gs.gc[zhan].skillTarget[j] - shokei == gs.getPlayerSum())
                                option.add(1);
                    }
                    if(option.size() >0)
                    {
                        int op = option.get(ConstNum.randomInt(0,option.size()-1));//最终的操作结果
                        int p = 90;
                        switch(gs.gc[num].actualRole)
                        {
                            case 7:p = 95;break;
                            case 8:p = 90;break;
                            case 9:p = 99;break;
                            case 10:p = 95;break;
                            case 11:p = 80;break;
                        }
                        if(!logicTools.probabilityJudge(p)) op  = 1 - op;
                        if(op == 1) gs.gc[num].skillTarget[gs.gameDay] += gs.getPlayerSum();
                    }
                    break;
                }
                //反对切线判定
                for(int i=0;i<zhans.size();i++)
                {
                    int zhan = zhans.get(i);
                    if(gs.gc[zhan].suspicionValue[num] > INFJ && gs.gc[num].suspicionValue[zhan] > INFJ)
                    {
                        //切线的占灵关系
                        for(int j=1;j<gs.gameDay;j++)
                            if(gs.gc[zhan].skillTarget[j] - shokei == gs.getPlayerSum())
                                option.add(0);
                    }
                    if(option.size() > 0)
                    {
                        int op = option.get(ConstNum.randomInt(0,option.size()-1));//最终的操作结果
                        int p = 90;
                        switch(gs.gc[num].actualRole)
                        {
                            case 7:p = 85;break;
                            case 8:p = 90;break;
                            case 9:p = 95;break;
                            case 10:p = 90;break;
                            case 11:p = 80;break;
                        }
                        if(!logicTools.probabilityJudge(p)) op  = 1 - op;
                        if(op == 1) gs.gc[num].skillTarget[gs.gameDay] += gs.getPlayerSum();
                    }
                    break;
                }
                //根据具体概率给出结果
                int op = getEventIndexByProbability(new ArrayList<Integer>(List.of(baip,heip)));
                if(op == 1) gs.gc[num].skillTarget[gs.gameDay] += gs.getPlayerSum();
                break;
            }
            case 3://假猎
            {
                
                    logicTools.log("进入假猎人撒谎环节，假猎人下标：" + num);
                int target[] = new int [2];
                ArrayList<Integer> weight = new ArrayList<>(List.of(50,50));
                for(int i=0;i<2;i++)//两篇猎文
                {
                    target[i] = zhenlie(num);
                    while(gs.gc[num].actualRole == 7 || gs.gc[num].actualRole == 9)//狼信上猎，会优先保护狼队友
                    {
                        if(gs.gc[target[i]].actualRole == 7) break;
                        if(logicTools.probabilityJudge(10))
                        {
                            target[i] = zhenlie(num);//重写猎文
                            continue;
                        }
                        break;
                    }
                    if(diebody.size() == 1 && target[i] == diebody.get(0))
                        weight.set(i,1);
                }
                int op = getEventIndexByProbability(weight);
                gs.gc[num].skillTarget[gs.gameDay] = target[op];
                
                    logicTools.log("假猎"+CharacterKanjiName.values()[gs.gc[num].number]
                            +",假猎目标"+CharacterKanjiName.values()[gs.gc[target[op]].number]);
                break;
            }
        }
    }
    private ArrayList<Integer> dieatnight(int day)//返回某一天夜间死亡的玩家array数组
    {
        //返回某一天夜间死亡的玩家array数组
        ArrayList<Integer> array = new ArrayList<Integer>();
        for(int i=1;i<=gs.gameDay;i++)
            if(gs.gc[i].dieDay == day && gs.gc[i].whyDie != whyDie.chuxing && gs.gc[i].whyDie != whyDie.dayhouzhui &&
            gs.gc[i].whyDie != whyDie.daymaozhou)
                      array.add(i);
        return array;
    }
    private void frlying()
    {
        //非人编造结果逻辑
        for (int i=1;i<=gs.getPlayerSum();i++)
        {
            if(zhenying(gs.gc[i])  == 0 || gs.gc[i].whyDie != whyDie.NONE) continue;//存活非人
            if(gs.gc[i].claimedRole == 6 || (gs.gc[i].claimedRole == 0 && nonHumanPlan[i] == 0)) continue;//死心潜伏或者已经村人co，则退出
            if(gs.gc[i].claimedRole == 1 || (gs.gc[i].claimedRole == 0 && nonHumanPlan[i] == 1))    //假占候选
            {
                frlyingaux(i,1);
            }
            else if(gs.gc[i].claimedRole == 2 || (gs.gc[i].claimedRole == 0 && nonHumanPlan[i] == 2))   //假灵候选
            {
                frlyingaux(i,2);
                logicTools.log("假灵能结果：" + gs.gc[i].skillTarget[gs.gameDay]);
            }
            else  frlyingaux(i,3);   //假猎候选
        }
    }
    private void dayaction()
    {
        logicTools.log("现在是白天。当前存活人数" + gs.aliveCounter + ",存活名单：\n");
        String alivemans = "";
        for (int i = 1; i <= gs.getPlayerSum(); i++)
            if (gs.gc[i].whyDie == whyDie.NONE)
                alivemans += CharacterKanjiName.values()[gs.gc[i].number];
        logicTools.log(alivemans);
        logicTools.log("\n");
        //1,占灵发球逻辑
        for(int i=0;i<zhans.size();i++)
        {
            zhanresult(zhans.get(i));//报告占卜结果（共有接黒已经包含在里面）
        }
        for(int i=0;i<lings.size();i++)
        {
            lingresult(lings.get(i));//报告灵能结果
        }
        //2,潜伏占灵co逻辑
        if(gs.gameDay == 3)//只有第三日才能co潜伏占灵
            for(int i=1;i<=gs.getPlayerSum();i++)
            {
                if(gs.gc[i].claimedRole != 0) continue;
                if(gs.gc[i].actualRole == 2 || nonHumanPlan[i] == 2)
                {
                    //真灵能或者计划上灵的非人
                    actualRoleCo(i,2);//灵能co
                    continue;
                }
                if(gs.gc[i].actualRole == 1 || nonHumanPlan[i] == 1)
                {
                    //真占或者计划上占的非人，连续报告两天的占卜结果
                    actualRoleCo(i,1);//占卜co
                }
                if(claimedRoleaskday[1] == 0)
                    claimedRoleaskday[1] = 3;
                if(claimedRoleaskday[2] == 0)
                    claimedRoleaskday[2] = 3;//锁死占灵不能再co
            }
        //3,非人co猫猎逻辑(共死猫，双死猫),真猫猎co逻辑
            //4共有全灭co猫
        if(gs.p != peiyi.jianyi && gyindex[1] > 0 && gs.gc[gyindex[1]].whyDie != whyDie.NONE && gs.gc[gyindex[2]].whyDie != whyDie.NONE)
            askCo(Role.mao);//喊猫
            //5猫村双死co猫
        if(diebody.size() == 2)//出现三死和四死，没必要co猫
            askCo(Role.mao);//喊猫
            //1接黒
            //先处理当天占卜师的黑球，然后处理特殊情况：第三天co的占卜在第一夜占卜的黑球.
        response.clear();
        boolean havecatco = false;//本次是否存在猫又co
        if(claimedRoleaskday[3] == 0 || (actualRoleindex[5] != 0 && claimedRoleaskday[5] == 0))//当前确实存在可以co的职业
        {
            for(int i=0;i<zhans.size();i++)
            {
                int zhan = zhans.get(i);
                if(gs.gc[zhan].skillTarget[gs.gameDay - 1] > gs.getPlayerSum())
                {
                    int target = gs.gc[zhan].skillTarget[gs.gameDay - 1] - gs.getPlayerSum();
                    if(gs.gc[target].whyDie != whyDie.NONE || gs.gc[target].claimedRole != 0)//已经死亡或者已经co职业
                        continue;
                    if(gs.gc[target].actualRole == 5 || nonHumanPlan[target] == 5)    //真猫或计划中的假猫，接黒co
                    {
                        response.add(new IntPair(target,5));havecatco = true;continue;
                    }
                    if(gs.gc[target].actualRole < 7 || nonHumanPlan[target] == 0 || nonHumanPlan[target] == 3) continue;//村侧其他情况已经排除,潜伏死心非人也排除
                    int zhi = gs.gc[target].actualRole;
                    switch(zhi)
                    {
                        case  7:
                            if(logicTools.probabilityJudge(probabilityCalculator.maolieco(0,1,getp1(7),getp2(7))))
                            {
                                int lweight = 50+50*maos.size(),mweight = 50 + 50*lies.size();
                                if(rlsl || claimedRoleaskday[3] != 0) lweight = 0;
                                if(rlsm || actualRoleindex[5] == 0 || claimedRoleaskday[5] != 0) mweight = 0;
                                if(lweight + mweight == 0) nonHumanPlan[target] = 0;
                                else
                                {
                                    nonHumanPlan[target] = 3 + 2 * getEventIndexByProbability(new ArrayList<>(List.of(lweight,mweight)));
                                    if(nonHumanPlan[target] == 5)
                                    {
                                        response.add(new IntPair(target,5));havecatco = true;rlsm = true;
                                    }
                                    else rlsl = true;
                                }
                            }
                            else
                                nonHumanPlan[target] = 0;//潜伏死心
                            break;
                        default:
                            if(logicTools.probabilityJudge(probabilityCalculator.maolieco(zhi-7,1,getp1(zhi),getp2(zhi))))
                            {
                                int lweight = 50+50*maos.size(),mweight = 50 + 50*lies.size();
                                if(claimedRoleaskday[3] != 0) lweight = 0;
                                if(actualRoleindex[5] == 0 || claimedRoleaskday[5] != 0) mweight = 0;
                                if(lweight + mweight == 0) nonHumanPlan[target] = 0;
                                else
                                {
                                    nonHumanPlan[target] = 3 + 2 * getEventIndexByProbability(new ArrayList<>(List.of(lweight,mweight)));
                                    if(nonHumanPlan[target] == 5)
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
            askCo(Role.mao);//喊猫cco
        //4,共有者相关逻辑
        gylogic();
        delieverevent();//发送事件，清空数组
    }
    private ArrayList<Integer> dielogic(int wolf,int wolfbite,int zhantarget,int lietarget)  //夜间死亡逻辑函数。处理夜间死亡的所有逻辑 返回值：死体arraylist
    {
        ArrayList<Integer> diebody = new ArrayList<>();
        eventarray.clear();
        //狼咬，夜间猫咒
        if(wolfbite != lietarget && actualRoleindex[10] != wolfbite)//守护失败，并且咬到的不是妖狐
        {
            diebody.add(wolfbite);//被咬玩家
            dieaux(wolfbite,whyDie.beiyao);//调用辅助函数：被咬
            if(gs.gc[wolfbite].actualRole == 5)
            {
                //咬到猫了
                diebody.add(wolf);
                dieaux(wolf,whyDie.nightmaozhou);//调用辅助函数：夜间猫咒
            }
        }
        //死因覆盖：狼咬优先于后追。背德被咬同时妖狐咒杀时，背德死因算作被咬。
        if(zhantarget > 0 && zhantarget <= gs.getPlayerSum() && gs.gc[zhantarget].actualRole == 10)
        {
            //咒杀
            diebody.add(zhantarget);//咒杀妖狐
            dieaux(zhantarget,whyDie.zhousha);//调用辅助函数：咒杀
            if(actualRoleindex[11] > 0 && gs.gc[actualRoleindex[11]].whyDie == whyDie.NONE)
            {
                //若本局存在背德，并且背德并没有死亡
                //（若当天背德被咬，此时gc已经改变，if判定为false）
                diebody.add(actualRoleindex[11]);
                dieaux(actualRoleindex[11],whyDie.nighthouzhui);//调用辅助函数：夜间后追
            }
        }
        eventarray = shuffleList(eventarray);//随机打乱死亡事件数组
            for(int i=0;i<diebody.size();i++)
            {
                logicTools.log("夜间死体："+CharacterKanjiName.values()[gs.gc[diebody.get(i)].number]);
            }
        if(diebody.size() < 1)
            eventarray.add(new Event(EventName.wsw,null,null));//平和事件
        else if(diebody.size() == 1)
            for(int i=0;i<zhans.size();i++)
            {
                //占候补是否破绽:黑球夜间单死
                int zhan = zhans.get(i);
                if(zhan == actualRoleindex[1]) continue;//真占肯定不会破绽
                for(int j=1;j<=gs.gameDay;j++)
                {
                    int target = gs.gc[zhan].skillTarget[j] - gs.getPlayerSum();
                    if(target < 1) continue;
                    //留下黑色结果
                    if(diebody.contains(target))
                        gs.gc[zhan].nonHumanMarker = true;//黑球夜间单死，确定破绽
                }
            }
        else
        {
            isDoubleDeathOccurred[gs.gameDay] = true;//维护多死数组
            if(actualRoleindex[5] > 1 && gs.gc[actualRoleindex[5]].whyDie == whyDie.NONE && isackwhite(actualRoleindex[5])
                && !diebody.contains(actualRoleindex[5]))//存在存活的唯一猫又
                for (int i = 0; i < zhans.size(); i++)
                {
                    //占候补是否破绽:黑球夜间死亡，并且场上存在唯一猫
                    int zhan = zhans.get(i);
                    if (zhan == actualRoleindex[1]) continue;//真占肯定不会破绽
                    for (int j = 1; j <= gs.gameDay; j++)
                    {
                        int target = gs.gc[zhan].skillTarget[j] - gs.getPlayerSum();
                        if (target < 1) continue;
                        //留下黑色结果
                        if (diebody.contains(target))
                            gs.gc[zhan].nonHumanMarker = true;//黑球夜间死亡并且场上存在存活的唯一猫又，确定破绽
                    }
                }
        }
        delieverevent();//播放死者事件
        return diebody;//返回死体数组
    }
    /**
     * 泛型洗牌函数：打乱任意类型ArrayList（不修改原列表）
     * 随机数来源：ConstNum.randomInt(x,y)（返回x到y之间的随机整数，包含两端）
     * @param originalList 待打乱的原列表（支持IntPair/AbstractMap.Entry/任意泛型）
     * @param <T> 列表元素的泛型类型（可传入IntPair、AbstractMap.Entry<Integer,Integer>等）
     * @return 打乱后的新列表；入参为null/空时返回空列表
     */
    public static <T> ArrayList<T> shuffleList(ArrayList<T> originalList)
    {
        // 1. 空值/空列表兜底
        if (originalList == null || originalList.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 复制原列表，避免修改原始数据
        ArrayList<T> shuffledList = new ArrayList<>(originalList);
        int listSize = shuffledList.size();

        // 3. Fisher-Yates洗牌算法，适配ConstNum.randomInt
        for (int i = listSize - 1; i > 0; i--) {
            // 生成0到i（包含两端）的随机索引
            int randomIndex = ConstNum.randomInt(0, i);
            // 交换当前索引和随机索引的元素（泛型无需类型转换）
            T temp = shuffledList.get(i);
            shuffledList.set(i, shuffledList.get(randomIndex));
            shuffledList.set(randomIndex, temp);
        }

        return shuffledList;
    }
    private void dieaux(int index,whyDie why)    //玩家死亡辅助函数，处理玩家死亡对于gs类的修正
    {
        gs.gc[index].whyDie = why;  //死因记录
        gs.aliveCounter --;     //存活玩家减少
        gs.deathCounter ++;       //死亡玩家增多
        gs.gc[index].dieDay = gs.gameDay;   //死亡日记录
        //将死亡事件放在事件数组当中
        if(why == whyDie.chuxing)
        {
            eventarray.add(new Event(EventName.cxs,CharacterEnglishName.values()[gs.gc[index].number]));
            if(gs.gc[index].claimedRole == 5 && gs.gc[index].actualRole != 5)
            {
                gs.gc[index].nonHumanMarker = true;
            }
        }
        else if(why == whyDie.dayhouzhui)
            eventarray.add(new Event(EventName.hzsw,CharacterEnglishName.values()[gs.gc[index].number]));
        else if(why == whyDie.daymaozhou)
            eventarray.add(new Event(EventName.mzsw,CharacterEnglishName.values()[gs.gc[index].number]));
        else
            eventarray.add(new Event(EventName.yjsw,CharacterEnglishName.values()[gs.gc[index].number]));
    }
    private void updatetop3SuspectedPlayersaux2(int num1,int num2,int w1,int w2)//功能：num1对num2的怀疑度增量为w1；num2对num1的怀疑度增量为w2
    {
        //功能：num1对num2的怀疑度增量为w1；num2对num1的怀疑度增量为w2
        gs.gc[num1].suspicionValue[num2] += w1;
        gs.gc[num2].suspicionValue[num1] += w2;
    }
    private int judgeend()//判断当前游戏是否结束 返回值：0未结束 1村胜利 2狼胜利 3狐胜利
    {
        //判断当前游戏是否结束 0未结束 1村胜利 2狼胜利 3狐胜利
        int humancnt = 0,wolfcnt = 0;
        for(int i=1;i<=gs.getPlayerSum();i++)
        {
            if(gs.gc[i].whyDie != whyDie.NONE) continue;
            if(gs.gc[i].actualRole == 7)
            {
                wolfcnt++;continue;
            }
            if(gs.gc[i].actualRole == 10) continue;//妖狐不算入人数
            humancnt++;
        }
        if(actualRoleindex[10] > 0 && gs.gc[actualRoleindex[10]].whyDie == whyDie.NONE
        && (wolfcnt == 0 || wolfcnt >= humancnt))
        {
            //妖狐胜利
            return 3;
        }
        if(wolfcnt == 0)
        {
            return 1;//村人胜利
        }
        if((actualRoleindex[8] > 0 && gs.gc[actualRoleindex[8]].whyDie == whyDie.NONE) ||
                (actualRoleindex[9] > 0 && gs.gc[actualRoleindex[9]].whyDie == whyDie.NONE))
        {
            wolfcnt++;humancnt--;//狂人狂信参与半pp
        }
        if((actualRoleindex[10] < 1 || gs.gc[actualRoleindex[10]].whyDie != whyDie.NONE) && wolfcnt >= humancnt)
            return 2;//人狼胜利,需要妖狐不在场
        return 0;//胜负未定
    }
    private void gylogic()
    {
        //共有者相关逻辑
        //排除共有者接黒以外的情况，处理共有者选择fo或ho，以及暴露进度的增加。
        //每天执行一次。
        //未处理共欠的情况
        if(gs.p == peiyi.jianyi) return;//没有共有者的情况
        if(gs.gc[gyindex[1]].whyDie != whyDie.NONE && gs.gc[gyindex[2]].whyDie != whyDie.NONE) return; //目前两共有全部死亡的情况
        
            logicTools.log("peiyi:"+gs.p);
        if(gyindex[1] < 1)return;//没有共有者
        if(gs.gc[gyindex[1]].claimedRole == 4 && gs.gc[gyindex[2]].claimedRole == 4)return; //全体co，无需处理
        if(gs.gc[gyindex[1]].claimedRole == 0 && gs.gc[gyindex[2]].claimedRole == 0)//目前两共有全部潜伏的情况
        {
            if(gs.gc[gyindex[1]].whyDie != whyDie.NONE || gs.gc[gyindex[2]].whyDie != whyDie.NONE)
            {
                //公开共死亡
                int gy = 1;//活着的共有
                if(gs.gc[gyindex[1]].whyDie != whyDie.NONE) gy = 2;
                eventarray.add(new Event(EventName.gkgsw4,CharacterEnglishName.values()[gs.gc[gyindex[gy]].number],
                        CharacterEnglishName.values()[gs.gc[gyindex[3-gy]].number]));//公开共死亡4
                gs.gc[gyindex[1]].claimedRole = 4;//共有co
                gs.gc[gyindex[1]].comingOutDay = gs.gameDay;//co时机
                gs.gc[gyindex[2]].claimedRole = 4;//共有co
                gs.gc[gyindex[2]].comingOutDay = gs.gameDay;//co时机
                lasySuspicionValue[gyindex[1]] -= INF;
                lasySuspicionValue[gyindex[2]] -= INF;
                return;
            }
            if(logicTools.probabilityJudge(50))
            {
                //50判定通过，共有fo
                if(logicTools.probabilityJudge(50))//共1co，共2确认
                {
                    eventarray.add(new Event(EventName.gyfo1,CharacterEnglishName.values()[gs.gc[gyindex[1]].number],
                            CharacterEnglishName.values()[gs.gc[gyindex[2]].number]));//共有co事件
                    eventarray.add(new Event(EventName.gyfo1r,CharacterEnglishName.values()[gs.gc[gyindex[2]].number],
                            CharacterEnglishName.values()[gs.gc[gyindex[1]].number]));//共有co同伴确认
                }
                else//共2co，共1确认
                {
                    eventarray.add(new Event(EventName.gyfo1,CharacterEnglishName.values()[gs.gc[gyindex[2]].number],
                            CharacterEnglishName.values()[gs.gc[gyindex[1]].number]));//共有co事件
                    eventarray.add(new Event(EventName.gyfo1r,CharacterEnglishName.values()[gs.gc[gyindex[1]].number],
                            CharacterEnglishName.values()[gs.gc[gyindex[2]].number]));//共有co同伴确认
                }
                gs.gc[gyindex[1]].claimedRole = 4;//共有co
                gs.gc[gyindex[1]].comingOutDay = gs.gameDay;//co时机
                gs.gc[gyindex[2]].claimedRole = 4;//共有co
                gs.gc[gyindex[2]].comingOutDay = gs.gameDay;//co时机
                lasySuspicionValue[gyindex[1]] -= INF;
                lasySuspicionValue[gyindex[2]] -= INF;
            }
            else
            {
                exposureProgress = ConstNum.randomInt(1,90);//暴露进度初始值
                //50判定未通过，共有ho
                if(logicTools.probabilityJudge(50))//共1co，共2潜伏
                {
                    eventarray.add(new Event(EventName.gyho2,CharacterEnglishName.values()[gs.gc[gyindex[1]].number]));//共有co事件
                    gs.gc[gyindex[1]].claimedRole = 4;//共有co
                    gs.gc[gyindex[1]].comingOutDay = gs.gameDay;//co时机
                    lasySuspicionValue[gyindex[1]] -= INF;
                }
                else //共2co，共1潜伏
                {
                    eventarray.add(new Event(EventName.gyho2,CharacterEnglishName.values()[gs.gc[gyindex[2]].number]));//共有co事件
                    gs.gc[gyindex[2]].claimedRole = 4;//共有co
                    gs.gc[gyindex[2]].comingOutDay = gs.gameDay;//co时机
                    lasySuspicionValue[gyindex[2]] -= INF;
                }
            }
            return;
        }
        int qfnum = 1;//潜伏共有是第几个共有
        if(gs.gc[gyindex[1]].claimedRole == 4) qfnum = 2;
        if(exposureProgress == 0)
            exposureProgress = ConstNum.randomInt(1,90);//暴露进度初始值
        else
            exposureProgress += ConstNum.randomInt(5,15);//增加暴露进度
        if(gs.gc[gyindex[3-qfnum]].whyDie != whyDie.NONE || gs.gc[gyindex[qfnum]].whyDie != whyDie.NONE) exposureProgress = INF;//共有同伴死亡，立即co
        if(exposureProgress > 99)
        {
            gs.gc[gyindex[qfnum]].claimedRole = 4;//共有co
            gs.gc[gyindex[qfnum]].comingOutDay = gs.gameDay;
            lasySuspicionValue[gyindex[qfnum]] -= INF;
            //原版处刑明共有者会固定触发回避共台词.所以明共有无法被处刑
            if(gs.gc[gyindex[qfnum]].whyDie == whyDie.chuxing)
                eventarray.add(new Event(EventName.gycx6,CharacterEnglishName.values()[gs.gc[gyindex[3-qfnum]].number],
                        CharacterEnglishName.values()[gs.gc[gyindex[qfnum]].number]));//共有处刑6
            else if(gs.gc[gyindex[3-qfnum]].whyDie != whyDie.NONE)
                eventarray.add(new Event(EventName.gkgsw4,CharacterEnglishName.values()[gs.gc[gyindex[qfnum]].number],
                        CharacterEnglishName.values()[gs.gc[gyindex[3-qfnum]].number]));//公开共死亡4
            else if(gs.gc[gyindex[qfnum]].whyDie != whyDie.NONE)
                eventarray.add(new Event(EventName.qfgsw3,CharacterEnglishName.values()[gs.gc[gyindex[3-qfnum]].number],
                        CharacterEnglishName.values()[gs.gc[gyindex[qfnum]].number]));//潜伏共死亡3
            else
            {
                eventarray.add(new Event(EventName.qfjc5,CharacterEnglishName.values()[gs.gc[gyindex[qfnum]].number],
                        CharacterEnglishName.values()[gs.gc[gyindex[3-qfnum]].number]));//潜伏解除5
                eventarray.add(new Event(EventName.qfjcqr5r,CharacterEnglishName.values()[gs.gc[gyindex[3-qfnum]].number],
                        CharacterEnglishName.values()[gs.gc[gyindex[qfnum]].number]));//潜伏解除确认5r
            }
        }
    }
    public int getEventIndexByProbability(ArrayList<Integer> probabilities) //根据概率，返回互斥事件中发生的事件的编号 从0开始计数
    {
        //不需要数组元素总和一定为100
        // 1. 基础参数校验：数组非空
        if (probabilities == null || probabilities.isEmpty())
        {
            throw new IllegalArgumentException("概率数组不能为空！");
        }

        // 2. 校验元素合法性（非负）
        int total = 0;
        for (Integer prob : probabilities)
        {
            if (prob == null || prob < 0) {
                throw new IllegalArgumentException("概率数组元素必须为非负整数！");
            }
            total += prob;
        }
        if(total == 0)
            throw new IllegalArgumentException("概率数组元素总和必须为正整数！");
        // 3. 替换为外部提供的随机数方法：生成0-99（包含两端）的随机数
        int randomValue = ConstNum.randomInt(0, total-1);

        // 4. 累加概率，匹配随机数对应的索引
        int cumulativeProb = 0;
        for (int i = 0; i < probabilities.size(); i++) {
            cumulativeProb += probabilities.get(i);
            if (randomValue < cumulativeProb) {
                return i; // 仅返回索引，无任何打印逻辑
            }
        }

        // 理论上不会执行到此处（总和为100，randomValue<100必然匹配到最后一个索引）
        throw new IllegalStateException("概率计算异常，未匹配到有效索引！");
    }
    private void zhanresult(int num)//之前已经co的占卜报告当天的占卜结果
    {
        //报告某一天的占卜结果
        //适用于早已co的占卜师候补。
        //当天临时co的占候补不适用
        //占结果黑、站结果白、咒杀、是咒杀吗、咒杀破绽、昨日占卜结果、占卜对象死亡
        //num:占候补编号 day:占卜日期 diebody当晚死者
        if(gs.gc[num].whyDie != whyDie.NONE || gs.gc[num].nonHumanMarker) return;//占候补不能已经死亡，也不能破绽
        int target = gs.gc[num].skillTarget[gs.gameDay-1];//占卜对象
        if(target < 1 || target == num)//非法占卜对象，视为完占
        {
            eventarray.add(new Event(EventName.wz17,CharacterEnglishName.values()[gs.gc[num].number]));//完占
            return;
        }
        if(diebody.size() < 2)//普通情况
        {
            if(target > gs.getPlayerSum())//黑球结果
            {
                target -= gs.getPlayerSum();
                lasySuspicionValue[target] += 10;
                lasySuspicionValue[num] += 2;
                updatetop3SuspectedPlayersaux2(num,target,INF,INF);
                eventarray.add(new Event(EventName.zjgh8b,CharacterEnglishName.values()[gs.gc[num].number],
                        CharacterEnglishName.values()[gs.gc[target].number]));//占结果黑
                if(gs.gc[target].whyDie == whyDie.NONE)
                {
                    if(gs.gc[target].actualRole != 4)
                        eventarray.add(new Event(EventName.jhdh8b, CharacterEnglishName.values()[gs.gc[target].number],
                                CharacterEnglishName.values()[gs.gc[num].number]));//接黒对话
                    else//查杀共有
                    {
                        eventarray.add(new Event(EventName.gprz11r,CharacterEnglishName.values()[gs.gc[target].number],
                                CharacterEnglishName.values()[gs.gc[num].number]));//共pair认证11r
                        int gy = 1;
                        if(gyindex[2] == target) gy = 2;
                        if(gs.gc[gyindex[3-gy]].whyDie == whyDie.NONE)
                            eventarray.add(new Event(EventName.gprz11p,CharacterEnglishName.values()[gs.gc[gyindex[3-gy]].number],
                                    CharacterEnglishName.values()[gs.gc[target].number]));//共pair认证11p
                        lasySuspicionValue[num] += INF;
                        lasySuspicionValue[target] -= INF;
                        gs.gc[target].claimedRole = 4;
                        gs.gc[target].comingOutDay = gs.gameDay;
                        gs.gc[num].nonHumanMarker = true;//破绽非人
                        if(gs.gc[gyindex[3-gy]].claimedRole != 4)
                        {
                            gs.gc[gyindex[3-gy]].claimedRole = 4;
                            gs.gc[gyindex[3-gy]].comingOutDay=gs.gameDay;
                        }
                    }
                }
            }
            else if(gs.gc[target].whyDie == whyDie.NONE)//白结果并且占卜对象存活
            {
                lasySuspicionValue[target] -= 10;
                updatetop3SuspectedPlayersaux2(num,target,-5,-10);
                eventarray.add(new Event(EventName.zjgb8,CharacterEnglishName.values()[gs.gc[num].number],
                        CharacterEnglishName.values()[gs.gc[target].number]));//占结果白
                eventarray.add(new Event(EventName.jbdh8r,CharacterEnglishName.values()[gs.gc[target].number],
                        CharacterEnglishName.values()[gs.gc[num].number]));//接白对话
            }
            else //白结果并且占卜对象死亡
            {
                eventarray.add(new Event(EventName.zbdxsw10,CharacterEnglishName.values()[gs.gc[num].number],
                        CharacterEnglishName.values()[gs.gc[target].number]));//占卜对象死亡
            }
        }
        else if(actualRoleindex[5] > 0 && isackwhite(actualRoleindex[5]) && !diebody.contains(actualRoleindex[5]) && !diebody.contains(target))
        {
            //唯一猫的前提，猫以外出现双死，没有对应。
            //咒杀破绽
            eventarray.add(new Event(EventName.zspz15, CharacterEnglishName.values()[gs.gc[num].number]));//咒杀破绽
            lasySuspicionValue[num] += INF;
            gs.gc[num].nonHumanMarker = true;//破绽非人占卜
        }
        else if((gs.p != peiyi.daxing && gs.p != peiyi.maoyou) || diebody.size() > 2)  //双死，本村无猫；或者死者超过两位
        {
            if(diebody.contains(target))    //白结果并且死体被占：咒杀
            {
                eventarray.add(new Event(EventName.zs14,CharacterEnglishName.values()[gs.gc[num].number],
                        CharacterEnglishName.values()[gs.gc[target].number]));//咒杀主张
            }
            else    //咒杀破绽
            {
                eventarray.add(new Event(EventName.zspz15,CharacterEnglishName.values()[gs.gc[num].number]));//咒杀破绽
                lasySuspicionValue[num] += INF;
                gs.gc[num].nonHumanMarker = true;//破绽非人占卜
            }
        }
        else    //双死，本村有猫
        {
            if(diebody.contains(target))    //占死体白
            {
                eventarray.add(new Event(EventName.szsm16,CharacterEnglishName.values()[gs.gc[num].number],
                        CharacterEnglishName.values()[gs.gc[target].number]));//是咒杀吗
            }
            else if(target <= gs.getPlayerSum()) //普通白色结果
            {
                lasySuspicionValue[target] -= 10;
                updatetop3SuspectedPlayersaux2(num,target,-5,-10);
                eventarray.add(new Event(EventName.zjgb8,CharacterEnglishName.values()[gs.gc[num].number],
                        CharacterEnglishName.values()[gs.gc[target].number]));//占结果白
                eventarray.add(new Event(EventName.jbdh8r,CharacterEnglishName.values()[gs.gc[target].number],
                        CharacterEnglishName.values()[gs.gc[num].number]));//接白对话
            }
            else    //黑色结果
            {
                target -= gs.getPlayerSum();
                lasySuspicionValue[target] += 10;
                lasySuspicionValue[num] += 2;
                updatetop3SuspectedPlayersaux2(num,target,INF,INF);
                eventarray.add(new Event(EventName.zjgh8b,CharacterEnglishName.values()[gs.gc[num].number],
                        CharacterEnglishName.values()[gs.gc[target].number]));//占结果黑
                if(gs.gc[target].whyDie == whyDie.NONE)//黑球没有死亡
                    eventarray.add(new Event(EventName.jhdh8b,CharacterEnglishName.values()[gs.gc[target].number],
                            CharacterEnglishName.values()[gs.gc[num].number]));//接黒对话
            }
        }
    }
    private void lingresult(int num)//任意灵媒师报告当天的灵能结果
    {
        if(gs.gc[num].whyDie != whyDie.NONE) return;//若已经死亡，则退出
        if(gs.gc[num].claimedRole != 2)   //当天刚co
        {
            gs.gc[num].claimedRole = 2;
            gs.gc[num].comingOutDay = gs.gameDay;//灵能co
            if(gs.gc[num].claimedRoleorder == 0)
                gs.gc[num].claimedRoleorder = ++claimedRoleorder[2];//同职业位次
            for(int i=0;i<lings.size();i++)
            {
                updatetop3SuspectedPlayersaux2(num,lings.get(i),INF,INF);//灵候补之间怀疑更新
            }
            if(!lings.contains(num))    //不重复获取位次
                lings.add(num);
            lasySuspicionValue[num] -= 30;
            if(gs.gameDay > 2)
                lasySuspicionValue[num] += 10;//潜伏占灵co被怀疑
        }
        if(gs.gc[num].skillTarget[gs.gameDay - 1] > gs.getPlayerSum())//灵结果黑
        {
            eventarray.add(new Event(EventName.ljgh19b,CharacterEnglishName.values()[gs.gc[num].number],
                    CharacterEnglishName.values()[gs.gc[getDiePlayerNum(whyDie.chuxing, gs.gameDay- 1) ].number]));//灵结果黑
        }
        else//灵结果白
        {
            eventarray.add(new Event(EventName.ljgb19,CharacterEnglishName.values()[gs.gc[num].number],
                    CharacterEnglishName.values()[gs.gc[getDiePlayerNum(whyDie.chuxing, gs.gameDay- 1) ].number]));//灵结果白
        }
        //只需要利用当天的灵能结果更新占灵切线逻辑即可
        for(int i=0;i<zhans.size();i++)
        {
            //占灵切线连线逻辑
            for (int j = 1; j <= gs.gameDay; j++)
                {
                    if(gs.gc[num].suspicionValue[zhans.get(i)] <= INFJ
                            && Math.abs(gs.gc[num].skillTarget[gs.gameDay - 1] - gs.gc[zhans.get(i)].skillTarget[j]) == gs.gameDay)
                    {
                        lasySuspicionValue[num] += 5;
                        lasySuspicionValue[zhans.get(i)] += 5;
                        updatetop3SuspectedPlayersaux2(num,zhans.get(i),INF,INF);
                    }
                    if(gs.gc[num].suspicionValue[zhans.get(i)] <= INFJ
                            && gs.gc[num].skillTarget[gs.gameDay - 1] == gs.gc[zhans.get(i)].skillTarget[j]
                    && gs.gc[zhans.get(i)].skillTarget[j] > gs.getPlayerSum())
                    {
                        updatetop3SuspectedPlayersaux2(num,zhans.get(i),-10,-10);
                        lined[zhans.get(i)][num] = 1;
                    }
                }
        }
    }
    private void actualRoleCo(int num,int zhi)
    {
        if(gs.gc[num].whyDie != whyDie.NONE) return;//死人不会co
        
            logicTools.log("进入役职co:num,zhi:"+num+","+zhi+",初日技能情况："+gs.gc[num].skillTarget[1]);
        //职业co辅助函数
        // 是普通地，对于当前时点的处理。其他处理需要额外使用语句，如第三日co的占灵候补信用-10.
        //参数：玩家下标、玩家co的职业
        //对于占卜co，需要考虑当天的死体数组
        //zhi范围：1~6 共有co默认是普通事件不是接黒事件
        if(gs.p != peiyi.daxing && gs.p != peiyi.maoyou && zhi == 5) return;//不允许没有猫的村去co猫
        if(gs.gc[num].claimedRole != zhi)   //不会重复co
        {
            gs.gc[num].claimedRole = zhi;//职业co
            gs.gc[num].comingOutDay = gs.gameDay;//co时机
        }
        switch(zhi)
        {
            case 1://占卜师
            {
                for(int i=0;i<zhans.size();i++)
                {
                    updatetop3SuspectedPlayersaux2(num,zhans.get(i),INF,INF);//占候补之间怀疑更新
                }

                //增加逻辑：非人三日目co占的时候，不能发当天单死的人黑球.
                if(gs.gameDay == 3 && gs.gc[num].actualRole > 6 && gs.gc[num].skillTarget[1] > gs.getPlayerSum()
                && getDiePlayerNum(whyDie.beiyao,2) == gs.gc[num].skillTarget[1] - gs.getPlayerSum() )
                {
                    if(zw[num] == gs.gc[num].skillTarget[1] && ybzw[num] > 0) gs.gc[num].skillTarget[1] = ybzw[num];
                    else if(ybzw[num] == gs.gc[num].skillTarget[1] && zw[num] > 0) gs.gc[num].skillTarget[1] = zw[num];
                }

                for(int i=0;i<lings.size();i++)
                {
                    //占灵切线连线逻辑
                    for (int j = 1; j <= gs.gameDay; j++)
                        for (int k = 2; k <= gs.gameDay; k++)
                        {
                            if(gs.gc[num].suspicionValue[lings.get(i)] <= INFJ
                                    && Math.abs(gs.gc[lings.get(i)].skillTarget[k] - gs.gc[num].skillTarget[j]) == gs.gameDay)//占灵切线
                            {
                                lasySuspicionValue[num] += 5;
                                lasySuspicionValue[lings.get(i)] += 5;
                                updatetop3SuspectedPlayersaux2(num,lings.get(i),INF,INF);
                                lined[num][lings.get(i)] = 0;
                            }
                            else if(gs.gc[num].suspicionValue[lings.get(i)] <= INFJ && gs.gc[lings.get(i)].skillTarget[k] == gs.gc[num].skillTarget[j]
                            && gs.gc[num].skillTarget[j] > gs.getPlayerSum())
                            {
                                updatetop3SuspectedPlayersaux2(num,lings.get(i),-10,-10);//占灵连线
                                lined[num][lings.get(i)] = 1;
                            }
                        }
                }
                if(gs.gc[num].claimedRoleorder == 0)
                {
                    gs.gc[num].claimedRoleorder = ++claimedRoleorder[1];//同职业位次
                }
                if(!zhans.contains(num))
                    zhans.add(num);//加入职业数组
                lasySuspicionValue[num] -= 50;
                if(gs.gameDay > 2)
                    lasySuspicionValue[num] += 10;//潜伏占灵co被怀疑
                //处理第一天的占卜结果
                int target = gs.gc[num].skillTarget[1];//占卜对象
                if (target > gs.getPlayerSum())//黑球结果
                {
                    target -= gs.getPlayerSum();
                    lasySuspicionValue[target] += 10;
                    lasySuspicionValue[num] += 2;
                    updatetop3SuspectedPlayersaux2(num, target, INF, INF);
                    eventarray.add(new Event(EventName.zjgh8b, CharacterEnglishName.values()[gs.gc[num].number],
                            CharacterEnglishName.values()[gs.gc[target].number]));//占结果黑
                    
                        logicTools.log("添加事件占结果黑，占卜师编号："+num+"黑球姓名:"+CharacterEnglishName.values()[gs.gc[target].number].toString());
                    if(gs.gc[target].whyDie == whyDie.NONE)
                    {
                        if(gs.gc[target].claimedRole != 4)
                            eventarray.add(new Event(EventName.jhdh8b, CharacterEnglishName.values()[gs.gc[target].number],
                                    CharacterEnglishName.values()[gs.gc[num].number]));//接黒对话
                        else//查杀共有
                        {
                            eventarray.add(new Event(EventName.gprz11r,CharacterEnglishName.values()[gs.gc[target].number],
                                    CharacterEnglishName.values()[gs.gc[num].number]));//共pair认证11r
                            int gy = 1;
                            if(gyindex[2] == target) gy = 2;
                            if(gs.gc[gyindex[3-gy]].whyDie == whyDie.NONE)
                                eventarray.add(new Event(EventName.gprz11p,CharacterEnglishName.values()[gs.gc[gyindex[3-gy]].number],
                                        CharacterEnglishName.values()[gs.gc[target].number]));//共pair认证11p
                            lasySuspicionValue[num] += INF;
                            lasySuspicionValue[target] -= INF;
                            gs.gc[target].claimedRole = 4;
                            gs.gc[target].comingOutDay = gs.gameDay;
                            if(gs.gc[gyindex[3-gy]].claimedRole != 4)
                            {
                                gs.gc[gyindex[3-gy]].claimedRole = 4;
                                gs.gc[gyindex[3-gy]].comingOutDay=gs.gameDay;
                            }
                            gs.gc[num].nonHumanMarker = true;//破绽非人
                        }
                    }
                }
                else if (target > 0)  //白球结果
                {
                    lasySuspicionValue[target] -= 10;
                    updatetop3SuspectedPlayersaux2(num, target, -5, -10);
                    eventarray.add(new Event(EventName.zjgb8, CharacterEnglishName.values()[gs.gc[num].number],
                            CharacterEnglishName.values()[gs.gc[target].number]));//占结果白
                    if(gs.gc[target].whyDie == whyDie.NONE)
                        eventarray.add(new Event(EventName.jbdh8r, CharacterEnglishName.values()[gs.gc[target].number],
                                CharacterEnglishName.values()[gs.gc[num].number]));//接白对话
                }
                //若有第二天的占卜结果，则处理之
                if(gs.gameDay == 3)
                {
                    lasySuspicionValue[num] += 10;//潜伏占灵co被怀疑
                    zhanresult(num);
                }
                break;
            }
            case 2://灵能者
            {
                for(int i=0;i<lings.size();i++)
                {
                    updatetop3SuspectedPlayersaux2(num,lings.get(i),INF,INF);//灵候补之间怀疑更新
                }
                if(!lings.contains(num))    //不重复获取位次
                {
                    lings.add(num);
                }
                if(gs.gc[num].claimedRoleorder == 0)
                    gs.gc[num].claimedRoleorder = ++claimedRoleorder[2];//同职业位次
                lasySuspicionValue[num] -= 30;
                if(gs.gameDay == 2)
                {
                    eventarray.add(new Event(EventName.lnco18,CharacterEnglishName.values()[gs.gc[num].number]));//灵能co
                    break;
                }
                else
                {
                    lasySuspicionValue[num] += 10;//潜伏占灵co被怀疑
                    lingresult(num);//报告当天灵能结果
                }
                break;
            }
            case 3://猎人
                for(int i=0;i<lies.size();i++)
                    updatetop3SuspectedPlayersaux2(lies.get(i),num,INF,INF);//对抗怀疑
                if(!lies.contains(num))    //不重复获取位次
                {
                    lies.add(num);
                }
                if(gs.gc[num].claimedRoleorder == 0)
                    gs.gc[num].claimedRoleorder = ++claimedRoleorder[3];//同职业位次
                lasySuspicionValue[num] -= 30;
                eventarray.add(new Event(EventName.lrco,CharacterEnglishName.values()[gs.gc[num].number]));
                if(gs.gc[num].actualRole == 7)
                    rlsl = true;//人狼上猎标记
                break;
            case 4://共有者
                //此处触发的一定是潜伏解除事件组
                int gy = 1;//潜伏共有是第几个共有
                if(gyindex[2] == num) gy = 2;
                //if(gs.gc[gyindex[3-gy]].claimedRole == 4) break;//不会重复进行co

                lasySuspicionValue[num] -= INF;
                eventarray.add(new Event(EventName.qfjc5,CharacterEnglishName.values()[gs.gc[num].number],
                        CharacterEnglishName.values()[gs.gc[gyindex[3-gy]].number]));//潜伏解除5
                eventarray.add(new Event(EventName.qfjcqr5r,CharacterEnglishName.values()[gs.gc[gyindex[3-gy]].number],
                        CharacterEnglishName.values()[gs.gc[num].number]));//潜伏解除确认5r
                //另外一个共有者在这里一同co了，不触发两次事件
                if(gs.gc[gyindex[3-gy]].claimedRole != zhi)
                {
                    gs.gc[gyindex[3-gy]].claimedRole = zhi;//职业co
                    gs.gc[gyindex[3-gy]].comingOutDay = gs.gameDay;//co时机
                }
                break;
            case 5:
                for(int i=0;i<maos.size();i++)
                    updatetop3SuspectedPlayersaux2(maos.get(i),num,INF,INF);//对抗怀疑
                if(!maos.contains(num))    //不重复获取位次
                {
                    maos.add(num);
                }
                if(gs.gc[num].claimedRoleorder == 0)
                    gs.gc[num].claimedRoleorder = ++claimedRoleorder[5];//同职业位次
                lasySuspicionValue[num] -= 30;
                eventarray.add(new Event(EventName.mco,CharacterEnglishName.values()[gs.gc[num].number]));
                if(gs.gc[num].actualRole == 7)
                    rlsm = true;//人狼上猫标记
                break;
            case 6://村人co
                break;//无特殊内容
        }
        
            logicTools.log("退出役职co");
    }
    private int getDiePlayerNum(whyDie why,int day)//返回当天该死因的唯一玩家编号，其他情况返回-1
    {
        //参数：死亡原因，死亡时间
        //返回值：唯一死亡玩家编号，若有复数名玩家因此死亡或者没有玩家因此死亡，则返回-1
        int num = -1;//唯一因此死亡的玩家编号
        for(int i=1;i<=gs.getPlayerSum();i++)
        {
            //保证死因和死亡时间正确
            if(gs.gc[i].whyDie != why ||gs.gc[i].dieDay != day) continue;
            if(num > -1) return -1;
            num = i;
        }
        return num;
    }
    private int[] wolfwork()//人狼咬杀逻辑
    {
       //狼咬逻辑
        //参数：无
        //返回值：int[2]，wolf[0]:主咬狼；wolf[1]：被咬玩家
        //分配初始被咬权重
            //若该玩家是人狼，或该玩家已经死亡，或该玩家已经破绽，则被咬权重-INF，否则为50.
        int biteWeight[] = new int[gs.getPlayerSum()+1];
        for(int i=1;i<=gs.getPlayerSum();i++)
        {
            //若该玩家是人狼，或该玩家已经死亡，或该玩家已经破绽，则被咬权重-INF
            if(gs.gc[i].actualRole == 7 || gs.gc[i].dieDay != 0 || gs.gc[i].nonHumanMarker) biteWeight[i] -= INF;
            else                    biteWeight[i] += 20; //否则为20.
        }

        //-3.对狼侧进行咬杀猎人补正，猎人被咬概率随时间缓慢增加，总体比村人多一定比例
        biteWeight[actualRoleindex[3]] += gs.gameDay * 2;
        //-2.狂信在场的村。狼咬狂信权重-45，模拟狂信会递话不会轻易被误杀
        if(kyojin == 0)
            biteWeight[actualRoleindex[9]] -= 45;
        //-1,若当前是末狼，则咬杀猫候补的权重-50
        int aliverl = 0;
        for(int i=1;i<=initialWolfCount;i++)
        {
            if(gs.gc[rlindex[i]].whyDie == whyDie.NONE)
                aliverl++;
        }
        if(aliverl == 1)
        {
            for(int i=0;i<maos.size();i++)
                biteWeight[maos.get(i)] -= 50;//最后一狼对猫候补的咬杀权重-50， 避免轻易发生末狼咬猫送狐狸的情况
        }



        //0,若当前是第二夜之后，则应先确定人狼当前战略：打信用还是打生推
        //y:打生推标志，z：打信用标志
        //根据第二夜咬杀对象决定本局游戏打什么战术
        //咬杀真占候补：打生推；咬杀外置位：打信用
        int y = 0,z = 0;//y:打生推标志，z：打信用标志
        int zzhb = getDiePlayerNum(whyDie.beiyao,2);//获取第二天死于狼咬的玩家编号
        if(zzhb != -1 && gs.gc[zzhb].claimedRole == 1)
        {
            //判断是否是当时的真占候补
            if(gs.gc[zzhb].skillTarget[1] > gs.getPlayerSum())
            {
                //黑球应该为真黑
                if(gs.gc[gs.gc[zzhb].skillTarget[1] - gs.getPlayerSum()].actualRole == 7)
                    y = 1;
            }
            else
            {
                //白球应该为真白球
                if(gs.gc[gs.gc[zzhb].skillTarget[1]].actualRole != 7)
                    y = 1;
            }
        }
        if(y == 0)
            z = 1;
        //1,确定真占候补以及真占候补数量
        //2,确定真猎候补以及真猎候补数量
        //3，确定真灵候补以及真灵候补数量(注意真灵能候补数量包含死者)
        int zhenzhancnt=0,zhenliecnt=0,zhenlingcnt=0;//真占候补、真猎候补、真灵候补计数
        boolean iszhan[] = new boolean[gs.getPlayerSum()+1];//记录该玩家是否是真占候选
        boolean islie[]  = new boolean[gs.getPlayerSum()+1];//记录该玩家是否是真猎候选
        boolean isling[] = new boolean[gs.getPlayerSum()+1];//记录该玩家是否是真灵候选
        int baiqiu[] = new int[gs.getPlayerSum()+1];//记录所有占候补的存活有效白球数
        for(int i=1;i<=gs.getPlayerSum();i++)
        {
            //对于真灵候补的统计，包含已经死亡的玩家
            //若该玩家声称是灵媒师，非狼且该玩家没有发过人狼白球和非人狼黑球，则划为真灵候补
            if(gs.gc[i].claimedRole == 2 && gs.gc[i].actualRole != 7 && !gs.gc[i].nonHumanMarker)
            {
                //排除一些特殊情况：明确的破绽玩家不划为真灵候补
                //比如灵上的狐侧玩家，但是背德已经追随死。则此灵破绽。

                //检查该灵发球是否有误
                boolean ok = true;
                for(int j=1;j<=gs.gameDay;j++)
                {
                    //若该玩家在第i天发过人狼白球或非人狼黑球，则该灵发球有误
                    if(gs.gc[i].skillTarget[j] > gs.getPlayerSum()) //黑球结果
                    {
                        if(gs.gc[i].actualRole != 7)
                        {
                            ok = false;//若该玩家在第j天发过非人狼黑球，则该灵发球有误
                            break;
                        }
                    }
                    else //白球结果
                    {
                        if(gs.gc[i].actualRole == 7)
                        {
                            ok = false;//若该玩家在第j天发过人狼白球，则该灵发球有误
                            break;
                        }
                    }
                }
                if(!ok) continue;//若该玩家发过人狼白球或非人狼黑球，则该灵发球有误，continue
                isling[i] = true;//若该玩家声称是灵媒师且非狼，则划为真灵候选
                zhenlingcnt++;//真灵候选数量+1
            }
            //若该玩家已经死亡，则continue
            if(gs.gc[i].dieDay != 0) continue;
            //若该玩家声称是猎人且非狼，则划为真猎候补
            if(gs.gc[i].claimedRole == 3 && gs.gc[i].actualRole != 7 && !gs.gc[i].nonHumanMarker)
            {
                islie[i] = true;//若该玩家声称是猎人且非狼，则划为真猎候选
                zhenliecnt++;//真猎候选数量+1
            }
            //若该玩家声称是占卜师且非狼且该玩家没有发过人狼白球和非人狼黑球，则划为真占候补
                //若该玩家声称职业是占卜师且该玩家不是人狼
            if(gs.gc[i].claimedRole == 1 && gs.gc[i].actualRole != 7 && !gs.gc[i].nonHumanMarker)
            {
               //检查该占发球是否有误
                boolean ok = true;
                for(int j=1;j<=gs.gameDay;j++)
                {
                    //若该玩家在第i天发过人狼白球或非人狼黑球，则该占发球有误
                    if(gs.gc[i].skillTarget[j] > gs.getPlayerSum()) //黑球结果
                    {
                        if(gs.gc[i].actualRole != 7)
                        {
                            ok = false;//若该玩家在第j天发过非人狼黑球，则该占发球有误
                            break;
                        }
                    }
                    else //白球结果
                    {
                        if(gs.gc[i].actualRole == 7)
                        {
                            ok = false;//若该玩家在第j天发过人狼白球，则该占发球有误
                            break;
                        }
                    }
                }
                if(!ok) continue;//若该玩家发过人狼白球或非人狼黑球，则该占发球有误，continue
                iszhan[i] = true;//若该玩家声称是占卜师且非狼，则划为真占候选
                zhenzhancnt++;//真占候选数量+1
            }
        }

        //3.5潜伏猎人相关逻辑
        double k;//潜伏猎人折旧系数
        int hiddenHunterScheduledSkillTargetscnt = 0;//潜伏猎人预告人数
        switch(zhenliecnt)
        {
            case 0: k = 0.8; break;
            case 1: k = 0.5; break;
            case 2: k = 0.3; break;
            case 3: k = 0.1; break;
            default: k = 0;  break;
        }
        //处理白色玩家当中当前活着的不带职业的人数和总的不带职业的人数,计算真猎人还存活的概率
        int wuco = 0,alivewuco = 0;
        for (int i=1;i<=gs.getPlayerSum();i++)
        {
            if(gs.gc[i].actualRole == 7 || Math.abs(gs.gc[i].claimedRole - 3) < 3)  continue;//若当前玩家是人狼或者是带职业的村侧，则退出
            wuco++;
            if(gs.gc[i].whyDie == whyDie.NONE)
                alivewuco++;
        }
        k *= alivewuco;
        k /= max(wuco,1);//避免除以0
        //若当前已经喊过猎了，则潜伏猎人折旧系数强制为0.
        if(claimedRoleaskday[3] > 0)
            k = 0;
        for(int i=1;i<=gs.getPlayerSum();i++)
        {
            //若该玩家是潜伏猎人的预告，则预告人数+1
            if(gs.hiddenHunterScheduledSkillTargets[i][gs.getPlayerSum()]) hiddenHunterScheduledSkillTargetscnt++;//潜伏猎人预告人数+1
        }
        for(int i=1;i<=gs.getPlayerSum();i++)
        {
           //若该玩家是潜伏猎人的预告，则被咬权重减少200/预告人数*潜伏猎人折旧系数
            if(gs.hiddenHunterScheduledSkillTargets[i][gs.getPlayerSum()])
            {
                biteWeight[i] -= 200.0 / (double)hiddenHunterScheduledSkillTargetscnt * k;//被咬权重减少200/预告人数*潜伏猎人折旧系数
            }
        }

        //4,遍历所有玩家，对被咬权重进行调整
        for(int i=1;i<=gs.getPlayerSum();i++)
        {
            //本体职业相关逻辑
                //若该玩家是真占候选，则被咬权重+200 -真占候补数*75 +50*y - 占候补数*20
            if(iszhan[i]) biteWeight[i] += 200 - zhenzhancnt * 75 + 50 * y - zhans.size() * 20;
                //若该玩家是真灵候选，则被咬权重+150-真灵候补数*100-灵候补数*70
            if(isling[i]) biteWeight[i] += 175 - zhenlingcnt * 100 - lings.size() * 70;
                //若该玩家是co出来的共有者，则被咬权重+75 + day * 15
            if(gs.gc[i].actualRole == 4 && gs.gc[i].claimedRole == 4) biteWeight[i] += 75 + gs.gameDay * 15;
                //若暂时没有出现猎人候补，则未声称职业者被咬权重+10
            if(zhenliecnt == 0 && gs.gc[i].claimedRole == 0) biteWeight[i] += 10;
                //若该玩家是猫又候补，则被咬权重-80
            if(gs.gc[i].claimedRole == 5) biteWeight[i] -= 80;

            //怀疑度相关逻辑：怀疑前三每有一名人狼，则被咬权重增加
            //怀疑度前三当中有1，2,3名人狼时，被咬权重会增加15+20z,30+20z,50+20z。
            int wolfcaught = 0;//被抓包的人狼数
            for(int j=1;j<=3;j++)
            {
                //logicTools.log("gs.gc[i].top3SuspectedPlayers[j][gs.gameDay]:" + gs.gc[i].top3SuspectedPlayers[j][gs.gameDay]);
                if(gs.gc[i].top3SuspectedPlayers[j][gs.gameDay] != 0 && gs.gc[gs.gc[i].top3SuspectedPlayers[j][gs.gameDay]].actualRole == 7) wolfcaught ++;//若怀疑第j名玩家,j是人狼，则被咬权重+5
            }
            switch(wolfcaught)
            {
                case 1:biteWeight[i] += 15 + 20 * z; break;
                case 2:biteWeight[i] += 30 + 20 * z; break;
                case 3:biteWeight[i] += 50 + 20 * z; break;
            }
            //占卜结果相关逻辑
                //若该人是占卜师co者
            if(gs.gc[i].claimedRole == 1)
            {
                //统计占卜师候补的有效白球数
                for(int j=1;j<gs.gameDay;j++)
                {
                    //白色结果
                    if(gs.gc[i].skillTarget[j] <= gs.getPlayerSum() && gs.gc[i].skillTarget[j]  > 0 && !gs.gc[gs.gc[i].skillTarget[j]].nonHumanMarker
                            && gs.gc[gs.gc[i].skillTarget[j]].claimedRole != 1 && !isackwhite(gs.gc[i].skillTarget[j]))
                    //占卜出白结果，并且对方不是破绽非人、占候补、确定白
                    {
                        baiqiu[i] ++;//此人的有效白球数+1
                    }
                }

                //真占候补
                if(iszhan[i])
                {
                    //真占白球被咬权重+25+2*有效白球数
                    for(int j=1;j<gs.gameDay;j++)
                    {
                        //白色结果，且不是人狼,且没死
                        if(gs.gc[j].whyDie == whyDie.NONE && gs.gc[i].skillTarget[j] <= gs.getPlayerSum() && gs.gc[i].actualRole != 7)
                        {
                            biteWeight[gs.gc[i].skillTarget[j]] += 45 + 2 * baiqiu[i] + 10*y;//真占的白球，被咬权重+45+2*白球数+10*y
                        }
                    }
                    //真占的预告被咬权重+10
                    for(int j=1;j<=gs.getPlayerSum();j++)
                    {
                        //预告对象，没死，不是狼
                        if(gs.gc[j].whyDie == whyDie.NONE && gs.gc[i].claimedRoleScheduledSkillTargets[j][gs.getPlayerSum()] && gs.gc[j].actualRole != 7)
                            biteWeight[j] += 15;//真占的预告，被占玩家j，被咬权重+15
                    }
                }
                //狼占
                else if(gs.gc[i].actualRole == 7)
                {
                    //狼占白球被咬权重+10+2*有效白球数
                    for(int j=1;j<gs.gameDay;j++)
                    {
                        if(gs.gc[i].skillTarget[j] < 1) continue;//非法值
                        //白色结果，且不是人狼,且没死
                        if(gs.gc[i].skillTarget[j] <= gs.getPlayerSum() && gs.gc[gs.gc[i].skillTarget[j]].whyDie == whyDie.NONE && gs.gc[i].actualRole != 7)
                        {
                            biteWeight[gs.gc[i].skillTarget[j]] += 25 + 2 * baiqiu[i];//狼占的白球，被咬权重+25
                        }
                        //黒色结果，且没有co职业,且没死
                        if(gs.gc[i].skillTarget[j] > gs.getPlayerSum()
                                && gs.gc[gs.gc[i].skillTarget[j]-gs.getPlayerSum()].whyDie == whyDie.NONE)
                        {
                           if( gs.gc[gs.gc[i].skillTarget[j]-gs.getPlayerSum()].claimedRole  == 6)
                                biteWeight[gs.gc[i].skillTarget[j]-gs.getPlayerSum()] -= 200;//狼占黑球且无co，被咬权重-200
                            else
                               biteWeight[gs.gc[i].skillTarget[j]-gs.getPlayerSum()] -= 100;//狼占黑球且有职业co，被咬权重-100
                        }
                    }

                    //狼占的预告被咬权重+10
                    for(int j=1;j<=gs.getPlayerSum();j++)
                    {
                        //预告对象，没死，不是狼
                        if(gs.gc[j].whyDie == whyDie.NONE && gs.gc[i].claimedRoleScheduledSkillTargets[j][gs.gameDay] && gs.gc[j].actualRole != 7)
                            biteWeight[j] += 15;//狼占的预告，被占玩家j，被咬权重+15
                    }
                }
                //白非占
                else
                {
                    //白非占的白球被咬权重+5+有效白球数
                    for(int j=1;j<=gs.gameDay;j++)
                    {
                        if(gs.gc[i].skillTarget[j] < 1) continue;//非法值
                        //之前的占卜对象应该存活
                        if(gs.gc[j].whyDie != whyDie.NONE) continue;
                        //白色结果，且不是人狼，且没死
                        if(gs.gc[i].skillTarget[j] <= gs.getPlayerSum() && gs.gc[i].actualRole != 7 && gs.gc[gs.gc[i].skillTarget[j]].whyDie == whyDie.NONE)
                        {
                            biteWeight[gs.gc[i].skillTarget[j]] += 15 + baiqiu[i];//白非占的白球，被咬权重+15+有效白球数
                        }
                        //黒色结果，且没有co职业,且没死
                        if(gs.gc[i].skillTarget[j] > gs.getPlayerSum()
                                && gs.gc[gs.gc[i].skillTarget[j]-gs.getPlayerSum()].whyDie == whyDie.NONE)
                        {
                            if( gs.gc[gs.gc[i].skillTarget[j]-gs.getPlayerSum()].claimedRole  == 6)
                                biteWeight[gs.gc[i].skillTarget[j]-gs.getPlayerSum()] -= 50;//狼占黑球且无co，被咬权重-50
                            else
                                biteWeight[gs.gc[i].skillTarget[j]-gs.getPlayerSum()] -= 25;//狼占黑球且有职业co，被咬权重-25
                        }
                        //每有一个假白（围狼），白非占卜被咬概率+15
                        if(gs.gc[j].actualRole == 7)
                        {
                            biteWeight[i] += 15;//每有一个假白（围狼），白非占卜被咬概率+15
                        }
                    }
                }

            }

            //猎人守卫相关逻辑
            if(islie[i])
            {
                //若该玩家是真猎候选，则被咬权重+350-真猎候补数*100
                //若当前喊过猎，则被咬权重还要增加50
                biteWeight[i] += 350 - zhenliecnt * 100;
                if(claimedRoleaskday[3] > 0)
                    biteWeight[i] += 50;
                int huweicnt = 0;//此猎护卫预告人数
                //统计此猎护卫预告人数
                for(int j=1;j<=gs.getPlayerSum();j++)
                {
                    if(gs.gc[i].claimedRoleScheduledSkillTargets[j][gs.gameDay]) huweicnt++;//若j是i的护卫预告，则此猎护卫预告人数+1
                }
                for(int j=1;j<=gs.getPlayerSum();j++)
                {
                    if(gs.gc[i].claimedRoleScheduledSkillTargets[j][gs.gameDay]) biteWeight[j] -= 200/(double)huweicnt;
                    //若j是i的护卫预告，则被占玩家j，被咬权重-200/此猎护卫预告人数
                }
            }
        }

        //4.5，接多个白球的玩家被咬权重额外上升
        int getWhite[] = new int[gs.getPlayerSum() + 1];
        for(int i=0;i<zhans.size();i++)
        {
            int zhan = zhans.get(i);
            for(int j=1;j<=gs.gameDay;j++)
            {
                int target = gs.gc[zhan].skillTarget[j];
                if(target < 1 || target > gs.getPlayerSum()) continue;
                getWhite[target]++;
            }
        }
        for(int i=1;i<=gs.getPlayerSum();i++)
        {
            biteWeight[i] += getWhite[i] * 5 + getWhite[i] * getWhite[i] * 2;
        }
        //5,给出主咬狼和被咬玩家
            //选出主咬狼
        int wolfbite[] = new int[gs.getPlayerSum()+1];
        for(int i=1;i<=gs.getPlayerSum();i++)
        {
            //若该玩家是存活人狼，则权重为1，否则权重为-INF
            if(gs.gc[i].actualRole != 7 || gs.gc[i].whyDie != whyDie.NONE) wolfbite[i] = -INF;
            else if(gs.gc[i].nonHumanMarker) wolfbite[i] = INF;
            else wolfbite[i] = 1;//优先选择破绽的人狼主咬
        }
        int bitewolf = getOne(wolfbite);//得到主咬狼
        int biteone = getOne(biteWeight);//得到被咬玩家
        return new int[]{bitewolf, biteone};//返回人狼咬杀逻辑二元组
    }
    private void printtop3SuspectedPlayers()  //输出当前的怀疑情况
    {
        logicTools.log("当前怀疑值情况：");

    }
    private boolean isackwhite(int num)
    {
        //判断玩家数组中的对应位置的玩家是否是确定白
        //参数：玩家编号num
        //返回值：true为是，false为否
        //工作：判断玩家是否是确定白
        //返回值：若该玩家是co出来的共有者，或该玩家co猫并且之前没有双死并且当前已经喊过猫
        //理论上可以实现唯一占和唯一灵的确定白逻辑（初日喊占喊灵+共欠），这里简化处理
        if(num < 1) return false;//数组越界
        if(gs.gc[num].claimedRole == 4) return true;//co出来的共有者
        if(gs.gc[num].actualRole != 5 || claimedRoleaskday[5] < 1) return false;//不是猫，或者没有喊猫，就不能当确定白

        //首先排除有其他猫候补（没有破绽）的情况
        ArrayList<Integer> mao = maos;//得到当前的所有猫候补
        for(int i = 0;i < mao.size();i++)
        {
            if(gs.gc[mao.get(i)].nonHumanMarker)
                mao.remove(i);//删除已经破绽的猫候补
        }
        if(mao.size() > 1 || (mao.size() == 1 && mao.get(0) != num) ) return false;//若存在其他未破绽的猫候补，则返回false

        //找到第一次出现多死的时点
        int firsttimemoredie = -1;
        for(int i=1;i<=gs.gameDay;i++)
        {
            if(isDoubleDeathOccurred[i] == true)
            {
                firsttimemoredie = i;
                break;
            }
        }
        if(firsttimemoredie != -1 && firsttimemoredie < claimedRoleaskday[5]) return false;//首次多死在喊猫之前，返回false

        //更改怀疑值
        lasySuspicionValue[num] -= INF;
        logicTools.log("玩家"+num+" 确定猫");
        return true;//首次多死在喊猫当天或之后，或者没有多死，返回true
    }
    private int zhenlie(int num)//返回真猎人当晚守护目标，若真猎死亡则返回-1或0
    {
        //CharacterEnglishName ch1;
        //CharacterKatakanaName ch2 = CharacterKatakanaName.values()[ch1.ordinal()];
        //猎人工作逻辑
        if(gs.gc[num].whyDie != whyDie.NONE) return 0;//猎人死亡，返回0
        
        {
            logicTools.log("猎人编号："+num+" 当前游戏日期："+gs.gameDay);
            System.out.print("这个猎的预告：");
            for(int i=1;i<=gs.getPlayerSum();i++)
            {
                if(gs.gc[num].claimedRoleScheduledSkillTargets[i][gs.gameDay])
                    System.out.print(i+" ");
            }
            logicTools.log("");
            logicTools.log("潜伏猎人的预告：");
            for(int i=1;i<=gs.getPlayerSum();i++)
            {
                if(gs.hiddenHunterScheduledSkillTargets[i][gs.gameDay])
                    System.out.print(i+" ");
            }
            logicTools.log("");
        }
        boolean haveterget = false;//判断当前是否有指定护卫对象
        boolean qf = false;//真猎人当前是否潜伏
        int huweiweight[] = new int[gs.getPlayerSum()+1];//护卫权重数组
        huweiweight[num]  = -INF;
        if(gs.gc[num].claimedRole != 3) qf = true;
        //1,判断当前是否存在指定护卫对象
        for(int i=1;i<=gs.getPlayerSum();i++)
        {
            //若该人已经死亡，或者该人是猎人自己，则continue
            if(gs.gc[i].whyDie != whyDie.NONE || num == i)
            {
                continue;
            }
            //对于潜伏猎人和明猎人，若当前角色不是指定护卫对象
            if((qf == false && gs.gc[num].claimedRoleScheduledSkillTargets[i][gs.gameDay] == false)
                    || (qf == true && gs.hiddenHunterScheduledSkillTargets[i][gs.gameDay] == false))
            {
                continue;
            }
            haveterget = true;//当前有指定护卫对象
            
                logicTools.log("当前猎人编号："+num+",指定护卫对象：" + i);
            break;
        }

        //2,根据当前是否存在指定护卫对象，以及猎人是否潜伏，赋予护卫权重初值
        //存在护卫对象
        if(haveterget)
        {
            if(!qf)//有护卫对象，明猎人
                for(int i=1;i<=gs.getPlayerSum();i++)
                {
                    if(gs.gc[i].whyDie != whyDie.NONE || gs.gc[num].claimedRoleScheduledSkillTargets[i][gs.gameDay] == false)
                        huweiweight[i] = -INF;//若该人已经死亡或者该人没有被指定护卫，则初值-INF
                    else
                        huweiweight[i] = 40;//若该人没有死亡并且被指定护卫，则初值40
                }
            else //有护卫对象，潜伏猎人
            {
                for(int i=1;i<=gs.getPlayerSum();i++)
                {
                    if(gs.gc[i].whyDie != whyDie.NONE || gs.hiddenHunterScheduledSkillTargets[i][gs.gameDay] == false)
                        huweiweight[i] = -INF;//若该人已经死亡或者该人没有被指定护卫，则初值-INF
                    else
                        huweiweight[i] = 40;//若该人没有死亡并且被指定护卫，则初值40
                }
            }
        }
        else//不存在护卫对象,潜伏猎人或者明猎人
        {
             for(int i=1;i<=gs.getPlayerSum();i++)
             {
                 if(gs.gc[i].whyDie != whyDie.NONE || i == num)
                        huweiweight[i] = -INF;//若该人已经死亡，或者该人是自己，则初值-INF
                    else
                        huweiweight[i] = 40;//若该人没有死亡，则初值40
             }
        }

        //3,根据护卫权重初值，更新护卫权重:职业、职业发的球，破绽非人
        //获取当前存活的村侧职业co者名单(占灵共)
        ArrayList zhan = getclaimedRole(1,false),ling = getclaimedRole(2,false),
                gong = getclaimedRole(4,false);
        for(int i=0;i<zhan.size();i++)//占
        {
            int j = (int)zhan.get(i);
            huweiweight[j] += 125 - 25*zhan.size();//占，护卫权重+125-25*当前占的人数
            //占卜发球相关逻辑
            for(int k=1;k<gs.gameDay;k++)
            {
                if(gs.gc[j].skillTarget[k] > gs.getPlayerSum())
                {
                    //黑结果
                    huweiweight[gs.gc[j].skillTarget[k] - gs.getPlayerSum()] -= 40;//接黒，护卫权重-40
                    if(gs.gc[j].skillTarget[k] - gs.getPlayerSum() == num)
                        huweiweight[j] -= 200;//给真猎发黑的假占，护卫权重-200
                }
                else if(gs.gc[j].skillTarget[k] > 0)
                {
                    //白结果
                    huweiweight[gs.gc[j].skillTarget[k]] += 40;//接白，护卫权重+10
                }
            }
        }
        for(int i=0;i<ling.size();i++)//灵
        {
            int j = (int)ling.get(i);
            huweiweight[j] += 75 - 25*ling.size();//灵，护卫权重+75-25*当前灵的人数

        }
        for(int i=0;i<gong.size();i++)//共
        {
            int j = (int)gong.get(i);
            huweiweight[j] += 80;//共，护卫权重+80
        }

        //唯一占和唯一灵相关的逻辑
        if(zhan.size() == 1)
            huweiweight[(int)zhan.get(0)] += 50;//唯一占，护卫权重+50
        if(ling.size() == 1)
            huweiweight[(int)ling.get(0)] += 50;//唯一灵，护卫权重+25

        //公共视角破绽非人或者猎人视角破绽非人,护卫权重-200
        for(int i=1;i<=gs.getPlayerSum();i++)
        {
            if(gs.gc[i].nonHumanMarker || gs.gc[num].suspicionValue[i] > INFJ)
                huweiweight[i] -= 200;//公共视角破绽非人或者猎人视角破绽非人，护卫权重-200
        }

        //4，根据怀疑度更新护卫权重
        //自己视角按照怀疑度排序，所有玩家被护卫权重改变值构成等差数列，公差为5，怀疑度最高的玩家的被护卫权重+0
        ArrayList<Integer> suspectorder = getpriority(gs.gc[num].suspicionValue,true);
        for(int i=1;i<=gs.getPlayerSum();i++)
        {
            huweiweight[i] += 5*suspectorder.get(i);//怀疑度最高的玩家的被护卫权重+0，其他玩家被护卫权重改变值构成等差数列，公差为5
        }

        //5,得到最终的护卫对象
        gs.gc[num].skillTarget[gs.gameDay] = getOne(huweiweight);
        
            logicTools.log("最终护卫对象："+ CharacterKanjiName.values()[gs.gc[gs.gc[num].skillTarget[gs.gameDay]].number]);
        return gs.gc[num].skillTarget[gs.gameDay];
    }
    private ArrayList<Integer> getpriority(int array[],boolean maxfirst)//排序辅助函数，返回对应下标数据之位次 0最小值编号为1 1最大值编号为1
    {
        // 初始化返回的ArrayList，索引0默认占位0
        ArrayList<Integer> result = new ArrayList<>();

        // 入参校验：数组为null或长度<2（无有效元素），直接返回仅含0的ArrayList
        if (array == null || array.length < 2) {
            result.add(0); // 索引0占位
            return result;
        }

        // 步骤1：提取有效元素（array[1]到array[array.length-1]），存入临时数组
        int validLen = array.length - 1; // 有效元素个数（索引1开始）
        int[] valArr = new int[validLen]; // 存储有效元素的值
        int[] idxArr = new int[validLen]; // 存储有效元素的原始索引（与valArr一一对应）
        for (int i = 0; i < validLen; i++)
        {
            valArr[i] = array[i + 1];    // 对应array[1],array[2]...
            idxArr[i] = i + 1;           // 对应原始索引1,2...
        }

        // 步骤2：冒泡排序（基础排序），同步排序valArr和idxArr，保证索引与值的对应
        for (int i = 0; i < validLen - 1; i++) { // 外层：排序轮数
            for (int j = 0; j < validLen - 1 - i; j++) { // 内层：每轮比较次数
                boolean needSwap = false;
                if (maxfirst) {
                    // maxfirst=true：降序（大值在前），前 < 后则交换
                    needSwap = valArr[j] < valArr[j + 1];
                } else {
                    // maxfirst=false：升序（小值在前），前 > 后则交换
                    needSwap = valArr[j] > valArr[j + 1];
                }
                // 同步交换值数组和索引数组
                if (needSwap) {
                    // 交换valArr
                    int tempVal = valArr[j];
                    valArr[j] = valArr[j + 1];
                    valArr[j + 1] = tempVal;
                    // 交换idxArr（保证索引与值对应）
                    int tempIdx = idxArr[j];
                    idxArr[j] = idxArr[j + 1];
                    idxArr[j + 1] = tempIdx;
                }
            }
        }

        // 步骤3：计算位次（处理并列值），存入rankArr
        int[] rankArr = new int[validLen];
        if (validLen == 0) { // 极端情况：无有效元素（已提前校验，此处为冗余防御）
            result.add(0);
            return result;
        }
        int currentRank = 1; // 初始位次为1
        int currentVal = valArr[0]; // 基准值（排序后第一个元素的值）
        rankArr[0] = currentRank;   // 第一个元素位次为1
        for (int i = 1; i < validLen; i++) {
            if (valArr[i] != currentVal) {
                // 值不同，更新位次为当前索引+1（索引从0开始）
                currentRank = i + 1;
                currentVal = valArr[i]; // 更新基准值
            }
            // 值相同则沿用当前位次，不同则用更新后的位次
            rankArr[i] = currentRank;
        }

        // 步骤4：构建返回的ArrayList（索引0占位0，索引1开始填充位次）
        // 先初始化所有位置为0（长度与入参数组一致）
        for (int i = 0; i < array.length; i++) {
            result.add(0);
        }
        // 按原始索引，将位次填充到对应位置
        for (int i = 0; i < validLen; i++) {
            int originalIndex = idxArr[i]; // 元素的原始索引（1开始）
            int rank = rankArr[i];         // 对应的位次
            result.set(originalIndex, rank); // 赋值到ArrayList的对应索引
        }

        return result;

    }
    private ArrayList<Integer> getclaimedRole(int claimedRolenum, boolean mustalive)//得到某个职业的所有候补（可以要求必须存活）的辅助函数
    {
        //参数：村侧职业编号claimedRolenum,是否必须存活mustalive
        //返回值：所有co出来的符合条件的职业(不排除明确破绽的)
        //工作：根据职业编号和是否必须存活，返回所有符合条件的玩家编号数组

        ArrayList<Integer> array = new ArrayList<Integer>();

        // 枚举所有玩家
        for (int j = 1; j <= gs.getPlayerSum(); j++) {
            if (gs.gc[j].claimedRole == claimedRolenum) {
                if (mustalive) {
                    if (gs.gc[j].whyDie == whyDie.NONE) {
                        array.add(j); // 直接添加玩家编号
                    }
                } else {
                    array.add(j); // 直接添加玩家编号
                }
            }
        }

        // 返回 ArrayList
        return array;
    }
    private void updatetop3SuspectedPlayers()//每天更新所有玩家的怀疑度：依据听感逻辑。
    {
        //参数：无
        //无返回值,直接改动玩家的怀疑度数组
        //工作：根据听感逻辑，更新所有玩家的怀疑度。
        //枚举所有玩家
        for(int i=1;i<=gs.getPlayerSum();i++)
        {
            if(gs.gc[i].whyDie != whyDie.NONE) continue;//死人的怀疑度无意义
            for(int j=1;j<=gs.getPlayerSum();j++)
            {
                if(gs.gc[j].whyDie != whyDie.NONE) continue;//死人的怀疑度无意义
                int p0 = ConstNum.randomInt(1, 100);
                if (i == j)
                    gs.gc[i].suspicionValue[j] = -INF;//对于自身，固定-INF怀疑度
                else
                {
                    switch(gs.gc[j].claimedRole)
                    {
                        //对于职业co者，随时间增加怀疑度
                        //占灵猎 5 10 15
                        case 1:gs.gc[i].suspicionValue[j] += 5 + 5 * zhans.size();break;
                        case 2:gs.gc[i].suspicionValue[j] += 10 + 10 * lings.size();break;
                        case 3:gs.gc[i].suspicionValue[j] += 15 + 5 *lies.size();break;
                    }
                    gs.gc[i].suspicionValue[j] += lasySuspicionValue[j];
                    if (zhenying(gs.gc[i]) == 0)
                    {
                        //村侧对他人的怀疑
                        int fr = feiren(gs.gc[j]);
                        if(fr == 0)
                        {
                            //村-村
                            if(p0 <= 10) gs.gc[i].suspicionValue[j] -= 15;
                            else if(p0 <= 12) gs.gc[i].suspicionValue[j] += 15;
                            else gs.gc[i].suspicionValue[j] += ConstNum.randomInt(-3,2);
                        }
                        else if(fr == -1)
                        {
                            //村-白非
                            if(p0 <= 5) gs.gc[i].suspicionValue[j] -= 15;
                            else if(p0 <= 10) gs.gc[i].suspicionValue[j] += 15;
                            else gs.gc[i].suspicionValue[j] += ConstNum.randomInt(-2,2);
                        }
                        else
                        {
                            //村-狼
                            if(p0 <= 2) gs.gc[i].suspicionValue[j] -= 15;
                            else if(p0 <= 12) gs.gc[i].suspicionValue[j] += 15;
                            else gs.gc[i].suspicionValue[j] += ConstNum.randomInt(-2,3);
                        }
                    }
                    else
                    {
                        //非人对他人的怀疑
                        if(p0 <= 5) gs.gc[i].suspicionValue[j] -= 15;
                        else if(p0 <= 10) gs.gc[i].suspicionValue[j] += 15;
                        else gs.gc[i].suspicionValue[j] += ConstNum.randomInt(-2,2);
                    }
                    //数值修正
                    if(gs.gc[i].suspicionValue[j] > INFJ)  gs.gc[i].suspicionValue[j] = INF;
                    else if(gs.gc[i].suspicionValue[j] < -INFJ) gs.gc[i].suspicionValue[j] = -INF;
                    else if(gs.gc[i].suspicionValue[j] > MAXN) gs.gc[i].suspicionValue[j]=  MAXN;
                    else if(gs.gc[i].suspicionValue[j] < 0) gs.gc[i].suspicionValue[j] = 0;
                }
            }
        }
        //占候补白球存活增加怀疑
        for(int i=0;i<zhans.size();i++)
        {
            for(int j=1;j<=gs.gameDay;j++)
                if(gs.gc[zhans.get(i)].skillTarget[j] <= gs.getPlayerSum() &&
                        gs.gc[zhans.get(i)].skillTarget[j] > 0 && gs.gc[gs.gc[zhans.get(i)].skillTarget[j]].whyDie == whyDie.NONE)
                    for(int k=1;k<= gs.getPlayerSum();k++)
                    {
                        if(k == zhans.get(i)) continue;
                        gs.gc[k].suspicionValue[j] ++;//所有玩家对任何占卜师的任何白球怀疑度每天都增加1,除开占卜师本人以外
                        if(gs.gc[k].claimedRole == 1)
                            gs.gc[k].suspicionValue[j] ++;//其他占卜师对任何占卜师的任何白球怀疑度每天都增加1
                    }
        }

        //重置怀疑度更新懒惰数组
        for(int i=1;i<=gs.getPlayerSum();i++)
            lasySuspicionValue[i] = 0;
        //怀疑前三的完全重新选取
            //枚举每个玩家
        for(int i=1;i<=gs.getPlayerSum();i++)
        {
            if(gs.gc[i].whyDie != whyDie.NONE) continue;//跳过已死亡玩家
            for(int j=1;j<=3;j++)
            {
                //每次选取一个下标
                int maxindex = 0;
                for(int k=1;k<=gs.getPlayerSum();k++)
                {
                    if(i == k || gs.gc[k].whyDie != whyDie.NONE) continue;//已死亡玩家和自己被排除
                    if(maxindex == 0 || gs.gc[i].suspicionValue[maxindex] < gs.gc[i].suspicionValue[k]) //满足选取大小关系
                    {
                        boolean selected = false;//选取过标记
                        for(int l=1;l<=j-1;l++)
                            if(k == gs.gc[i].top3SuspectedPlayers[l][gs.gameDay])
                                selected = true;
                        if(selected) continue;
                        maxindex = k;
                    }
                }
                gs.gc[i].top3SuspectedPlayers[j][gs.gameDay] = maxindex;//更新最受怀疑的玩家
            }
        }
    }
    private int zhenying(GameCharacter gc)//得到游戏角色对应的阵营
    {
        //-1 狐侧 0 村侧 1 狼侧
        switch(gc.actualRole)
        {
            case 7: case 8: case 9://狼 狂 狂信
                return 1;
            case 10: case 11://狐 背
                return -1;
            default:
                return 0;
        }
    }
    private int feiren(GameCharacter gc)//得到游戏角色关于非人的三分类
    {
        //-1白非人 0 村侧 1人狼
        switch(gc.actualRole)
        {
            case 7: //狼
                return 1;
            case 10: case 11:case 8: case 9://狐 背 狂 狂信
                return -1;
            default:
                return 0;
        }
    }
    private int getOne(int val[]) //通用处理函数：根据权重，随机选择一名玩家 1开始计数
    {
        //参数：权重数组val
        //返回值：随机生成的玩家编号
        //工作：根据权重数组，随机生成一个玩家编号。权重正比于被选中的概率，若权重小于等于0，则永远不可能被选中。
        //特殊：若所有权重都小于等于0，则将最大和次大权重等量增加，使得次大权重为5
        //（若次大权重为非法值，则直接返回最大对应的玩家，次大权重不会增加）
        
        // 1. 获取玩家数量并校验合法性
        int playerNum = gs.getPlayerSum();
        if (playerNum < 1) {
            throw new IllegalArgumentException("玩家数量不能小于1，当前数量：" + playerNum);
        }
        if (val == null || val.length < playerNum + 1) {
            throw new IllegalArgumentException("权重数组为空或长度不足，需至少包含" + (playerNum + 1) + "个元素");
        }

        // 2. 遍历找到最大/次大权重及其对应的玩家编号
        int maxVal = Integer.MIN_VALUE;   // 最大权重值
        int maxIdx = -1;                  // 最大权重对应的玩家编号
        int secondVal = Integer.MIN_VALUE;// 次大权重值
        int secondIdx = -1;               // 次大权重对应的玩家编号

        for (int i = 1; i <= playerNum; i++) {
            int currWeight = val[i];
            // 若当前权重大于最大值，更新次大/最大值
            if (currWeight > maxVal) {
                secondVal = maxVal;
                secondIdx = maxIdx;
                maxVal = currWeight;
                maxIdx = i;
            }
            // 若当前权重不大于最大值，但大于次大值，更新次大值
            else if (currWeight > secondVal) {
                secondVal = currWeight;
                secondIdx = i;
            }
        }

        // 3. 创建临时权重数组（避免修改原数组的副作用）
        int[] tempWeights = new int[playerNum + 1];
        System.arraycopy(val, 1, tempWeights, 1, playerNum);

        // 4. 处理「所有权重≤0」的特殊场景
        boolean isAllNonPositive = maxVal <= 0;
        if (isAllNonPositive) {
            // 次大权重为非法值（< -INFJ），直接返回最大权重玩家
            if (secondVal < -INFJ) {
                return maxIdx;
            }
            // 等量增加最大/次大权重，使次大权重为5
            int delta = 5 - secondVal;
            tempWeights[maxIdx] = maxVal + delta;
            tempWeights[secondIdx] = 5; // 次大权重固定为5
        }

        // 5. 计算正权重总和（仅正权重参与随机选择）
        int totalWeight = 0;
        for (int i = 1; i <= playerNum; i++) {
            if (tempWeights[i] > 0) {
                totalWeight += tempWeights[i];
            }
        }
        // 理论上经过特殊处理后总权重必>0，兜底校验
        if (totalWeight <= 0) {
            throw new IllegalStateException("无有效正权重玩家，无法随机选择");
        }

        // 6. 生成随机数并按权重占比选择玩家
        int randomNum = ConstNum.randomInt(0,totalWeight-1);
        int cumulativeWeight = 0; // 累加权重
        for (int i = 1; i <= playerNum; i++) {
            int weight = tempWeights[i];
            if (weight > 0) {
                cumulativeWeight += weight;
                // 累加权重超过随机数时，选中当前玩家
                if (cumulativeWeight > randomNum) {
                    return i;
                }
            }
        }

        // 兜底返回（理论上不会执行到此处）
        return maxIdx;
    }
    private int zhenzhan(int znum)
    {
        //znum:占卜师编号
        if(gs.gc[znum].whyDie != whyDie.NONE) return -1;//占已经死亡，返回-1
        
        {
            logicTools.log("占卜师编号："+znum+" 当前游戏日期："+gs.gameDay);
            System.out.print("这个占的预告：");
            for(int i=1;i<=gs.getPlayerSum();i++)
            {
                if(gs.gc[znum].claimedRoleScheduledSkillTargets[i][gs.gameDay])
                    System.out.print(i+" ");
            }
            logicTools.log("");
        }
        int weight[] = new int [gs.getPlayerSum()+1];//定义权重数组
        ArrayList target = new ArrayList<Integer>();//定义合法占卜对象数组
        boolean istarget[] = new boolean [gs.getPlayerSum()+1];//定义是否是合法占卜对象数组
        for(int i=1;i<=gs.getPlayerSum();i++)
        {
            istarget[i] = true;//标记所有玩家为合法的占卜对象
        }
        for(int i=1;i<gs.gameDay;i++)
        {
            int ztarget = gs.gc[znum].skillTarget[i];
            if(ztarget > gs.getPlayerSum()) ztarget -= gs.getPlayerSum();
            istarget[ztarget] = false;//标记已经占卜过的玩家为非法占卜对象
        }
        //不能直接修改预告数组，记录需要使用，所以需要引入istarget数组来标记是否是合法的占卜对象
        //流程：首先处理占卜权重初值，若为预告，则初值为50.否则初值为-INF.
        int claimedRoleScheduledSkillTargetssum = 0;//记录合法占卜对象数量，预告为0,1，复数时，采取不同的逻辑
        if(gs.gc[znum].claimedRole == 1)//不是潜伏占
        {
            for(int i=1;i<=gs.getPlayerSum();i++)
            {
                if(!istarget[i]) continue;//不能占卜已经占卜过的玩家
                if(gs.gc[znum].claimedRoleScheduledSkillTargets[i][gs.gameDay] != true)
                {
                    istarget[i] = false;//标记为非法的占卜对象
                    continue;//没出现在预告中，直接退出
                }
                if( (gs.gc[i].whyDie != whyDie.NONE &&
                        (gs.gc[i].dieDay != gs.gameDay || isDayDie(gs.gc[i].whyDie)))
                        || i == znum || isackwhite(i) )
                {
                    //非法的占卜预告：占卜对象已经死亡并且不是当天死亡的（当天死亡的是夜间死亡，也可以被占卜），占卜自己，占卜确定白
                    //标记为非法的占卜对象
                    
                        logicTools.log("排除玩家："+CharacterKanjiName.values()[i]);
                    istarget[i] = false;//标记为非法的占卜对象
                    continue;//退出
                }
                istarget[i] = true;
                claimedRoleScheduledSkillTargetssum++;//增加统计出来的占卜师预告数量
                target.add(i);//添加到合法占卜对象数组中
                
                {
                    logicTools.log("占卜师"+CharacterKanjiName.values()[znum]+"占卜预告："+CharacterKanjiName.values()[i]);
                }
                weight[i] = 50;//若为预告，则初值为50
            }
        }
        else//是潜伏占
        {
            for(int i=1;i<=gs.getPlayerSum();i++)
            {
                if(!istarget[i]) continue;//不能占卜已经占卜过的玩家
                if(gs.hiddenSeerScheduledSkillTargets[i][gs.gameDay] != true)
                {
                    istarget[i] = false;//标记为非法的占卜对象
                    continue;//没出现在预告中，直接退出
                }
                if((gs.gc[i].whyDie != whyDie.NONE && (gs.gc[i].dieDay != gs.gameDay || isDayDie(gs.gc[i].whyDie)) )|| i == znum || isackwhite(i))
                {
                    //非法的占卜预告：占卜对象已经在之前死亡，占卜自己，占卜确定白
                    //标记为非法的占卜对象
                    istarget[i] = false;//标记为非法的占卜对象
                    continue;//退出
                }
                istarget[i] = true;
                claimedRoleScheduledSkillTargetssum++;//增加统计出来的占卜师预告数量
                target.add(i);//添加到合法占卜对象数组中
                weight[i] = 50;//若为预告，则初值为50
            }
        }
        //若为单预告，则直接占卜他。若为妖狐，则妖狐死亡。
        if(claimedRoleScheduledSkillTargetssum == 1)
        {
            int ztarget = (int)target.get(0);//返回唯一的预告
            if(gs.gc[ztarget].actualRole != 7)
                return gs.gc[znum].skillTarget[gs.gameDay] = ztarget;//白结果
            else
                return gs.gc[znum].skillTarget[gs.gameDay] = ztarget + gs.getPlayerSum();//黑结果
        }
        //若没有合法的预告对象，则设置占卜师自己和之前占卜过的人之外的人为合法占卜对象
        if(claimedRoleScheduledSkillTargetssum == 0)
        {
            for(int i=1;i<=gs.getPlayerSum();i++)
            {
                if((gs.gc[i].whyDie != whyDie.NONE && (gs.gc[i].dieDay != gs.gameDay || isDayDie(gs.gc[i].whyDie)) )|| i == znum || isackwhite(i))
                {
                    istarget[i] = false;//标记为非法的占卜对象
                    continue;//退出
                }
                //检查真占每天的技能使用目标是否是此人，若之前有过占卜此人，则此人不是合法占卜对象
                istarget[i] = true;//假设此人是合法的占卜对象
                for (int j = 1; j <= gs.gameDay; j++)
                {
                    if (gs.gc[znum].skillTarget[j] == i || gs.gc[znum].skillTarget[j] == i + gs.getPlayerSum())//黑球白球
                    {//若真占第i天的技能使用目标是此人，且此人不是占卜师自己->此人不是合法的占卜对象
                        istarget[i] = false;//标记为非法的占卜对象
                        break;//跳出检查真占每天的技能使用目标是否是此人的循环
                    }
                }
                if (istarget[i]) //若此人是合法的占卜对象
                {
                    target.add(i);//添加到合法占卜对象数组中
                    weight[i] = 50;//占卜权重初始设置为50
                }
            }
            if(target.size() == 0) return -1;//若没有合法的占卜对象，则返回-1,对应事件：占卜完占(没有合法的占卜对象)
        }
        //此时未设置为合法占卜对象的都必为非法对象
        for(int i=1;i<=gs.getPlayerSum();i++)
        {
            if(weight[i] < 50)
                weight[i] = -INF;//若为非合法占卜对象，则初值为-INF
        }
        //统一处理所有合法的占卜对象
            //1,对方接到对抗球的相关逻辑
        for(int i=1;i<=gs.getPlayerSum();i++)
        {
            if(gs.gc[i].claimedRole == 1 && !gs.gc[i].nonHumanMarker)
            {//若此人是对抗，且此人没破绽
                for(int j=1;j<=gs.gameDay;j++)
                {//遍历该人每天的技能使用目标
                    if(gs.gc[i].skillTarget[j] <gs.getPlayerSum() && istarget[gs.gc[i].skillTarget[j]])//若该人第j天占出合法占卜对象白色，则占卜权重-10
                        weight[gs.gc[i].skillTarget[j]] -= 35;//减少25的权重
                    if(gs.gc[i].skillTarget[j]-gs.getPlayerSum() > 0 && istarget[gs.gc[i].skillTarget[j]-gs.getPlayerSum()])
                        //若该人第j天占出合法占卜对象黒色，则占卜权重-45
                        weight[gs.gc[i].skillTarget[j]-gs.getPlayerSum()] -= 45;//减少40的权重
                }
            }
        }
            //2,对方被怀疑的情况
        int top3SuspectedPlayerscnt[] = new int[gs.getPlayerSum()+1];//记录每个角色被怀疑的次数,"被怀疑"：进入某人的怀疑前三
        for(int i=1;i<=gs.getPlayerSum();i++)
        {
           for(int j=1;j<=3;j++)
           {
               if(istarget[gs.gc[i].top3SuspectedPlayers[j][gs.getPlayerSum()]])
                   top3SuspectedPlayerscnt[gs.gc[i].top3SuspectedPlayers[j][gs.getPlayerSum()]]++;//增加被怀疑的次数
           }
        }
                //找到占卜对象当中怀疑次数最少的次数
        int mincnt = Integer.MAX_VALUE;//记录最小的怀疑次数
        for(int i=1;i<=gs.getPlayerSum();i++)
        {
            if(istarget[i])//若为合法的占卜对象
                mincnt = Math.min(mincnt,top3SuspectedPlayerscnt[i]);//更新最小的怀疑次数
        }
                //所有占卜对象增加对应的权重
        for(int i=1;i<=gs.getPlayerSum();i++)
        {
            if(istarget[i])//若为合法的占卜对象
                weight[i] += top3SuspectedPlayerscnt[i] - mincnt;//增加对应的权重:被怀疑次数-最小怀疑次数
        }
        //对于每个占卜预告或者合法的占卜对象，分配权重
       for(int i=0;i<target.size();i++)
       {
           int num = (int)target.get(i);//获取当前合法的占卜对象编号
           //对方声称的职业相关逻辑
           switch(gs.gc[num].claimedRole)
           {
               case 1://是占卜师
                   weight[num] -= 45;//对抗，减少40的权重
                   break;
               case 2://是灵
                   weight[num] -= 45;//灵媒师候补，减少45的权重
                   break;
               case 3://是猎
               case 5://是猫
                   weight[num] -= 35;//猫猎候补,减少35的权重
                   break;
               default://是平民
                   weight[num] += 0;//增加0的权重
                   break;
           }
       }
       //权重微调
        //获得合法占卜对象的最小权重
        int minweight = Integer.MAX_VALUE;//记录最小的权重
        for(int i=1;i<=gs.getPlayerSum();i++)
        {
            if(istarget[i])//若为合法的占卜对象
                minweight = Math.min(minweight,weight[i]);//更新最小的权重
        }
        //若这个最小权重小于5，则最小权重改为5，并且其他占卜对象的权重也增加对应的差值
        if(minweight < 5)
        {
            int diff = 5 - minweight;//计算差值
            for(int i=1;i<=gs.getPlayerSum();i++)
            {
                if(istarget[i])//若为合法的占卜对象
                    weight[i] += diff;//增加对应的差值
            }
        }
        int ztarget = getOne(weight);
        if(gs.gc[ztarget].actualRole != 7)
            return gs.gc[znum].skillTarget[gs.gameDay] = ztarget;//白结果
        else
            return gs.gc[znum].skillTarget[gs.gameDay] = ztarget + gs.getPlayerSum();//黑结果
    }
    public boolean shokei(int dailyVotingRule, List<Integer> chuxingList,boolean huibi)
    {
        //提供给UI类，主逻辑现在进行投票判定
        //参数：投票方法。 0：自由投票 1：随机灰吊 2：指定投票 回避处刑是否开启
        // 被制定处刑的玩家编号数组
        // 假定给定的数据合法，也就是说指定投票时处刑列表存在存活玩家
        //返回值：true：投票成功，并且得到票型，这之后进行夜间，发送一系列的事件，然后再返回函数shokei
        //false:没有进行投票(存在回避co) true:投票顺利进行

        //0,获取UI类中的gs，补充指定信息

        gs = Game.getInstance().getUI().getGameStatus();

          //显示预告信息
        {
            for(int i=0;i<zhans.size();i++)
            {
                int zhan = zhans.get(i);
                System.out.print("占卜师候补编号：" + gs.gc[zhan].claimedRoleorder+"预告情况：");
                for(int j=1;j<=gs.getPlayerSum();j++)
                    if(gs.gc[zhan].claimedRoleScheduledSkillTargets[j][gs.gameDay])
                        System.out.print(CharacterKanjiName.values()[gs.gc[j].number] + " ");
                logicTools.log("");
            }
            for (int i = 0; i < lies.size(); i++)
            {
                int lie = lies.get(i);
                System.out.print("猎人候补编号：" + gs.gc[lie].claimedRoleorder + "指定护卫情况：");
                for (int j = 1; j <= gs.getPlayerSum(); j++)
                    if (gs.gc[lie].claimedRoleScheduledSkillTargets[j][gs.gameDay])
                        System.out.print(CharacterKanjiName.values()[gs.gc[j].number] + " ");
                logicTools.log("");
            }
            System.out.print("潜伏占预告情况：");
            for(int i=1;i<=gs.getPlayerSum();i++)
            {
                if(gs.hiddenSeerScheduledSkillTargets[i][gs.gameDay])
                        System.out.print(CharacterKanjiName.values()[gs.gc[i].number] + " ");
            }
            logicTools.log("");
            System.out.print("潜伏猎人指定护卫情况：");
            for(int i=1;i<=gs.getPlayerSum();i++)
            {
                if(gs.hiddenHunterScheduledSkillTargets[i][gs.gameDay])
                    System.out.print(CharacterKanjiName.values()[gs.gc[i].number] + " ");
            }
            logicTools.log("");
        }

        //1,非人或真职业回避处刑指定的逻辑
        eventarray.clear();
        if(dailyVotingRule == 2)
        for(int i=0;i<chuxingList.size();i++)
        {
            int num = chuxingList.get(i);
            if(gs.gc[num].whyDie != whyDie.NONE ) continue;//若该人已经死亡，则退出
            if(gs.gc[num].actualRole == 4)   //共有者无论如何，被指定时都会co回避共
            {
                eventarray.add(new Event(EventName.hbg,CharacterEnglishName.values()[gs.gc[num].number]));//回避共
                gs.gc[num].claimedRole = 4;
                continue;
            }
            if(gs.gc[num].claimedRole != 0) continue;//已经明确自己职业的人，不参与回避讨论
            if(huibi)//需要回避开启
            {
                if(gs.gc[num].actualRole < 7)//村侧
                {
                    actualRoleCo(num,gs.gc[num].actualRole);
                    continue;
                }
                if(nonHumanPlan[num] != 0 && nonHumanPlan[num] != 4 && claimedRoleaskday[nonHumanPlan[num]] == 0)//按照计划co,之前没有喊过对应的职业
                {
                    if(claimedRoleaskday[nonHumanPlan[num]] == 0)    //之前喊过对应的职业，则直接退出
                        actualRoleCo(num,nonHumanPlan[num]);
                    else
                        nonHumanPlan[num] = 0;
                    continue;
                }
                if(claimedRoleaskday[3] == 0  || claimedRoleaskday[5] == 0)
                    switch (gs.gc[num].actualRole)//临时co猫猎
                    {
                        case 7:
                            if ((actualRoleindex[3] < 1 || rlsl || claimedRoleaskday[3] > 0) &&
                                    (actualRoleindex[5] < 1 || rlsm || claimedRoleaskday[5] > 0))
                                break;//没有co猫猎的名额
                            if (logicTools.probabilityJudge(probabilityCalculator.maolieco(0, 0, getp1(7), getp2(7))))
                            {
                                //通过判定
                                if (actualRoleindex[3] < 1 || rlsl || claimedRoleaskday[3] > 0) actualRoleCo(num, 5);
                                else if (actualRoleindex[5] < 1 || rlsm || claimedRoleaskday[5] > 0) actualRoleCo(num, 3);
                                else actualRoleCo(num, 5 - 2 * getEventIndexByProbability
                                            (new ArrayList<Integer>(List.of(50 + 50 * lies.size(), 50 + 50 * maos.size()))));
                            }
                            break;
                        default:
                            if (logicTools.probabilityJudge(probabilityCalculator.maolieco
                                    (gs.gc[num].actualRole - 7, 0, getp1(gs.gc[num].actualRole), getp2(gs.gc[num].actualRole))))
                            {
                                //通过判定
                                if (actualRoleindex[3] < 1 || claimedRoleaskday[3] > 0) actualRoleCo(num, 5);
                                else if (actualRoleindex[5] < 1 || claimedRoleaskday[5] > 0) actualRoleCo(num, 3);
                                else actualRoleCo(num, 5 - 2 * getEventIndexByProbability
                                        (new ArrayList<Integer>(List.of(50 + 50 * lies.size(), 50 + 50 * maos.size()))));
                            }
                            break;
                    }
            }
        }
        if(eventarray.size() > 0)
        {
            delieverevent();
            return false;//存在回避处刑事件，处刑失败
        }
        //2,无人回避处刑，投票开始
        boolean votable[] = new boolean[gs.getPlayerSum()+1];//记录每个玩家是否可以被投票
        boolean have_votable = false;//记录当前是否有可以被投票的玩家

          //输出当天的指定信息
        {

        }

        if(dailyVotingRule == 1)
        {
            //随机灰吊
            //排除上职玩家
            for(int i=1;i<=gs.getPlayerSum();i++)
            {
                if(gs.gc[i].whyDie == whyDie.NONE && (gs.gc[i].claimedRole == 6 || gs.gc[i].claimedRole == 0))//没有死亡，并且无co
                {
                    votable[i] = true;//可以被投票
                }
            }
            //排除接了球的玩家
            for(int i=0;i<zhans.size();i++)
            {
                int zhan = zhans.get(i);
                for(int j=1;j<gs.gameDay;j++)
                {
                    int target = gs.gc[zhan].skillTarget[j];
                    if(target > 0)
                    {
                        if(target > gs.getPlayerSum()) votable[target - gs.getPlayerSum()] = false;
                        else votable[target] = false;
                    }
                }
            }
            //再次判断是否有能够被投票的玩家
            for(int i=1;i<gs.getPlayerSum();i++)
            {
                if(votable[i])have_votable = true;
            }
        }
        else if(dailyVotingRule == 2)
        {
            //指定投票,只有被指定的人可以被投票
            for(int i=0;i<chuxingList.size();i++)
            {
                int num = chuxingList.get(i);//获取当前指定的处刑玩家编号
                if(gs.gc[num].whyDie == whyDie.NONE)//没有死亡
                {
                    votable[num] = true;//可以被投票
                    have_votable = true;//存在着可以被投票的玩家
                }
            }
        }
        else if(!have_votable)//自由投票 dailyVotingRule == 0 || !have_votable
        {
            for(int i=1;i<=gs.getPlayerSum();i++)
            {
                if(gs.gc[i].whyDie == whyDie.NONE)//没有死亡
                {
                    votable[i] = true;//可以被投票
                    have_votable = true;//存在着可以被投票的玩家
                }
            }
        }
        int shokeicnt = 1,topnum = 0;//投票轮数计数 票数唯一最多者下标
        while(true)
        {
            topnum = shokeiaux(shokeicnt,votable);//处理被投票候选人的减少在投票辅助函数当中已经实现了
            if(topnum != 0) break;
            if(shokeicnt < 3)
                shokeicnt++;
            //若是前两轮投票，则本次票型会保留，若是第三轮票型，则本次票型作废。
        }
        //最多可以平票两轮
        //保证没有第三轮平票，有第三轮平票的话自动隐式重投
        
        {
            logicTools.log("投票结束，一共投票了" + shokeicnt + "轮" + "\n票型：\n" );
            for(int k=1;k<=shokeicnt;k++)
            {
                logicTools.log("第" + k + "轮投票结果：");
                int beitou[] = new int[gs.getPlayerSum()+1];
                for(int i=1;i<=gs.getPlayerSum();i++)
                {
                    beitou[gs.gc[i].voteTarget[gs.gameDay][k]]++;
                }
                for(int i=1;i<=gs.getPlayerSum();i++)
                {
                    if(gs.gc[i].whyDie != whyDie.NONE)continue;
                    logicTools.log(CharacterKanjiName.values()[gs.gc[i].number]+""+beitou[i]+"票 投票先 "
                            +CharacterKanjiName.values()[gs.gc[gs.gc[i].voteTarget[gs.gameDay][k]].number]);
                }
            }

        }
        //3,处理投票事件当中触发的后追与猫咒
        delieverevent();
        dieaux(topnum,whyDie.chuxing);
        if(topnum == actualRoleindex[5])
        {
            //猫咒逻辑
            if(gs.gc[topnum].claimedRole != 5)
            {
                gs.gc[topnum].claimedRole = 5;
                gs.gc[topnum].comingOutDay = gs.gameDay + 1;
            }
            int mztarget[] = new int[gs.getPlayerSum() + 1];
            for(int i=1;i<=gs.getPlayerSum();i++)
            {
                if(gs.gc[i].whyDie == whyDie.NONE)
                    mztarget[i] = 1;
            }
            int mz = getOne(mztarget);
            dieaux(mz,whyDie.daymaozhou);
            if(mz == actualRoleindex[10] && actualRoleindex[11] > 0 && gs.gc[actualRoleindex[11]].whyDie == whyDie.NONE)
            {
                dieaux(actualRoleindex[11],whyDie.dayhouzhui);
                gs.gc[actualRoleindex[10]].nonHumanMarker = true;
                gs.gc[actualRoleindex[11]].nonHumanMarker = true;
            }
        }
        else if(topnum == actualRoleindex[10] && actualRoleindex[11] > 0 && gs.gc[actualRoleindex[11]].whyDie == whyDie.NONE)
        {
            //后追逻辑
            dieaux(actualRoleindex[11],whyDie.dayhouzhui);
            gs.gc[actualRoleindex[10]].nonHumanMarker = true;
            gs.gc[actualRoleindex[11]].nonHumanMarker = true;
        }
        delieverevent();
        gs.end = judgeend();
        if(gs.end != 0)
        {
            logicTools.log("游戏结束，添加结束事件");
            int weight[] = new int[gs.getPlayerSum()+1];
            for(int i=1;i<=gs.getPlayerSum();i++)
            {
                if(gs.gc[i].whyDie != whyDie.NONE) continue;
                if(gs.end == 1 && gs.gc[i].actualRole < 7) weight[i] = 1;
                else if(gs.end == 2 && gs.gc[i].actualRole == 7) weight[i] = 1;
                else if(gs.end == 3 && gs.gc[i].actualRole == 10) weight[i] = 1;
                else weight[i] = -INF;
            }
            CharacterEnglishName player = CharacterEnglishName.values()[gs.gc[getOne(weight)].number];//获取发表获奖感言的玩家
            switch(gs.end)
            {
                case 1:
                    eventarray.add(new Event(EventName.crsl,player,null));
                    break;//村人胜利
                case 2:
                    if((actualRoleindex[9-kyojin] > 0 && gs.gc[actualRoleindex[9-kyojin]].whyDie == whyDie.NONE))
                        eventarray.add(new Event(EventName.krsl,CharacterEnglishName.values()[gs.gc[actualRoleindex[9-kyojin]].number],null));
                    else
                        eventarray.add(new Event(EventName.rlsl,player,null));
                    break;//人狼或狂人胜利
                case 3:
                    eventarray.add(new Event(EventName.yhsl,player,null));
                    break;//妖狐胜利
            }
            delieverevent();
            return true;//胜负已分
        }
        nightaction();//入夜，夜间行动
        return true;//测试
    }
    private int shokeiaux(int shokeinum,boolean votable[])
    {
        //投票辅助函数，返回值：被投唯一最多票数的玩家,或0
        //票型直接记录在gc里面
        //参数：当前是第几轮投票;投票数组，记录每个玩家是否可以被投票
        //会修改votable数组，期望保留到调用完成之后
        for(int i=1;i<=gs.getPlayerSum();i++)
        {
            if(gs.gc[i].whyDie != whyDie.NONE) continue;//若为死亡玩家，则退出
            int weight[] = new int[gs.getPlayerSum()+1];//投票权重数组
            for(int j=1;j<=gs.getPlayerSum();j++)
            {
                if(gs.gc[j].whyDie != whyDie.NONE || i == j || (gs.gc[i].actualRole == 4 && gs.gc[j].actualRole == 4))
                {
                    weight[j] = -INF;//已死亡玩家或该玩家本身，或该玩家的共有同伴,被投票权重固定为-INF
                    continue;
                }
                if(!votable[j]) weight[j] = -INFJ;//不是合法投票目标，被投票权重固定为-INFJ
                else weight[j] = gs.gc[i].suspicionValue[j];//基于怀疑度数组，分配权重
                if(gs.gc[j].actualRole == 5 && weight[j] < -INFJ)
                    weight[j] = 5;//正常权重，放置残局不能处刑唯一猫
            }
            
            {
                logicTools.log("玩家" + CharacterKanjiName.values()[gs.gc[i].number] + "投票权重:");
                for(int j=1;j<=gs.getPlayerSum();j++)
                {
                    if(gs.gc[j].whyDie != whyDie.NONE) continue;
                    System.out.print("玩家" + CharacterKanjiName.values()[gs.gc[j].number] + "," + weight[j] + ";");
                }
                logicTools.log("");
            }
            int shokeitaregt = getOne(weight);
            gs.gc[i].voteTarget[gs.gameDay][shokeinum] = shokeitaregt;//得到该名玩家的票型
            updatetop3SuspectedPlayersaux2(i,shokeitaregt,1,1);//更新二人之间怀疑度
            if(gs.gc[shokeitaregt].voteTarget[gs.gameDay][shokeinum] == i)
                updatetop3SuspectedPlayersaux2(i,shokeitaregt,1,1);//额外更新二人之间怀疑度：二人互投
        }
        //敌人的敌人是朋友逻辑
        for(int i=1;i<=gs.getPlayerSum();i++)
        {
            if(gs.gc[i].whyDie != whyDie.NONE) continue;//若为死亡玩家，则退出
            for(int j=1;j<=gs.getPlayerSum();j++)
            {
                if(gs.gc[i].whyDie != whyDie.NONE) continue;//若为死亡玩家，则退出
                if(i == j) continue;//不同的玩家
                for(int k=1;k<=gs.getPlayerSum();k++)
                {
                    if(gs.gc[k].whyDie != whyDie.NONE) continue;//若为死亡玩家，则退出
                    if(i == k || j == k) continue;//不同的玩家
                    if(gs.gc[i].voteTarget[gs.gameDay][shokeinum] == j
                    && gs.gc[j].voteTarget[gs.gameDay][shokeinum] == k)
                        updatetop3SuspectedPlayersaux2(i,k,-1,0);//更新二者怀疑度:i->k -1 k->i 0
                }
            }
        }
        //判断是否产生了平票
        int maxcnt = -1,shokeicnt[] = new int[gs.getPlayerSum()+1];//最大票数 每人被投票数累计
        //得到所有玩家被投的票数
        for(int i=1;i<=gs.getPlayerSum();i++)
        {
            shokeicnt[gs.gc[i].voteTarget[gs.gameDay][shokeinum]] ++;
        }
        //得到最大被投票数
        for(int i=1;i<=gs.getPlayerSum();i++)
        {
            maxcnt = max(maxcnt,shokeicnt[i]);//得到最大被投票数
        }
        int topsum = 0;//被投了最大票数的玩家数量
        int topnum = 0;//被投了最大票数的玩家
        //计算被投了最大票数的玩家数量
        for(int i=1;i<=gs.getPlayerSum();i++)
        {
            if(shokeicnt[i] == maxcnt)
            {
                topsum++;
                topnum = i;
                votable[i] = true;
            }
            else
                votable[i] = false;
        }
        if(topsum == 1) return topnum;//没有平票
        else  return 0;//存在平票
    }
    private int max(int a,int b)
    {
        if(a>b) return a;
        return b;
    }
    public void askCo(Role aclaimedRole)
    {
        //提供给UI类，调用后主逻辑处理询问某一职业co的逻辑，这一步有可能会添加一些事件给UI类
        //参数：被询问co的职业。
        //无返回值，在函数体运行时添加对应的事件
        
            logicTools.log("询问职业：" + aclaimedRole);
        int zhi = aclaimedRole.ordinal();
        if(claimedRoleaskday[zhi] != 0)
        {
            //这个职业已经被整体询问过。直接返回
            return;
        }
        claimedRoleaskday[zhi] = gs.gameDay;//维护askday数组
        if(zhi > 3 && zhi < 6)//本村没有对应的职业，直接返回
        {
            if(gs.p == peiyi.jianyi) return;
            if(zhi == 5 && gs.p != peiyi.maoyou && gs.p != peiyi.daxing) return;
        }
        if(zhi == 5)    //猫村出现两次多死，不可能还有猫了
        {
            int counter = 0;
            for(int i=1;i<=gs.gameDay;i++)
            {
                if(isDoubleDeathOccurred[i])
                    counter++;
            }
            if(counter == 2 && gs.gc[actualRoleindex[5]].whyDie != whyDie.NONE)
                return;
        }
        response.clear();
        for(int i=1;i<=gs.getPlayerSum();i++)
        {
            //死者或职业co者，则不考虑
            if(gs.gc[i].whyDie != whyDie.NONE || gs.gc[i].claimedRole != 0) continue;
            if(gs.gc[i].actualRole < 7)//村侧玩家
            {
                //真职业，直接加入反应数组
                if(gs.gc[i].actualRole == aclaimedRole.ordinal())
                    response.add(new IntPair(i,zhi));
                continue;
            }
            //以下是暂无co的非人玩家逻辑
            if(nonHumanPlan[i] == zhi && nonHumanPlan[i] != 4)//排除co共有的选项
            {
                if(gs.gameDay == 2)
                    gs.gc[i].skillTarget[1] = zw[i];//添加占文
                response.add(new IntPair(i,zhi));//若有计划，则直接co
                continue;
            }
            if(zhi < 3 || aclaimedRole.ordinal() == 4 || nonHumanPlan[i] != 0) continue;//不会临时起意上占上灵,也不会篡共,也不会改变现有计划
            switch(gs.gc[i].actualRole)
            {
                case 7:
                    if(zhi == 3 && rlsl == true) break;
                    if(zhi == 5 && rlsm == true) break;
                    int probability = probabilityCalculator.maolieco(0,3,getp1(7),getp1(7));
                    if(logicTools.probabilityJudge(probability))
                    {
                        response.add(new IntPair(i,zhi));
                        if(zhi == 3)
                            rlsl = true;
                        else
                            rlsm = true;
                    }
                    break;
                default:
                    if(logicTools.probabilityJudge(probabilityCalculator.maolieco(gs.gc[i].actualRole-7,3,getp1(gs.gc[i].actualRole),getp2(gs.gc[i].actualRole))))
                    {
                        response.add(new IntPair(i,zhi));
                    }
                    break;
            }
        }
        response = shuffleList(response);//打乱回应数组
        if(aclaimedRole == Role.gong && response.size() > 1)
            response.remove(1);//去掉复数的潜伏共有解除的事件
        for(int i=0;i<response.size();i++)
        {
            actualRoleCo(response.get(i).first,response.get(i).second);//co职业
        }
        response.clear();
        delieverevent();
    }
    private int getp1(int zhi)//参数：职业 返回值：对应的参数1
    {
        switch(zhi)
        {
            case 7://人狼 上职人狼数
                int p1 = 0;
                for(int i=1;i<=initialWolfCount;i++)
                    if(gs.gc[rlindex[i]].claimedRole != 0 &&gs.gc[rlindex[i]].claimedRole != 6)
                        p1++;
                return p1;
            case 8://其余非人 空置
            case 9:
            case 10:
            case 11:
                return 0;//暂时空置。指定人数通过其他途径获得，其他时候本参数是无效的。填充默认值0
        }
        return 0;//默认不会执行到这一步
    }
    private int getp2(int zhi)//参数：职业 返回值：对应的参数2
    {
        switch(zhi)
        {
            case 7: case 9:
                    //人狼 狂信 剩余人狼数
                int p2 = 0;
                for(int i=1;i<=initialWolfCount;i++)
                    if(gs.gc[rlindex[i]].whyDie == whyDie.NONE)
                        p2++;
                return p2 - 1;
            case 8://其余非人 游戏阶段
            case 10:
            case 11:
                if(gs.deathCounter <= 3)
                    return 0;
                else if(gs.deathCounter <= gs.aliveCounter)
                    return 1;
                else if(gs.aliveCounter > 6)
                    return 2;
                else
                    return 3;
        }
        return 0;//默认不会执行到这一步
    }
    private void delieverevent()//发送当前得到的事件并清空事件数组
    {
        for(int i=0;i<eventarray.size();i++)
        {
            //输出测试信息
            
            {
                logicTools.log("添加事件中");
                if(eventarray.get(i) == null)
                    logicTools.log("事件为空");
                else
                {
                    logicTools.log("成功添加事件，事件信息：" + eventarray.get(i).toString());
                }

               // if(eventarray.get(i).eventname == EventName.cxs)
                //    logicTools.log("成功添加被处刑事件");
                //if(eventarray.get(i).e)
            }
            //将事件添加到UI类当中
            Game.getInstance().getUI().addEvent(eventarray.get(i));
        }
        eventarray.clear();//清空事件数组
    }
    public void askCo(List<Integer> askList)
    {
        //提供给UI类，调用后主逻辑处理询问co的逻辑，这一步有可能会添加一些事件给UI类
        //参数：被询问co的角色编号。
        //无返回值，在函数体运行时添加对应的事件
        response.clear();
        //放在外面定义二者，避免同一轮询问co当中人狼撞车co猫猎
        int probability;
        for(int i=0;i<askList.size();i++)
        {
            int num = askList.get(i);
            if(gs.gc[num].claimedRole != 0) continue;//是co者，则退出
            if(gs.gc[num].actualRole < 7)//村侧
            {
                response.add(new IntPair(num,gs.gc[num].actualRole));//真职业co
            }
            else if(nonHumanPlan[num] != 0 && nonHumanPlan[num] != 4)//已经有上职计划的非人
            {
                if(nonHumanPlan[num] == 1)
                    gs.gc[num].skillTarget[1] = zw[num];
                response.add(new IntPair(num,nonHumanPlan[num]));//非人上职
            }
            else
            {
                //被询问co临时上职的非人
                int option = 0;
                switch(gs.gc[num].actualRole)
                {
                    case 7://人狼
                        if(rlsl && rlsm)
                        {
                            response.add(new IntPair(num,6));
                            break;
                        }
                        probability = probabilityCalculator.maolieco(0,2,getp1(7),getp2(7));
                        if(!logicTools.probabilityJudge(probability))
                        {
                            //未通过co判定，不co
                            response.add(new IntPair(num,6));
                            break;
                        }
                        if(rlsl)
                        {
                            //已经有人狼co猎，自己会co猫
                            if(actualRoleindex[5] != 0)
                                response.add(new IntPair(num,5));
                            else
                                response.add(new IntPair(num,6));
                            rlsm = true;
                            break;
                        }
                        if(rlsm)
                        {
                            //已经有人狼co猫，自己回co猎
                            response.add(new IntPair(num,3));
                            rlsl = true;
                            break;
                        }
                        option = 0;
                        if(actualRoleindex[5] != 0)//have cat
                            option = getEventIndexByProbability(new ArrayList<Integer>(List.of(50+50*maos.size(),50+50*lies.size())));
                        //没有人狼co猫或者co猎，决定猫猎co的选择 0猎 1猫
                        response.add(new IntPair(num,3+2*option));//3 lie 5 mao
                        break;
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                        probability =probabilityCalculator.maolieco(gs.gc[num].actualRole - 7,2,getp1(gs.gc[num].actualRole - 7),getp2(gs.gc[num].actualRole - 7));
                        if(!logicTools.probabilityJudge(probability))
                        {
                            //未通过co判定，不co
                            response.add(new IntPair(num,6));
                            break;
                        }
                        option = 0;
                        if(actualRoleindex[5] != 0)
                            option = getEventIndexByProbability(new ArrayList<Integer>(List.of(50+50*maos.size(),50+50*lies.size())));
                        //没有人狼co猫或者co猎，决定猫猎co的选择 0猎 1猫
                        response.add(new IntPair(num,3+2*option));//3 lie 5 mao
                        break;
                }
            }
        }
        response = shuffleList(response);//打乱回应数组
        for(int i=0;i<response.size();i++)
        {
            actualRoleCo(response.get(i).first,response.get(i).second);//co职业
        }
        delieverevent();//发送当前事件，清空事件数组
    }
}
