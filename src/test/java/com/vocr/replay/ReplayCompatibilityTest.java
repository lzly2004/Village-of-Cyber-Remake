package com.vocr.replay;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocr.core.GameRecorder;
import com.vocr.history.GameRecordManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReplayCompatibilityTest
{
    @TempDir
    Path tempDir;

    @Test
    void versionOneJson_shouldMigrateAndRoundTripWithoutDataLoss() throws Exception
    {
        Path replayFile = tempDir.resolve("legacy-v1.json");
        try (InputStream fixture = requiredResource("/replay/legacy-v1.json"))
        {
            Files.copy(fixture, replayFile);
        }

        ReplayManager manager = new ReplayManager(replayFile.toString());
        assertEquals(1, manager.getUsedSlotCount());
        ReplaySave migrated = manager.getSlot(0);
        assertVersionOneFixture(migrated);

        manager.save();
        ReplayManager reloaded = new ReplayManager(replayFile.toString());
        assertEquals(1, reloaded.getUsedSlotCount());
        assertVersionOneFixture(reloaded.getSlot(0));
    }

    @Test
    void legacyRawRecording_shouldRebuildReplayAndSurviveJsonRoundTrip() throws Exception
    {
        List<String> records;
        try (InputStream fixture = requiredResource("/replay/legacy-record-v0.txt"))
        {
            records = new String(fixture.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
        GameRecorder recorder = new GameRecorder(
                "legacy-v0", new GameRecordManager(tempDir.resolve("record.json").toString()))
        {
            @Override
            public List<String> getRecords()
            {
                return List.copyOf(records);
            }
        };

        ReplaySave rebuilt = ReplaySave.fromRecorder(3, recorder);
        assertLegacyRecording(rebuilt, records);

        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ReplaySave roundTripped = mapper.readValue(mapper.writeValueAsBytes(rebuilt), ReplaySave.class);
        assertLegacyRecording(roundTripped, records);
    }

    private InputStream requiredResource(String name)
    {
        InputStream resource = getClass().getResourceAsStream(name);
        assertNotNull(resource, "缺少测试资源: " + name);
        return resource;
    }

    private static void assertVersionOneFixture(ReplaySave save)
    {
        assertNotNull(save);
        assertEquals(ReplaySave.CURRENT_VERSION, save.version);
        assertEquals(0, save.slotIndex);
        assertEquals("2025/01/02 03:04:05", save.saveTime);
        assertEquals("tongchang", save.peiyiName);
        assertEquals(2, save.peiyiOrdinal);
        assertEquals(-1, save.peiyiVillageCount);
        assertEquals(-1, save.totalVillageCount);
        assertEquals(3, save.totalDays);
        assertEquals(1, save.endResult);
        assertEquals(6, save.totalEvents);
        assertEquals(6, save.rawRecords.size());
        assertEquals(1, save.daySnapshots.size());

        DaySnapshot day = save.daySnapshots.get(0);
        assertEquals(2, day.dayNumber);
        assertEquals(1, day.aliveCount);
        assertEquals(2, day.players.length);
        assertEquals(30, day.players[0].characterNumber);
        assertEquals(2, day.players[0].comingOutDay);
        assertEquals(2, day.players[0].skillTarget);
        assertEquals(35, day.players[1].characterNumber);
        assertEquals(2, day.players[1].deathDay);
        assertEquals(1, day.players[1].deathReason);
        assertEquals(true, day.players[1].nonHumanMarked);
        assertEquals(2, day.dailyVotingRule);
        assertEquals(List.of(30), day.greyTargetCharNums);
        assertEquals(List.of(35), day.designatedTargetCharNums);
        assertEquals(1, day.voteRounds.size());
        assertEquals(1, day.voteRounds.get(0).round);
        assertArrayEquals(new int[][]{{1, 2}}, day.voteRounds.get(0).votes);
    }

    private static void assertLegacyRecording(ReplaySave save, List<String> records)
    {
        assertEquals(ReplaySave.CURRENT_VERSION, save.version);
        assertEquals(3, save.slotIndex);
        assertEquals("yaoohu", save.peiyiName);
        assertEquals(3, save.peiyiOrdinal);
        assertEquals(4, save.peiyiVillageCount);
        assertEquals(19, save.totalVillageCount);
        assertEquals(3, save.totalDays);
        assertEquals(3, save.endResult);
        assertEquals(records.size(), save.totalEvents);
        assertEquals(records, save.rawRecords);
        assertEquals(2, save.daySnapshots.size());

        DaySnapshot day2 = save.daySnapshots.get(0);
        assertEquals(2, day2.dayNumber);
        assertEquals(2, day2.aliveCount);
        assertEquals(3, day2.players.length);
        assertEquals(7, day2.players[0].characterNumber);
        assertEquals(3, day2.players[0].skillTarget);
        assertEquals(12, day2.players[1].characterNumber);
        assertEquals(2, day2.players[1].deathDay);
        assertEquals(1, day2.players[1].deathReason);
        assertEquals(true, day2.players[1].nonHumanMarked);
        assertEquals(2, day2.dailyVotingRule);
        assertEquals(List.of(1, 2), day2.greyTargetCharNums);
        assertEquals(List.of(12), day2.designatedTargetCharNums);
        assertEquals(1, day2.voteRounds.size());
        assertArrayEquals(new int[][]{{1, 2}, {3, 2}}, day2.voteRounds.get(0).votes);

        DaySnapshot day3 = save.daySnapshots.get(1);
        assertEquals(3, day3.dayNumber);
        assertEquals(2, day3.aliveCount);
        assertEquals(3, day3.players.length);
        assertFalse(day3.players[0].nonHumanMarked);
    }
}
