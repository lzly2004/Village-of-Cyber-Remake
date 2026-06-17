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