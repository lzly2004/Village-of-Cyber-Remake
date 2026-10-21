package com.vocr.application;

import com.vocr.domain.GameResult;
import com.vocr.domain.peiyi;
import com.vocr.history.GameRecord;
import com.vocr.history.GameRecordManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GameRecordManagerAdapterTest
{
    @TempDir
    Path tempDir;

    @Test
    void memoryGuests_shouldAccumulateIndependently()
    {
        GameRecordManager first = GameRecordManager.inMemory();
        GameRecordManager second = GameRecordManager.inMemory();
        first.record(peiyi.jianyi, GameResult.VILLAGE_WIN);
        first.record(peiyi.jianyi, GameResult.FOX_WIN);
        second.record(peiyi.tongchang, GameResult.WOLF_WIN);

        assertEquals(2, first.getRecord().totalPlayCnt);
        assertEquals(0, first.getRecord().playcnt[peiyi.tongchang.ordinal()]);
        assertEquals(1, second.getRecord().totalPlayCnt);
        assertEquals(0, second.getRecord().playcnt[peiyi.jianyi.ordinal()]);
    }

    @Test
    void resultPort_shouldPreserveCumulativeDesktopJsonSemantics()
    {
        Path recordPath = tempDir.resolve("game-record.json");
        GameResultSink sink = new GameRecordManager(recordPath.toString());

        sink.record(peiyi.jianyi, GameResult.VILLAGE_WIN);
        sink.record(peiyi.jianyi, GameResult.WOLF_WIN);

        GameRecord reloaded = new GameRecordManager(recordPath.toString()).getRecord();
        int index = peiyi.jianyi.ordinal();
        assertEquals(GameRecord.CURRENT_VERSION, reloaded.version);
        assertEquals(2, reloaded.playcnt[index]);
        assertEquals(1, reloaded.villageWincnt[index]);
        assertEquals(1, reloaded.wolfWincnt[index]);
        assertEquals(50.0, reloaded.winrate[index]);
        assertEquals(2, reloaded.totalPlayCnt);
    }

    @Test
    void resultPort_shouldRejectUnfinishedOutcome()
    {
        GameRecordManager manager = new GameRecordManager(
                tempDir.resolve("unfinished.json").toString());

        assertThrows(IllegalArgumentException.class,
                () -> manager.record(peiyi.jianyi, GameResult.NONE));
        assertEquals(0, manager.getRecord().totalPlayCnt);
    }
}
