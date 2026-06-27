enum EventName
{
    NONE(""),
    gyfo1r("1r共有FO"),
    gyfo1("1共有FO"),
    gyho2("2共有FO"),
    qfgsw3("3潜伏共死亡"),
    gkgsw4("4公开共死亡"),
    qfjcqr5r("5r潜伏解除确认"),
    qfjc5("5潜伏解除"),
    gycx6("6共有处刑"),
    zco7("7占co"),
    jhdh8b("8b接黒对话"),
    zjgh8b("8b占结果黑"),
    jbdh8r("8r接白对话"),
    zjgb8("8占结果白"),
    zrzbjg9("9昨日占卜结果"),
    zbdxsw10("10占卜对象死亡"),
    gprz11p("11p共pair认证"),
    gprz11r("11r共pair认证"),
    zcrh12r("12r占初日黑"),
    zcrh12("12占初日黑"),
    gyzs13("13共有指示"),
    zs14("14咒杀"),
    zspz15("15咒杀破绽"),
    szsm16("16是咒杀吗？"),
    wz17("17完占"),
    lnco18("18灵co"),
    ljgh19b("19b灵结果黑"),
    ljgb19("19灵结果白"),
    cxs("处刑时"),
    crsl("村人胜利"),
    rlsl("人狼胜利"),
    yhsl("妖狐胜利"),
    krsl("狂人胜利"),
    hblr("回避猎人"),
    hbg("回避共有"),
    hbln("回避灵能"),
    hbz("回避占"),
    hbm("回避猫"),
    lrco("猎人co"),
    mco("猫co"),
    yjsw("夜间死亡"),
    hzsw("后追死亡"),
    mzsw("猫咒死亡"),
    wsw("无死亡，平和"),
    ;

    private final String label;

    EventName(String label) { this.label = label; }

    @Override
    public String toString() { return label; }
}