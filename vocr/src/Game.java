import javax.swing.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static javax.swing.text.html.HTML.Attribute.N;


enum CharacterEnglishName {
    NONE, // 角色信息：无，日文名：无，编号：0
    Abel, // 角色信息：青年，日文名：アーベル，编号：1
    Erich, // 角色信息：研究，日文名：エーリッヒ，编号：2
    Matthäus, // 角色信息：佣兵，日文名：マテウス，编号：3
    Otfried, // 角色信息：教师，日文名：オトフリート，编号：4
    Karl, // 角色信息：情报，日文名：カルル，编号：5
    Till, // 角色信息：少年，日文名：ティル，编号：6
    Samuel, // 角色信息：宝石，日文名：ザムエル，编号：7
    Hans, // 角色信息：旅人，日文名：ハンス，编号：8
    Beatrice, // 角色信息：少女，日文名：ベアト，编号：9
    Amanda, // 角色信息：陶艺，日文名：アマンダ，编号：10
    Irene, // 角色信息：洋灯，日文名：イレーネ，编号：11
    Elsa, // 角色信息：歌姬，日文名：エルザ，编号：12
    Nora, // 角色信息：未亡，日文名：ノーラ，编号：13
    Johanna, // 角色信息：物识，日文名：ヨハナ，编号：14
    Milli, // 角色信息：读书，日文名：ミリィ，编号：15
    Judith, // 角色信息：召使，日文名：ユーディット，编号：16
    Michael, // 角色信息：贵族，日文名：ミハエル，编号：17
    Günther, // 角色信息：团长，日文名：ギュンター，编号：18
    David, // 角色信息：骑士，日文名：ダーヴィッド，编号：19
    Julian, // 角色信息：职人，日文名：ユリアン，编号：20
    Klemens, // 角色信息：神父，日文名：クレメンス，编号：21
    Heinrich, // 角色信息：探侦，日文名：ハインリヒ，编号：22
    Liddi, // 角色信息：学生，日文名：リディ，编号：23
    Brigitte, // 角色信息：小说，日文名：ブリジット，编号：24
    Helga, // 角色信息：女将，日文名：ヘルガ，编号：25
    Natalie, // 角色信息：尼僧，日文名：ナターリエ，编号：26
    Volker, // 角色信息：双弟，日文名：フォルカー，编号：27
    Eva, // 角色信息：双姐，日文名：エーファ，编号：28
    Willy, // 角色信息：流者，日文名：ヴィリー，编号：29
    Reichard, // 角色信息：修道，日文名：ライヒアルト，编号：30
    Hugo, // 角色信息：宿主，日文名：フーゴー，编号：31
    Rosa, // 角色信息：踊子，日文名：ローザ，编号：32
    Wendel, // 角色信息：神学，日文名：ウェンデル，编号：33
    Sergius, // 角色信息：药师，日文名：ゼルギウス，编号：34
    Kaja, // 角色信息：烟突，日文名：カヤ，编号：35
    Betti, // 角色信息：店员，日文名：ベッティ，编号：36
    Chloe, // 角色信息：洗濯，日文名：クロエ，编号：37
    Carmen, // 角色信息：娼妓，日文名：カルメン，编号：38
    Renate, // 角色信息：剑士，日文名：レナーテ，编号：39
    Romi, // 角色信息：村娘，日文名：ロミ，编号：40
    Gerda, // 角色信息：刺绣，日文名：ゲルダ，编号：41
    Iwan, // 角色信息：大工，日文名：イヴァン，编号：42
    Oktavia, // 角色信息：妇人，日文名：オクタヴィア，编号：43
    Helmut; // 角色信息：音乐，日文名：ヘルムート，编号：44
}
enum CharacterKanjiName
{
    NONE,
    青年,          // 青年
    研究生,        // 研究生
    傭兵,          // 傭兵
    教師,          // 教師
    情報通,        // 情報通
    少年,          // 少年
    宝石商,        // 宝石商
    旅人,          // 旅人
    少女,          // 少女
    陶芸家,        // 陶芸家
    ランプ屋,      // ランプ屋
    歌姫,          // 歌姫
    未亡人,        // 未亡人
    物識り,        // 物識り
    読書家,        // 読書家
    召使い,        // 召使い
    貴族,          // 貴族
    自衛団長,      // 自衛団長
    騎士,          // 騎士
    職人見習い,    // 職人見習い
    神父,          // 神父
    探偵,          // 探偵
    学生,          // 学生
    小説家,        // 小説家
    酒場のママ,    // 酒場のママ
    シスター,      // シスター
    双生児弟,        // 双生児（エーファ）
    双生児姉,        // 双生児（フォルカー）
    流れ者,        // 流れ者
    修道士,        // 修道士
    宿屋主人,      // 宿屋主人
    踊り子,        // 踊り子
    神学生,        // 神学生
    薬師,          // 薬師
    煙突掃除人,    // 煙突掃除人
    店員,          // 店員
    洗濯女,        // 洗濯女
    娼妓,          // 娼妓
    剣士,          // 剣士
    村娘,          // 村娘
    刺繍工,        // 刺繍工
    大工,          // 大工
    貴婦人,        // 貴婦人
    音楽家         // 音楽家
}

