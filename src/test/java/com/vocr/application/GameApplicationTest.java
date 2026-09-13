package com.vocr.application;

import com.vocr.core.MainLogic;
import com.vocr.domain.Event;
import com.vocr.domain.peiyi;
import com.vocr.history.GameRecordManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameApplicationTest
{
    @TempDir
    Path tempDir;

    @Test
    void startGame_shouldReturnSafeSummaryAndKeepEventsAvailableToCaller()
    {
        GameEventBuffer events = new GameEventBuffer();
        MainLogic logic = new MainLogic(events::publish, records("start-record.json"));
        GameApplication application = new GameApplication(logic, events);

        StartedGame result = application.startGame(new StartGameCommand(peiyi.jianyi));

        assertEquals(peiyi.jianyi, result.configuration());
        assertEquals(logic.getGameContext().getPlayerSum(), result.playerCount());
        assertEquals(logic.getGameContext().getGameDay(), result.gameDay());
        assertTrue(result.playerCount() > 0);
        assertTrue(result.gameDay() >= 2);
        GameUpdate update = application.currentUpdate();
        assertFalse(update.events().isEmpty(), "开局领域事件应进入应用层更新");
        Event first = update.events().getFirst();
        assertEquals(first, application.pollNextEvent());
        assertEquals(first, update.events().getFirst(), "已发布的更新快照不得随消费进度变化");
        assertThrows(UnsupportedOperationException.class, () -> update.events().add(first));
    }

    @Test
    void startCommand_shouldRejectMissingOrInvalidConfiguration()
    {
        assertThrows(NullPointerException.class, () -> new StartGameCommand(null));
        assertThrows(IllegalArgumentException.class, () -> new StartGameCommand(peiyi.NONE));
    }

    private GameRecordManager records(String fileName)
    {
        return new GameRecordManager(tempDir.resolve(fileName).toString());
    }
}
