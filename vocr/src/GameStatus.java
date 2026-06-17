import java.util.Date;

class GameStatus
{//游戏状态封装类
    /*
    * public static int CharacterSum = 44;// 游戏角色数量
    public static int N = 50;//最大游戏天数
    public static int M = 20;//职业状态数量
    * */
    public peiyi p;//游戏配役
    public Date date;//游戏开始时间
    boolean isInGame;//当前是否在游戏中
    public int end;//当前游戏是否结束 int 0未结束 1村胜 2狼胜 3狐胜
    boolean isHeInGame[];//这个角色是否在游戏中 int[CharecterSum+1]
    public int gameDay;//游戏进行到多少日目
    public int deathCounter;//游戏中当前的死亡人数计数
    public int aliveCounter;//游戏中当前的存活人数计数
    public int dailyVotingRule[];//游戏中每天的处刑方法，每一天的默认值都是-1   0：自由投票 1：随机灰吊 2：指定投票 int[N+1]
    GameCharacter gc[];//游戏角色数组 int[CharecterSum+1]
    boolean hiddenSeerScheduledSkillTargets[][];//潜伏占卜师的预告 int[getPlayerSum()+1][N+1]N:最大游戏天数 最大可以取50
    boolean hiddenHunterScheduledSkillTargets[][];//潜伏猎人的预告 int[getPlayerSum()+1][N+1]  N:最大游戏天数 最大可以取50
    public GameStatus()
    {//普通的构造函数。构造时赋初值-1，表示游戏还未开始
        //初始化游戏状态的其他字段
        this.date = new Date();
        this.isInGame = true;
        this.end = 0;
        this.isHeInGame = new boolean[ConstNum.CharacterSum+1];
        this.gameDay = 1;
        this.deathCounter = 0;
        this.dailyVotingRule = new int [ConstNum.N+1];
    }
    public void startGame(peiyi p)  //开始一局游戏
    {
        //游戏初始化，参数：游戏配役编号
        //...
        this.p = p;
        int characters[] = new int[30];//登场的角色编号
        int zysz[] = new int[30];//本局游戏职业配置
        /*
        * 0 NONE,
    1 zhan,//占卜师
    2 ling,//灵媒师
    3 lie,//猎人
    4 gong,//共有者
    5 mao,//猫又
    6 wu,//无co，村人
    //以下是非人职业，游戏中不会有人co这些职业
    7 renlang,//人狼
    8 kuangren,//狂人
    9 kuangxinzhe,//狂信者
    10 yaohu,//妖狐
    11 beidezhe//背德者
        * */
        switch(p)
        {
            //固定：占灵猎
            case jianyi:
                characters =//简易村 12人 2狼1狂6村
                        new int[]{1, 39, 37, 22, 6, 27, 3, 35, 42, 7, 9, 28};
                zysz = new int[]{1, 2, 3, 7, 7, 8, 6, 6, 6, 6, 6, 6};
                break;
            case tongchang:
                characters =//通常村 16人 3狼1狂2共7村
                        new int[]{30, 35, 39, 11, 18, 2, 23, 24, 40, 20, 34, 29, 13, 4, 8, 15};
                zysz = new int[]{1, 2, 3, 4, 4, 7, 7, 7, 8, 6, 6, 6, 6, 6, 6, 6};
                break;
            case yaoohu:
                characters =//妖狐村 16人 3狼1狂2共1狐6村
                        new int[]{1, 7, 12, 6, 20, 43, 31, 27, 36, 3, 29, 9, 42, 44, 25, 28};
                zysz = new int[]{1, 2, 3, 4, 4, 7, 7, 7, 8, 10, 6, 6, 6, 6, 6, 6};
                break;
            case kuangxin:
                characters =//狂信村 17人 3狼1信2共1狐7村
                        new int[]{30, 34, 17, 16, 26, 22, 38, 5, 32, 44, 19, 21, 33, 37, 41, 10, 14};
                zysz = new int[]{1, 2, 3, 4, 4, 7, 7, 7, 9, 10, 6, 6, 6, 6, 6, 6, 6};
                break;
            case beide:
                characters =//背德村 18人 3狼1狂2共1狐1背7村
                        new int[]{6, 8, 9, 11, 13, 21, 22, 24, 25, 26, 30, 31, 32, 33, 37, 38, 40, 41};
                zysz = new int[]{1, 2, 3, 4, 4, 7, 7, 7, 8, 10, 11, 6, 6, 6, 6, 6, 6, 6};
                break;
            case maoyou:
                characters =//猫又村 18人 4狼1狂2共1狐1猫又6村
                        new int[]{1, 2, 4, 5, 10, 12, 14, 15, 16, 17, 18, 19, 23, 27, 28, 36, 43, 44};
                zysz = new int[]{1, 2, 3, 4, 4, 5, 7, 7, 7, 7, 8, 10, 6, 6, 6, 6, 6, 6};
                break;
            case daxing:
                characters =//大型村 20人 4狼1信2共1狐1背1猫又7村
                        generateRandomCharacterArray();
                zysz = new int[]{1, 2, 3, 4, 4, 5, 7, 7, 7, 7, 9, 10, 11, 6, 6, 6, 6, 6, 6, 6};
                break;
        }
        //实现发身份的随机洗牌
        int cnt = characters.length;
        //System.out.println("玩家数量%："+cnt);

        for(int i=0;i<cnt-1;i++)
        {
            DebugLogger.log("当前随机交换玩家：" + i);
            int rd = ConstNum.randomInt(i+1,cnt-1),tmp;
            tmp = characters[i];//随机交换
            characters[i] = characters[rd];
            characters[rd] = tmp;

            int rd1 = ConstNum.randomInt(i+1,cnt-1);
            tmp = zysz[i];
            zysz[i] = zysz[rd1];
            zysz[rd1] = tmp;
        }
        /**/

        //初始化游戏状态的其他字段
        this.aliveCounter = cnt;
        gc = new GameCharacter[getPlayerSum()+1];//初始化gc
        //System.out.println("初始化startgame时，gc长度："+gc.length);
        //初始化每一个角色
        for(int i=1;i<=cnt;i++)
        {
            gc[i] = new GameCharacter(characters[i-1],zysz[i-1]);
            this.isHeInGame[characters[i-1]] = true;
        }
        for(int i=1;i<=cnt;i++)
        {
            if(gc[i] == null || gc[i].actualRole == 0)
            {
                DebugLogger.error("第"+i+"个角色初始化失败");
            }
        }
        this.hiddenHunterScheduledSkillTargets = new boolean[getPlayerSum()+1][ConstNum.N+1];
        this.hiddenSeerScheduledSkillTargets = new boolean[getPlayerSum()+1][ConstNum.N+1];
    }
    public int getPlayerSum()
    {//得到本局游戏的玩家数
        return deathCounter + aliveCounter;//死亡人数+存活人数=总人数
    }
    public static int[] generateRandomCharacterArray()  //得到一个合法的大型村玩家编号数组
    {
        // 1. 定义核心常量
        final int MIN_VALUE = 1;
        final int MAX_VALUE = 44;
        final int ARRAY_LENGTH = 20;

        // 2. 初始化 1-44 的数组（源头确保无重复）
        int[] numberPool = new int[MAX_VALUE - MIN_VALUE + 1];
        for (int i = 0; i < numberPool.length; i++) {
            numberPool[i] = MIN_VALUE + i; // 数组值：1,2,3,...,44
        }

        // 3. 随机交换元素（Fisher-Yates 洗牌算法）：不依赖外部库，保证位置和概率随机
        // 从数组末尾往前遍历，每次随机选一个前面的位置，交换元素
        for (int i = numberPool.length - 1; i > 0; i--) {
            // 调用已有随机数方法：生成 0-i 闭区间的随机索引（对应 numberPool 未洗牌的前半部分）
            int randomIndex = ConstNum.randomInt(0, i);

            // 交换当前索引 i 和随机索引 randomIndex 的元素
            int temp = numberPool[i];
            numberPool[i] = numberPool[randomIndex];
            numberPool[randomIndex] = temp;
        }

        // 4. 截取前 20 个元素，生成目标数组
        int[] character = new int[ARRAY_LENGTH];
        for (int i = 0; i < ARRAY_LENGTH; i++) {
            character[i] = numberPool[i];
        }

        return character;
    }
}