enum CharacterKatakanaName
{
    NONE,
    アーベル,      // アーベル
    エーリッヒ,    // エーリッヒ
    マテウス,      // マテウス
    オトフリート,  // オトフリート
    カルル,        // カルル
    ティル,        // ティル
    ザムエル,      // ザムエル
    ハンス,        // ハンス
    ベアトリーチェ,// ベアトリーチェ
    アマンダ,      // アマンダ
    イレーネ,      // イレーネ
    エルザ,        // エルザ
    ノーラ,        // ノーラ
    ヨハナ,        // ヨハナ
    ミリィ,        // ミリィ
    ユーディット,  // ユーディット
    ミハエル,      // ミハエル
    ギュンター,    // ギュンター
    ダーヴィッド,  // ダーヴィッド
    ユリアン,      // ユリアン
    クレメンス,    // クレメンス
    ハインリヒ,    // ハインリヒ
    リディ,        // リディ
    ブリジット,    // ブリジット
    ヘルガ,        // ヘルガ
    ナターリエ,    // ナターリエ
    フォルカー,    // フォルカー（双生儿弟弟）
    エーファ,      // エーファ（双生儿姐姐）
    ヴィリー,      // ヴィリー
    ライヒアルト,  // ライヒアルト
    フーゴー,      // フーゴー
    ローザ,        // ローザ
    ウェンデル,    // ウェンデル
    ゼルギウス,    // ゼルギウス
    カヤ,          // カヤ
    ベッティ,      // ベッティ
    クロエ,        // クロエ
    カルメン,      // カルメン
    レナーテ,      // レナーテ
    ロミ,          // ロミ
    ゲルダ,        // ゲルダ
    イヴァン,      // イヴァン
    オクタヴィア,  // オクタヴィア
    ヘルムート     // ヘルムート
}
enum EventName
{//主逻辑类传递给UI类的事件名称枚举类型
    NONE,
    gyfo1r,//1r共有FO
    gyfo1,//1共有FO
    gyho2,//2共有FO
    qfgsw3,//3潜伏共死亡
    gkgsw4,//4公开共死亡
    qfjcqr5r,//5r潜伏解除确认
    qfjc5,//5潜伏解除
    gycx6,//6共有处刑
    zco7,//7占co
    jhdh8b,//8b接黒对话
    zjgh8b,//8b占结果黑
    jbdh8r,//8r接白对话
    zjgb8,//8占结果白
    zrzbjg9,//9昨日占卜结果
    zbdxsw10,//10占卜对象死亡
    gprz11p,//11p共pair认证
    gprz11r,//11r共pair认证
    zcrh12r,//12r占初日黑
    zcrh12,//12占初日黑
    gyzs13,//13共有指示
    zs14,//14咒杀
    zspz15,//15咒杀破绽
    szsm16,//16是咒杀吗？
    wz17,//17完占
    lnco18,//18灵co
    ljgh19b,//19b灵结果黑
    ljgb19,//19灵结果白
    cxs,//处刑时
    crsl,//村人胜利
    rlsl,//人狼胜利
    yhsl,//妖狐胜利
    krsl,//狂人胜利
    hblr,//回避猎人
    hbg,//回避共有
    hbln,//回避灵能
    hbz,//回避占
    hbm,//回避猫
    lrco,//猎人co
    mco,//猫co
    //以下是自加的事件。这些事件没有原本的台词txt文件
    yjsw,//夜间死亡
    hzsw,//后追死亡
    mzsw,//猫咒死亡
    wsw,//无死亡，平和
    ; // 注意添加分号，这是枚举重写方法的必要前提

