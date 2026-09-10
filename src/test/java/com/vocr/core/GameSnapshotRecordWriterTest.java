package com.vocr.core;

import com.vocr.domain.Role;
import com.vocr.domain.peiyi;
import com.vocr.history.GameRecordManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSnapshotRecordWriterTest
{
    @TempDir
    Path tempDir;

    @Test
    void writer_shouldPreserveLegacyAndDailyStateFormats()
    {
        GameStatus status = createStatus();
        GameContext context = createContext(status);
        int seer = context.getActualRoleIndex(Role.zhan.ordinal());
        context.registerClaim(seer, Role.zhan.ordinal());
        context.markNonHuman(seer);
        context.recordSkillTarget(seer, 1,
                context.getPlayerSum() + context.getActualRoleIndex(Role.ling.ordinal()));
        List<String> records = new ArrayList<>();
        GameSnapshotRecordWriter writer = new GameSnapshotRecordWriter(records);

        writer.writeStateSnapshot(1, status);
        writer.writeDailySnapshot(2, context);

        assertEquals(List.of(
                expectedLegacyState(1, status),
                "DAILY_SNAPSHOT|day=2|alive=" + context.getAliveCounter(),
                expectedDailyState(2, context)), records);
    }

    @Test
    void recorder_shouldKeepSnapshotMethodsInactiveBeforeStart()
    {
        GameStatus status = createStatus();
        GameContext context = createContext(status);
        GameRecorder recorder = new GameRecorder(
                "inactive-snapshot", new GameRecordManager(tempDir.resolve("record.json").toString()));

        recorder.recordStateSnapshot(1, status);
        recorder.recordDailySnapshot(2, context);
        recorder.recordVoteData(2, context);

        assertTrue(recorder.getRecords().isEmpty());
    }

    private static String expectedLegacyState(int day, GameStatus status)
    {
        StringBuilder state = new StringBuilder("STATE|day=" + day + "|");
        for (int player = 1; player <= status.getPlayerSum(); player++)
        {
            GameCharacter character = status.gc[player];
            state.append("{").append(String.format(
                    "%d:n=%d,r=%d,cr=%d,cro=%d,dd=%d,wd=%d,nm=%d",
                    player, character.number, character.actualRole,
                    character.getClaimedRole(), character.getClaimedRoleOrder(),
                    character.getDeathDay(), character.getDeathReason().ordinal(),
                    character.nonHumanMarker ? 1 : 0)).append("}");
        }
        return state.toString();
    }

    private static String expectedDailyState(int day, GameContext context)
    {
        StringBuilder state = new StringBuilder("STATE|day=" + day + "|");
        for (int player = 1; player <= context.getPlayerSum(); player++)
        {
            state.append("{").append(String.format(
                    "%d:n=%d,r=%d,cr=%d,cro=%d,dd=%d,wd=%d,nm=%d,cod=%d,st=%d",
                    player, context.getCharacterNumber(player), context.getActualRole(player),
                    context.getClaimedRole(player), context.getClaimedRoleOrder(player),
                    context.getDeathDay(player), context.getDeathReason(player).ordinal(),
                    context.isNonHumanMarked(player) ? 1 : 0, context.getComingOutDay(player),
                    context.getSkillTarget(player, day - 1))).append("}");
        }
        return state.toString();
    }

    private static GameStatus createStatus()
    {
        GameStatus status = new GameStatus();
        status.startGame(peiyi.jianyi);
        return status;
    }

    private static GameContext createContext(GameStatus status)
    {
        return new GameContext(status, NonHumanConfiguration.from(peiyi.jianyi, 0));
    }
}
