import java.util.List;
public interface MainLogicInterface
{
    GameStatus getGameStatus();//提供给UI类，让UI类得到当前的游戏状态
    GameStatus start(peiyi p);
    //提供给UI类，开始一局游戏
    //参数：配役p
    //返回值：游戏状态
    //函数体中有可能向UI类添加一系列事件
    boolean shokei(int cxMethod, List<Integer> chuxingList,boolean huibi);
    //提供给UI类，主逻辑现在进行投票判定
    //参数：投票方法。 0：自由投票 1：随机灰吊 2：指定投票
    // 被制定处刑的玩家编号数组
    //返回值：true：投票成功，并且得到票型
    //false:没有进行投票(存在回避co)
    //huibi:true开启回避 false关闭回避
    void askCo(Role azhiye);
    //提供给UI类，调用后主逻辑处理询问某一职业co的逻辑，这一步有可能会添加一些事件给UI类
    //参数：被询问co的职业。
    //无返回值，在函数体运行时添加对应的事件
    void askCo(List<Integer> askList);
    //提供给UI类，调用后主逻辑处理询问co的逻辑，这一步有可能会添加一些事件给UI类
    //参数：被询问co的角色编号。
    //无返回值，在函数体运行时添加对应的事件
}
