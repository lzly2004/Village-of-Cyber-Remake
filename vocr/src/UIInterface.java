public interface UIInterface
{
    void addEvent(Event event);
    //主逻辑类调用。用来添加事件
    GameStatus getGameStatus();
    //主逻辑类调用。用于获取UI类的gs
}
