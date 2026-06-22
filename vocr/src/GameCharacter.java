class GameCharacter
{//游戏角色封装类
    //角色编号
    int number;//青年1 研究2 ...
    //职业相关
    int actualRole;//真实职业 claimedRole
    int claimedRole;//当前声称的职业 claimedRole
    int claimedRoleorder;//当前声称的职业是同职业声称者中的第几个,从1开始计数
    int comingOutDay;//当前角色是第几天co出来的职业 默认值是-1 若当前角色是村人，此项无意义
    //怀疑相关
    int[][] top3SuspectedPlayers;//怀疑前三的角色在gc数组中是第几个,int [3+1][N+1]
    int[] suspicionValue;//对其他角色的怀疑度 int [配役人数+1]
    int[][] voteTarget;//每天的的票型 int[N+1][4] N:最大游戏天数 最大可以取50 最多可以投3轮票 voteTarget[4][2] = 3: 第四天产生平票，第二轮投票中该玩家投给了玩家编号3的玩家
    boolean[] isSelectedVoteTarget;//角色是否当天被指定 boolean [N+1]
    boolean nonHumanMarker;//角色是否已经明确破绽了
    //技能相关
    int[] skillTarget;//每天使用技能的对象 int[N+1] 额外+配役人数代表黑色结果，否则代表白色结果和守护对象
    //可以代表潜伏非人的预定编造结果（猎人日记）
    boolean[][] claimedRoleScheduledSkillTargets;//若为职业co者，被安排的技能使用对象 boolean[配役人数+1][N+1]
    //生死相关
    whyDie whyDie;//死亡原因枚举类型
    int dieDay;//死亡日
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
        this.top3SuspectedPlayers = new int[GameConstants.TOP3_ARRAY_SIZE][GameConstants.MAX_GAME_DAYS+1];
        this.suspicionValue = new int[GameConstants.CHARACTER_SUM+1];
        this.voteTarget = new int[GameConstants.MAX_GAME_DAYS+1][4];
        this.isSelectedVoteTarget = new boolean[GameConstants.MAX_GAME_DAYS+1];
        this.nonHumanMarker = false;
        this.skillTarget = new int[GameConstants.MAX_GAME_DAYS+1];
        this.claimedRoleScheduledSkillTargets = new boolean[GameConstants.CHARACTER_SUM+1][GameConstants.MAX_GAME_DAYS+1];
        this.whyDie = whyDie.NONE;
        this.comingOutDay = -1;
    }
}