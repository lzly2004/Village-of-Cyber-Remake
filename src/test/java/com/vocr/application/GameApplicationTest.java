package com.vocr.application;

import com.vocr.core.MainLogic;
import com.vocr.domain.Event;
import com.vocr.domain.peiyi;
import com.vocr.history.GameRecordManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
        List<Event> events = new ArrayList<>();
        MainLogic logic = new MainLogic(events::add, records("start-record.json"));
        GameApplication application = new GameApplication(logic);

        StartedGame result = application.startGame(new StartGameCommand(peiyi.jianyi));

        assertEquals(peiyi.jianyi, result.configuration());
        assertEquals(logic.getGameContext().getPlayerSum(), result.playerCount());
        assertEquals(logic.getGameContext().getGameDay(), result.gameDay());
        assertTrue(result.playerCount() > 0);
        assertTrue(result.gameDay() >= 2);
        assertFalse(events.isEmpty(), "开局领域事件仍应交付给现有桌面事件队列");
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
