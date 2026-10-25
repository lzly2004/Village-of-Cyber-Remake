package com.vocr.resources;

import com.vocr.domain.Event;

import javax.swing.*;

public interface ResourcesInterface
{
    ImageIcon getImage(UiImage image);
    //参数：固定用途图片的语义标识
    //提供给UI类。固定图片应优先走此入口，避免界面依赖实际文件名
    ImageIcon getImage(String imageName);
    //参数：图像名称
    //保留给事件映射与按规则生成名称的图片
    boolean playBgm(BgmTrack bgmTrack);
    //参数：背景音乐语义标识
    //提供给UI类，调用后循环播放背景音乐，之前播放的背景音乐自动停止并释放资源
    //默认返回值true，播放失败返回false
    void stopBgm();
    //停止并释放当前背景音乐
    BgmTrack getCurrentBgm();
    //返回当前正在播放的BGM语义标识；未播放时返回null
    boolean playSound(SoundEffect soundEffect);
    //参数：固定用途音效
    //提供给UI类，调用后播放对应语义的音效，不可暂停，一次性播放不会循环
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
}
