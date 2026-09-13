package com.vocr.resources;

import com.vocr.domain.Event;

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
