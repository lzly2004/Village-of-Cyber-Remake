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

    /** WEB-017 only: select the existing 4200+ordinal baseline before start consumes randomness. */
    public static GameSession createReleaseBaseline(String id)
    {
        GameEventBuffer events = new GameEventBuffer();
        GameRecordManager records = GameRecordManager.inMemory();
        GameRandom seededRandom = new GameRandom(0);
        MainLogic logic = new MainLogic(events::publish, records, seededRandom) {
            @Override public void start(com.vocr.domain.peiyi configuration) {
                seededRandom.reseedForTest(4200 + configuration.ordinal());
                super.start(configuration);
            }
        };
        return new GameSession(id, logic, events, records);
    }
}
