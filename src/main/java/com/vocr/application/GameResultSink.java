package com.vocr.application;

import com.vocr.domain.GameResult;
import com.vocr.domain.peiyi;

/**
 * 对局结果持久化端口。
 *
 * <p>应用会话只提交不可变的配役与胜负值；具体写入本地 JSON、数据库或仅保存在
 * 会话内，由组合根选择。</p>
 */
@FunctionalInterface
public interface GameResultSink
{
    void record(peiyi configuration, GameResult result);
}