    @Override
    public String toString()
    {
        return switch (this) {
            case NONE -> ""; // NONE无注释，返回空字符串
            case gyfo1r -> "1r共有FO";
            case gyfo1 -> "1共有FO";
            case gyho2 -> "2共有FO";
            case qfgsw3 -> "3潜伏共死亡";
            case gkgsw4 -> "4公开共死亡";
            case qfjcqr5r -> "5r潜伏解除确认";
            case qfjc5 -> "5潜伏解除";
            case gycx6 -> "6共有处刑";
            case zco7 -> "7占co";
            case jhdh8b -> "8b接黒对话";
            case zjgh8b -> "8b占结果黑";
            case jbdh8r -> "8r接白对话";
            case zjgb8 -> "8占结果白";
            case zrzbjg9 -> "9昨日占卜结果";
            case zbdxsw10 -> "10占卜对象死亡";
            case gprz11p -> "11p共pair认证";
            case gprz11r -> "11r共pair认证";
            case zcrh12r -> "12r占初日黑";
            case zcrh12 -> "12占初日黑";
            case gyzs13 -> "13共有指示";
            case zs14 -> "14咒杀";
            case zspz15 -> "15咒杀破绽";
            case szsm16 -> "16是咒杀吗？";
            case wz17 -> "17完占";
            case lnco18 -> "18灵co";
            case ljgh19b -> "19b灵结果黑";
            case ljgb19 -> "19灵结果白";
            case cxs -> "处刑时";
            case crsl -> "村人胜利";
            case rlsl -> "人狼胜利";
            case yhsl -> "妖狐胜利";
            case krsl -> "狂人胜利";
            case hblr -> "回避猎人";
            case hbg -> "回避共有";
            case hbln -> "回避灵能";
            case hbz -> "回避占";
            case hbm -> "回避猫";
            case lrco -> "猎人co";
            case mco -> "猫co";
            case yjsw -> "夜间死亡";
            case hzsw -> "后追死亡";
            case mzsw -> "猫咒死亡";
            case wsw -> "无死亡，平和";
        };
    }
}
enum Role
{//职业声称情况
    NONE,
    zhan,//1占卜师
    ling,//2灵媒师
    lie,//3猎人
    gong,//4共有者
    mao,//5猫又
    wu,//6无co，村人
    //以下是非人职业，游戏中不会有人co这些职业
    renlang,//7人狼
    kuangren,//8狂人
    kuangxinzhe,//9狂信者
    yaohu,//10妖狐
    beidezhe//11背德者
}
enum peiyi
{//开村配役
    NONE,
    jianyi,//简易
    tongchang,//通常
    yaoohu,//妖狐
    kuangxin,//狂信
    beide,//背德
    maoyou,//猫又
    daxing//大型
}
class Event
{//事件类型
    public CharacterEnglishName ch1,ch2,ch3;
    public EventName eventname;
    public Event(EventName eventname, CharacterEnglishName ch1, CharacterEnglishName ch2, CharacterEnglishName ch3)
    {
        // 统一的业务逻辑实现
        this.eventname = eventname;
        this.ch1 = ch1;
        this.ch2 = ch2;
        this.ch3 = ch3;
    }

    // 2. 重载：仅传ename+ch1+ch2，ch3默认null
    public Event(EventName ename, CharacterEnglishName ch1, CharacterEnglishName ch2) {
        this(ename, ch1, ch2, null); // 调用全参构造函数，复用逻辑
    }

    // 3. 重载：仅传ename+ch1，ch2、ch3默认null
    public Event(EventName ename, CharacterEnglishName ch1) {
        this(ename, ch1, null, null); // 调用全参构造函数
    }

    //

