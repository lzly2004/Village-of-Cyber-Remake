import javax.swing.*;

public interface ResourcesInterface
{
    ImageIcon getImage(String imageName);
    //参数：图像名称
    //提供给UI类。给定图片名称，得到对应的图片图标对象
    boolean playBgm(String bgmName);
    //参数：背景音乐名称
    //提供给UI类，调用后循环播放背景音乐，之前播放的背景音乐自动停止并释放资源
    //默认返回值true，播放失败返回false
    String getCurrentBgmName();
    //返回当前正在播放的BGM文件名
    boolean playSound(String soundName);
    //参数：音频名称
    //提供给UI类，调用后播放一段音效，不可暂停，一次性播放不会循环
    //默认返回值true，播放失败返回false
    String getEventText(Event event);
    //参数：事件
    //提供给UI类，调用之后返回一个事件的台词。
    String getHelpText(String helpTitle);
    //参数：帮助条文标题。
    //提供给UI类，调用之后返回对应标题的帮助文字。
    ImageIcon[] getEventImage(Event event);
    //参数：事件
    //提供给UI类，调用之后返回一个事件的图片图标。
    void save(GameInfo gameInfo,int cnt);
    //参数：局内信息;第cnt个存档
    //实现：提供给UI类，存档，存为第cnt个存档。有可能会覆盖已有的存档。此时不改变胜场数胜率等数据，
    //当cnt为0，表示不存档，此时这个函数的功能是记录对局记录，调整胜场数胜率等数据
    //cnt=0时存储的有可能是还没有打完的对局，需要自己判断
    GameInfo load(int cnt);
    //提供给UI类，
    //参数：第cnt个存档
    //返回：第cnt个存档的存档信息
    GameRecord getRecord();
    //提供给UI类，返回历史对局情况
}