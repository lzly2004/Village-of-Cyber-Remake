import java.util.LinkedList;

public interface UIInterface
{
    void addEvent(Event event);
    //主逻辑类调用。用来添加事件
    LinkedList<Event> getEvents();
    //获取事件队列
}