    @Override
    public String toString()
    {
        if(eventname == null) return null;
        String str = eventname.toString();
        if(ch1 == null) str += " null";
        else str += " " + CharacterKanjiName.values()[ch1.ordinal()].toString();
        if(ch2 == null) str += " null";
        else str += " " + CharacterKanjiName.values()[ch2.ordinal()].toString();
        return str;
    }
}
enum whyDie
{//死亡原因枚举类型
    NONE,//存活，暂时还未死亡
    //白天死亡，可以辨别死因。
    chuxing,//处刑
    daymaozhou,//白天猫咒
    dayhouzhui,//白天后追
     //晚上死亡，注意无法辨别死因。
    beiyao,//被咬
    nightmaozhou,//夜间猫咒
    nighthouzhui,//夜间后追
    zhousha,//咒杀
}
class ConstNum
{//常量类，定义游戏常量以及静态方法
    public static int CharacterSum = 44;// 游戏角色数量
    public static int N = 50;//最大游戏天数
    public static int M = 20;//职业状态数量
    public static int randomInt(int min,int max)
    {//生成[min,max]的随机整数
        return min + (int)(Math.random() * (max - min + 1));
    }
    public static int WINDOW_WIDTH = 1280;//游戏窗口的宽度
    public static int WINDOW_HEIGHT = 720;//游戏窗口的高度
    public static double WINDOW_WIDTH_D = 1280.0;//游戏窗口的宽度(double)
    public static double WINDOW_HEIGHT_D = 720.0;//游戏窗口的高度(double)
}
class GameCharacter
{//游戏角色封装类
    //角色编号
    public int number;//青年1 研究2 ...
    //职业相关
    public int actualRole;//真实职业 claimedRole
    public int claimedRole;//当前声称的职业 claimedRole
    public int claimedRoleorder;//当前声称的职业是同职业声称者中的第几个,从1开始计数
    public int comingOutDay;//当前角色是第几天co出来的职业 默认值是-1 若当前角色是村人，此项无意义
    //怀疑相关
    public int top3SuspectedPlayers[][];//怀疑前三的角色在gc数组中是第几个,int [3+1][N+1]
    public int suspicionValue[];//对其他角色的怀疑度 int [配役人数+1]
    public int voteTarget[][];//每天的的票型 int[N+1][4] N:最大游戏天数 最大可以取50 最多可以投3轮票 voteTarget[4][2] = 3: 第四天产生平票，第二轮投票中该玩家投给了玩家编号3的玩家
    public boolean asked;//角色是否被问过co（无用，考虑删除）
    public boolean isSelectedVoteTarget[];//角色是否当天被指定 boolean [N+1]
    public boolean nonHumanMarker;//角色是否已经明确破绽了
    //技能相关
    public int skillTarget[];//每天使用技能的对象 int[N+1] 额外+配役人数代表黑色结果，否则代表白色结果和守护对象
    //可以代表潜伏非人的预定编造结果（猎人日记）
    public boolean claimedRoleScheduledSkillTargets[][];//若为职业co者，被安排的技能使用对象 boolean[配役人数+1][N+1]
    //生死相关
    public whyDie whyDie;//死亡原因枚举类型
    public int dieDay;//死亡日
    //日期的定义：
    //游戏开始时，日期为1日
    //1日夜->2日昼->2日夜->3日昼->3日夜->4日昼->4日夜->...
    GameCharacter(int number,int actualRole)
    {
        this.number = number;
        this.actualRole = actualRole;
        this.claimedRole = 0;
        this.dieDay = 0;
        this.claimedRoleorder = 0;
        this.top3SuspectedPlayers = new int[4][ConstNum.N+1];
        this.suspicionValue = new int[ConstNum.CharacterSum+1];
        this.voteTarget = new int[ConstNum.N+1][4];
        this.asked = false;
        this.isSelectedVoteTarget = new boolean[ConstNum.N+1];
        this.nonHumanMarker = false;
        this.skillTarget = new int[ConstNum.N+1];
        this.claimedRoleScheduledSkillTargets = new boolean[ConstNum.CharacterSum+1][ConstNum.N+1];
        this.whyDie = whyDie.NONE;
        this.comingOutDay = -1;
    }
    GameCharacter()
    {
        this(1,1);
    }
}
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
            System.out.println("当前随机交换玩家：" + i);
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
                System.out.println("第"+i+"个角色初始化失败");
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
class GameInfo
{//游戏信息封装类，用于游戏存档与对局查看
    int days;//游戏一共进行多少天
    GameStatus gs[];
    GameInfo(GameStatus ggs[],int days)
    {
       //赋值,深度拷贝
        this.days = days;
        gs = new GameStatus[days];
        for(int i=0;i<=days;i++)
            gs[i] = ggs[i];
    }
}
class GameRecord
{
    //各数组的有效下标：1~7，表示各个村的历史游玩情况
    int playcnt[];//游玩计数
    double winrate[];//胜率
    int villageWincnt[];//村胜利计数
    int wolfWincnt[];//狼胜利计数
    int foxWincnt[];//狐胜利计数
    int wincnt[];//连胜计数
    GameRecord()
    {//这个函数定义由资源管理负责
        //...
    }
}
public class Game
{   //单例模式
    //公共成员，三个类共同维护的外部类的数据成员
    //...
    private static Game instance = null;
    UI ui;
    Resources resources;
    MainLogic mainlogic;
    private Game()
    {
        this.resources = new Resources();
        this.mainlogic = new MainLogic();
        this.ui = new UI();
        this.init(); // 再调用初始化方法
    }
    public static Game getInstance()
    {
        if (instance == null) instance = new Game();
        return instance;
    }
    public void init()
    {
        resources.init();
        ui.init();
    }
    public void run()
    {
        init();
        System.out.println("village of cyber");
        resources.run();
        ui.run();
    }
    public static void main(String[] args)
   {
       new Game().run();
   }
    public UIInterface getUI()
    {
        return ui;
    }
    public MainLogicInterface getMainLogic()
    {
        return mainlogic;
    }
    public ResourcesInterface getResources()
    {
        return resources;
    }
}