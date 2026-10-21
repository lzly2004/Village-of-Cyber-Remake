package com.vocr.core;

import com.vocr.application.GameEventBuffer;
import com.vocr.application.GameSession;
import com.vocr.history.GameRecordManager;

/** 仅测试使用的种子注入，避免为 HTTP 增加生产随机/Replay 协议。 */
public final class SeededGuestSession
{
    private SeededGuestSession() { }

    public static GameSession create(String id, long seed)
    {
        GameEventBuffer events = new GameEventBuffer();
        GameRecordManager records = GameRecordManager.inMemory();
        return new GameSession(id, new MainLogic(events::publish, records,
                new GameRandom(seed)), events, records);
    }
}
